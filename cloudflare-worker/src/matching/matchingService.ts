import {
  FirestoreClient,
  FirestoreError,
  getMatchDocPath,
  getTransferRequestDocPath,
  type FirestoreWrite
} from "../firestore/firestoreClient.ts";
import type { MatchDecisionResponse } from "../types.ts";
import {
  findBestMatch,
  type CandidateRequest,
  type DirectMatch
} from "./matchingEngine.ts";

export class MatchServiceError extends Error {
  statusCode: number;
  code: string;

  constructor(message: string, statusCode = 400, code = "MATCH_ERROR") {
    super(message);
    this.name = "MatchServiceError";
    this.statusCode = statusCode;
    this.code = code;
  }
}

export interface FindAndLockMatchSuccess {
  matched: true;
  matchId: string;
  match: DirectMatch;
  createdAt: string;
  expiresAt: string;
}

export interface FindAndLockNoMatch {
  matched: false;
  message: string;
}

export type FindAndLockResult = FindAndLockMatchSuccess | FindAndLockNoMatch;

export interface FindAndLockMatchOptions {
  expirationHours?: number;
  candidateLimit?: number;
  now?: () => Date;
  generateMatchId?: () => string;
}

/**
 * Validates caller transfer request for eligibility to enter matching.
 */
function validateCallerRequest(caller: CandidateRequest): void {
  if (caller.locked) {
    throw new MatchServiceError(
      "Caller transfer request is already locked",
      409,
      "CALLER_LOCKED"
    );
  }

  if (caller.currentMatchId && caller.currentMatchId.trim().length > 0) {
    throw new MatchServiceError(
      "Caller already has an active match",
      409,
      "CALLER_ALREADY_MATCHED"
    );
  }

  const status = (caller.status || "").trim().toUpperCase();
  if (status !== "SEARCHING") {
    throw new MatchServiceError(
      `Caller transfer request status is '${caller.status}', expected 'SEARCHING'`,
      400,
      "INVALID_STATUS"
    );
  }

  if (!caller.grade || caller.grade.trim().length === 0) {
    throw new MatchServiceError(
      "Caller transfer request missing grade",
      400,
      "INVALID_REQUEST"
    );
  }

  if (!caller.currentHospitalId || caller.currentHospitalId.trim().length === 0) {
    throw new MatchServiceError(
      "Caller transfer request missing currentHospitalId",
      400,
      "INVALID_REQUEST"
    );
  }

  if (
    !Array.isArray(caller.preferenceHospitalIds) ||
    caller.preferenceHospitalIds.length === 0
  ) {
    throw new MatchServiceError(
      "Caller transfer request has no destination preferences",
      400,
      "INVALID_REQUEST"
    );
  }
}

/**
 * Responds to a matched transfer request with an ACCEPT or REJECT decision.
 */
export async function respondToMatch(
  callerUid: string,
  matchId: string,
  decision: "ACCEPT" | "REJECT",
  firestoreClient: FirestoreClient
): Promise<MatchDecisionResponse> {
  // 1. Load match document
  const rawMatch = await firestoreClient.getMatchDoc(matchId);
  if (!rawMatch) {
    throw new MatchServiceError("Match not found", 404, "MATCH_NOT_FOUND");
  }

  // Extract fields (assume FirestoreValue objects)
  const fields = rawMatch.fields ?? {};
  const getString = (key: string) => {
    const val = fields[key];
    return typeof val === "object" && "stringValue" in val ? val.stringValue : undefined;
  };
  const getBool = (key: string) => {
    const val = fields[key];
    return typeof val === "object" && "booleanValue" in val ? val.booleanValue : undefined;
  };

  const nurseAUid = getString("nurseAUid");
  const nurseBUid = getString("nurseBUid");
  const status = getString("status");
  const expiresAtStr = getString("expiresAt");
  const updateTime = rawMatch.updateTime;

  if (!nurseAUid || !nurseBUid || !status || !expiresAtStr) {
    throw new MatchServiceError("Malformed match document", 500, "MALFORMED_MATCH_DOC");
  }

  // 2. Verify caller is a participant
  const isParticipantA = callerUid === nurseAUid;
  const isParticipantB = callerUid === nurseBUid;
  if (!isParticipantA && !isParticipantB) {
    throw new MatchServiceError("Caller is not a participant of this match", 403, "NON_PARTICIPANT");
  }

  // 3. Verify match is actionable
  if (status !== "PENDING_CONFIRMATION") {
    throw new MatchServiceError("Match is not awaiting confirmation", 409, "INVALID_STATUS");
  }

  const now = new Date();
  const expiresAt = new Date(expiresAtStr);
  if (now > expiresAt) {
    throw new MatchServiceError("Match deadline has expired", 410, "EXPIRED");
  }

  // 4. Verify participant has not already responded
  const acceptedKey = isParticipantA ? "acceptedByA" : "acceptedByB";
  const rejectedKey = isParticipantA ? "rejectedByA" : "rejectedByB";
  const alreadyAccepted = getBool(acceptedKey);
  const alreadyRejected = getBool(rejectedKey);
  if (alreadyAccepted || alreadyRejected) {
    throw new MatchServiceError("Participant has already responded", 409, "ALREADY_RESPONDED");
  }

  // 5. Determine new status after this decision
  let newStatus = "PENDING_CONFIRMATION";
  if (decision === "REJECT") {
    newStatus = "CANCELLED";
  } else {
    // ACCEPT: check opponent's decision
    const opponentAcceptedKey = isParticipantA ? "acceptedByB" : "acceptedByA";
    const opponentRejectedKey = isParticipantA ? "rejectedByB" : "rejectedByA";
    const opponentAccepted = getBool(opponentAcceptedKey);
    const opponentRejected = getBool(opponentRejectedKey);
    if (opponentRejected) {
      newStatus = "CANCELLED"; // should not happen due to precondition but safe
    } else if (opponentAccepted) {
      newStatus = "CONFIRMED";
    }
  }

  // 6. Build Firestore write with precondition on updateTime
  const writeMatch: FirestoreWrite = {
    update: {
      name: getMatchDocPath(firestoreClient.getProjectId(), matchId),
      fields: {
        [acceptedKey]: { booleanValue: decision === "ACCEPT" },
        [rejectedKey]: { booleanValue: decision === "REJECT" },
        status: { stringValue: newStatus },
        updatedAt: { integerValue: Date.now().toString() }
      }
    },
    updateMask: { fieldPaths: [acceptedKey, rejectedKey, "status", "updatedAt"] },
    currentDocument: updateTime
      ? { updateTime }
      : { exists: true }
  };

  // 7. Commit atomically
  try {
    await firestoreClient.commitAtomicMatch([writeMatch]);
  } catch (err: unknown) {
    if (err instanceof FirestoreError) {
      throw new MatchServiceError(`Failed to commit decision: ${err.message}`, err.statusCode || 500, "FIRESTORE_COMMIT_FAILED");
    }
    const msg = err instanceof Error ? err.message : "Internal error";
    throw new MatchServiceError(`Commit error: ${msg}`, 500, "INTERNAL_ERROR");
  }

  return {
    matchId,
    newStatus,
    expiresAt: expiresAtStr,
    decisionApplied: true
  };
}

/**
 * Server-Side Matching and Concurrency-Safe Locking Orchestrator.
 *
 * Invariants:
 * 1. Derives authenticated caller strictly from callerUid verified via JWT.
 * 2. Queries searching candidates without grade-based exclusionary filtering.
 * 3. Uses deterministic pure matching engine (same-grade prioritized, cross-grade eligible).
 * 4. Executes concurrency-safe atomic lock via Firestore :commit with precondition updateTime.
 * 5. Guarantees zero partial locking: if either nurse or match doc fails, entire commit aborts.
 */
function readStringField(
  fields: Record<string, { stringValue?: string; booleanValue?: boolean; integerValue?: string }>,
  key: string
): string | undefined {
  const value = fields[key];
  return value && typeof value === "object" && "stringValue" in value
    ? value.stringValue
    : undefined;
}

function readBooleanField(
  fields: Record<string, { stringValue?: string; booleanValue?: boolean; integerValue?: string }>,
  key: string
): boolean | undefined {
  const value = fields[key];
  return value && typeof value === "object" && "booleanValue" in value
    ? value.booleanValue
    : undefined;
}

function readIntegerField(
  fields: Record<string, { stringValue?: string; booleanValue?: boolean; integerValue?: string }>,
  key: string
): number | undefined {
  const value = fields[key];
  if (!value || typeof value !== "object" || !("integerValue" in value)) return undefined;
  const parsed = Number(value.integerValue);
  return Number.isSafeInteger(parsed) ? parsed : undefined;
}

/**
 * Reconstructs an already-created active match after a concurrent caller lost
 * the Firestore commit race. This is recovery only; it never fabricates a match.
 */
async function recoverExistingMatch(
  callerUid: string,
  matchId: string,
  firestoreClient: FirestoreClient
): Promise<FindAndLockMatchSuccess> {
  const rawMatch = await firestoreClient.getMatchDoc(matchId);
  if (!rawMatch) {
    throw new MatchServiceError(
      "Caller is marked MATCHED but the referenced match document is missing",
      409,
      "MATCH_STATE_INCOMPLETE"
    );
  }

  const fields = rawMatch.fields ?? {};
  const nurseAUid = readStringField(fields, "nurseAUid");
  const nurseBUid = readStringField(fields, "nurseBUid");
  const nurseACurrentHospitalId = readStringField(fields, "nurseACurrentHospitalId");
  const nurseBCurrentHospitalId = readStringField(fields, "nurseBCurrentHospitalId");
  const nurseADestinationHospitalId = readStringField(fields, "nurseADestinationHospitalId");
  const nurseBDestinationHospitalId = readStringField(fields, "nurseBDestinationHospitalId");
  const nurseAGrade = readStringField(fields, "nurseAGrade");
  const nurseBGrade = readStringField(fields, "nurseBGrade");
  const isSameGrade = readBooleanField(fields, "isSameGrade");
  const nurseAPreferenceRank = readIntegerField(fields, "nurseAPreferenceRank");
  const nurseBPreferenceRank = readIntegerField(fields, "nurseBPreferenceRank");
  const combinedPreferenceRank = readIntegerField(fields, "combinedPreferenceRank");
  const priorityReason = readStringField(fields, "priorityReason");
  const createdAt = readStringField(fields, "createdAt");
  const expiresAt = readStringField(fields, "expiresAt");

  if (
    !nurseAUid ||
    !nurseBUid ||
    !nurseACurrentHospitalId ||
    !nurseBCurrentHospitalId ||
    !nurseADestinationHospitalId ||
    !nurseBDestinationHospitalId ||
    !nurseAGrade ||
    !nurseBGrade ||
    isSameGrade === undefined ||
    nurseAPreferenceRank === undefined ||
    nurseBPreferenceRank === undefined ||
    combinedPreferenceRank === undefined ||
    !priorityReason ||
    !createdAt ||
    !expiresAt
  ) {
    throw new MatchServiceError(
      "Referenced match document is malformed",
      500,
      "MALFORMED_MATCH_DOC"
    );
  }

  if (callerUid !== nurseAUid && callerUid !== nurseBUid) {
    throw new MatchServiceError(
      "Caller is not a participant of the referenced match",
      409,
      "MATCH_STATE_INCOMPLETE"
    );
  }

  const match: DirectMatch = {
    nurseAUid,
    nurseBUid,
    nurseACurrentHospitalId,
    nurseBCurrentHospitalId,
    nurseADestinationHospitalId,
    nurseBDestinationHospitalId,
    nurseAGrade,
    nurseBGrade,
    isSameGrade,
    nurseAPreferenceRank,
    nurseBPreferenceRank,
    combinedPreferenceRank,
    priorityReason
  };

  return {
    matched: true,
    matchId,
    match,
    createdAt,
    expiresAt
  };
}

function conflictRetryDelayMs(callerUid: string): number {
  // Small deterministic jitter avoids synchronized retry bursts without using randomness.
  let hash = 0;
  for (let index = 0; index < callerUid.length; index += 1) {
    hash = (hash * 31 + callerUid.charCodeAt(index)) >>> 0;
  }
  return 75 + (hash % 101);
}

async function findAndLockMatchInternal(
  callerUid: string,
  firestoreClient: FirestoreClient,
  options: FindAndLockMatchOptions,
  allowConflictRetry: boolean
): Promise<FindAndLockResult> {
  const cleanCallerUid = callerUid.trim();

  // Always re-read the caller before matching. This is also the recovery point
  // after a competing worker has already won the race.
  const caller = await firestoreClient.getRequestDoc(cleanCallerUid);
  if (!caller) {
    throw new MatchServiceError(
      "No transfer request found for caller",
      404,
      "CALLER_NOT_FOUND"
    );
  }

  if (
    caller.locked &&
    caller.currentMatchId &&
    caller.currentMatchId.trim().length > 0 &&
    caller.status.trim().toUpperCase() === "MATCHED"
  ) {
    return recoverExistingMatch(
      cleanCallerUid,
      caller.currentMatchId.trim(),
      firestoreClient
    );
  }

  validateCallerRequest(caller);

  const candidateLimit = options.candidateLimit ?? 100;
  const rawCandidates = await firestoreClient.querySearchingCandidates(candidateLimit);

  const pool = rawCandidates.filter(
    candidate =>
      candidate.firebaseUid.trim() !== cleanCallerUid &&
      !candidate.locked &&
      (!candidate.currentMatchId || candidate.currentMatchId.trim().length === 0)
  );

  const bestMatch = findBestMatch(caller, pool);
  if (!bestMatch) {
    return {
      matched: false,
      message: "No compatible match found"
    };
  }

  const candidate = pool.find(
    candidateRequest =>
      candidateRequest.firebaseUid.trim() === bestMatch.nurseBUid.trim()
  );
  if (!candidate) {
    return {
      matched: false,
      message: "Candidate no longer in pool"
    };
  }

  const matchId = options.generateMatchId
    ? options.generateMatchId()
    : crypto.randomUUID();

  const now = options.now ? options.now() : new Date();
  const nowIso = now.toISOString();
  const expiresAt = new Date(now.getTime() + options.expirationHours! * 3600 * 1000);
  const expiresAtIso = expiresAt.toISOString();
  const projectId = firestoreClient.getProjectId();

  const writeCaller: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, cleanCallerUid),
      fields: {
        locked: { booleanValue: true },
        currentMatchId: { stringValue: matchId },
        status: { stringValue: "MATCHED" },
        updatedAt: { integerValue: now.getTime().toString() }
      }
    },
    updateMask: {
      fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"]
    },
    currentDocument: caller.updateTime
      ? { updateTime: caller.updateTime }
      : { exists: true }
  };

  const writeCandidate: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, bestMatch.nurseBUid),
      fields: {
        locked: { booleanValue: true },
        currentMatchId: { stringValue: matchId },
        status: { stringValue: "MATCHED" },
        updatedAt: { integerValue: now.getTime().toString() }
      }
    },
    updateMask: {
      fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"]
    },
    currentDocument: candidate.updateTime
      ? { updateTime: candidate.updateTime }
      : { exists: true }
  };

  const writeMatch: FirestoreWrite = {
    update: {
      name: getMatchDocPath(projectId, matchId),
      fields: {
        nurseAUid: { stringValue: bestMatch.nurseAUid },
        nurseBUid: { stringValue: bestMatch.nurseBUid },
        nurseACurrentHospitalId: { stringValue: bestMatch.nurseACurrentHospitalId },
        nurseBCurrentHospitalId: { stringValue: bestMatch.nurseBCurrentHospitalId },
        nurseADestinationHospitalId: { stringValue: bestMatch.nurseADestinationHospitalId },
        nurseBDestinationHospitalId: { stringValue: bestMatch.nurseBDestinationHospitalId },
        nurseAGrade: { stringValue: bestMatch.nurseAGrade },
        nurseBGrade: { stringValue: bestMatch.nurseBGrade },
        isSameGrade: { booleanValue: bestMatch.isSameGrade },
        nurseAPreferenceRank: { integerValue: bestMatch.nurseAPreferenceRank.toString() },
        nurseBPreferenceRank: { integerValue: bestMatch.nurseBPreferenceRank.toString() },
        combinedPreferenceRank: { integerValue: bestMatch.combinedPreferenceRank.toString() },
        priorityReason: { stringValue: bestMatch.priorityReason },
        status: { stringValue: "PENDING_CONFIRMATION" },
        acceptedByA: { booleanValue: false },
        acceptedByB: { booleanValue: false },
        rejectedByA: { booleanValue: false },
        rejectedByB: { booleanValue: false },
        createdAt: { stringValue: nowIso },
        expiresAt: { stringValue: expiresAtIso },
        updatedAt: { integerValue: now.getTime().toString() }
      }
    },
    currentDocument: {
      exists: false
    }
  };

  try {
    await firestoreClient.commitAtomicMatch([writeCaller, writeCandidate, writeMatch]);
  } catch (err: unknown) {
    if (err instanceof FirestoreError) {
      const isConcurrencyConflict =
        err.statusCode === 409 ||
        err.code === "ABORTED" ||
        err.code === "FAILED_PRECONDITION";

      if (isConcurrencyConflict) {
        // First recover the winner if another worker committed the match between
        // our initial read and the failed atomic commit.
        const latestCaller = await firestoreClient.getRequestDoc(cleanCallerUid);
        if (
          latestCaller?.locked &&
          latestCaller.currentMatchId &&
          latestCaller.currentMatchId.trim().length > 0 &&
          latestCaller.status.trim().toUpperCase() === "MATCHED"
        ) {
          return recoverExistingMatch(
            cleanCallerUid,
            latestCaller.currentMatchId.trim(),
            firestoreClient
          );
        }

        // Firestore REST :commit is not automatically retried like a client
        // transaction. Retry exactly once with fresh reads/preconditions.
        if (allowConflictRetry) {
          await new Promise<void>(resolve => {
            setTimeout(resolve, conflictRetryDelayMs(cleanCallerUid));
          });

          return findAndLockMatchInternal(
            cleanCallerUid,
            firestoreClient,
            options,
            false
          );
        }

        throw new MatchServiceError(
          "Concurrent modification conflict detected while locking transfer match",
          409,
          "MATCH_CONFLICT"
        );
      }

      throw new MatchServiceError(
        `Failed to commit match: ${err.message}`,
        err.statusCode || 500,
        "FIRESTORE_COMMIT_FAILED"
      );
    }

    const msg = err instanceof Error ? err.message : "Internal error";
    throw new MatchServiceError(`Commit error: ${msg}`, 500, "INTERNAL_ERROR");
  }

  return {
    matched: true,
    matchId,
    match: bestMatch,
    createdAt: nowIso,
    expiresAt: expiresAtIso
  };
}

export async function findAndLockMatch(
  callerUid: string,
  firestoreClient: FirestoreClient,
  options?: FindAndLockMatchOptions
): Promise<FindAndLockResult> {
  // Expiration duration must be explicitly configured.
  // Invariant: No implicit 48-hour or 72-hour default is permitted.
  const expirationHours = options?.expirationHours;
  if (
    expirationHours === undefined ||
    expirationHours === null ||
    typeof expirationHours !== "number" ||
    isNaN(expirationHours) ||
    expirationHours <= 0
  ) {
    throw new MatchServiceError(
      "Match expiration duration must be explicitly configured (no implicit 48h/72h default permitted)",
      500,
      "CONFIG_ERROR"
    );
  }

  if (!callerUid || callerUid.trim().length === 0) {
    throw new MatchServiceError("callerUid must not be empty", 400, "INVALID_UID");
  }

  return findAndLockMatchInternal(
    callerUid,
    firestoreClient,
    {
      ...options,
      expirationHours
    },
    true
  );
}
