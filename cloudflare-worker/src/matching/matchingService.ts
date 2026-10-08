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
  findBestThreeWayCycle,
  type CandidateRequest,
  type DirectMatch,
  type ThreeWayMatch
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
  matchType?: "DIRECT_2_WAY" | "THREE_WAY";
  match: DirectMatch | ThreeWayMatch;
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
  firestoreClient: FirestoreClient,
  retryCount = 1
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
  const nurseCUid = getString("nurseCUid");
  const status = getString("status");
  const expiresAtStr = getString("expiresAt");
  const updateTime = rawMatch.updateTime;

  if (!nurseAUid || !nurseBUid || !status || !expiresAtStr) {
    throw new MatchServiceError("Malformed match document", 500, "MALFORMED_MATCH_DOC");
  }

  // Detect whether match is 3-way or 2-way.
  // A match is 3-way if nurseCUid is present or if any participant C flag exists.
  const hasParticipantC =
    Boolean(nurseCUid) ||
    fields.acceptedByC !== undefined ||
    fields.rejectedByC !== undefined;

  let isParticipantA = false;
  let isParticipantB = false;
  let isParticipantC = false;
  let acceptedKey: string;
  let rejectedKey: string;

  if (hasParticipantC) {
    // In a 3-way match, nurseCUid must be valid and decision booleans must be present
    const acceptedByC = getBool("acceptedByC");
    const rejectedByC = getBool("rejectedByC");
    if (!nurseCUid || acceptedByC === undefined || rejectedByC === undefined) {
      throw new MatchServiceError("Malformed 3-way match document", 500, "MALFORMED_MATCH_DOC");
    }

    isParticipantA = callerUid === nurseAUid;
    isParticipantB = callerUid === nurseBUid;
    isParticipantC = callerUid === nurseCUid;

    if (!isParticipantA && !isParticipantB && !isParticipantC) {
      throw new MatchServiceError("Caller is not a participant of this match", 403, "NON_PARTICIPANT");
    }

    if (isParticipantA) {
      acceptedKey = "acceptedByA";
      rejectedKey = "rejectedByA";
    } else if (isParticipantB) {
      acceptedKey = "acceptedByB";
      rejectedKey = "rejectedByB";
    } else {
      acceptedKey = "acceptedByC";
      rejectedKey = "rejectedByC";
    }
  } else {
    // 2-Way match
    isParticipantA = callerUid === nurseAUid;
    isParticipantB = callerUid === nurseBUid;

    if (!isParticipantA && !isParticipantB) {
      throw new MatchServiceError("Caller is not a participant of this match", 403, "NON_PARTICIPANT");
    }

    acceptedKey = isParticipantA ? "acceptedByA" : "acceptedByB";
    rejectedKey = isParticipantA ? "rejectedByA" : "rejectedByB";
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
    // ACCEPT decision
    if (hasParticipantC) {
      // 3-Way match: check other two participants' decisions
      const otherAccepted = [
        isParticipantA ? true : Boolean(getBool("acceptedByA")),
        isParticipantB ? true : Boolean(getBool("acceptedByB")),
        isParticipantC ? true : Boolean(getBool("acceptedByC"))
      ];
      const otherRejected =
        Boolean(getBool("rejectedByA")) ||
        Boolean(getBool("rejectedByB")) ||
        Boolean(getBool("rejectedByC"));

      if (otherRejected) {
        newStatus = "CANCELLED";
      } else if (otherAccepted[0] && otherAccepted[1] && otherAccepted[2]) {
        newStatus = "CONFIRMED";
      }
    } else {
      // 2-Way match: check opponent's decision
      const opponentAcceptedKey = isParticipantA ? "acceptedByB" : "acceptedByA";
      const opponentRejectedKey = isParticipantA ? "rejectedByB" : "rejectedByA";
      const opponentAccepted = getBool(opponentAcceptedKey);
      const opponentRejected = getBool(opponentRejectedKey);
      if (opponentRejected) {
        newStatus = "CANCELLED";
      } else if (opponentAccepted) {
        newStatus = "CONFIRMED";
      }
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

  const writes: FirestoreWrite[] = [writeMatch];

  if (newStatus === "CANCELLED") {
    const participantUids = hasParticipantC && nurseCUid
      ? [nurseAUid, nurseBUid, nurseCUid]
      : [nurseAUid, nurseBUid];

    const participantDocs: (CandidateRequest | null)[] = [];
    for (const uid of participantUids) {
      try {
        const doc = await firestoreClient.getRequestDoc(uid);
        participantDocs.push(doc);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Unknown error";
        throw new MatchServiceError(
          `Failed to read participant request ${uid} during cancellation: ${msg}`,
          500,
          "PARTICIPANT_READ_FAILED"
        );
      }
    }

    for (const doc of participantDocs) {
      if (doc && doc.currentMatchId === matchId) {
        writes.push({
          update: {
            name: getTransferRequestDocPath(firestoreClient.getProjectId(), doc.firebaseUid),
            fields: {
              locked: { booleanValue: false },
              currentMatchId: { nullValue: null },
              status: { stringValue: "SEARCHING" },
              updatedAt: { integerValue: Date.now().toString() }
            }
          },
          updateMask: { fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"] },
          currentDocument: doc.updateTime
            ? { updateTime: doc.updateTime }
            : { exists: true }
        });
      }
    }
  }

  // 7. Commit atomically
  try {
    await firestoreClient.commitAtomicMatch(writes);
  } catch (err: unknown) {
    if (err instanceof FirestoreError) {
      const isConcurrencyConflict =
        err.statusCode === 409 ||
        err.code === "ABORTED" ||
        err.code === "FAILED_PRECONDITION";

      if (isConcurrencyConflict) {
        const freshRawMatch = await firestoreClient.getMatchDoc(matchId).catch(() => null);
        if (freshRawMatch) {
          const freshFields = freshRawMatch.fields ?? {};
          const freshStatus = readStringField(freshFields, "status") ?? "PENDING_CONFIRMATION";
          const freshExpiresAt = readStringField(freshFields, "expiresAt") ?? expiresAtStr;
          const freshAccepted = readBooleanField(freshFields, acceptedKey);
          const freshRejected = readBooleanField(freshFields, rejectedKey);

          if ((decision === "ACCEPT" && freshAccepted) || (decision === "REJECT" && freshRejected)) {
            return {
              matchId,
              newStatus: freshStatus,
              expiresAt: freshExpiresAt,
              decisionApplied: true
            };
          }

          if (freshStatus === "CANCELLED" || freshStatus === "CONFIRMED") {
            return {
              matchId,
              newStatus: freshStatus,
              expiresAt: freshExpiresAt,
              decisionApplied:
                (decision === "ACCEPT" && freshStatus === "CONFIRMED") ||
                (decision === "REJECT" && freshStatus === "CANCELLED")
            };
          }

          if (retryCount > 0) {
            return respondToMatch(callerUid, matchId, decision, firestoreClient, retryCount - 1);
          }
        }
      }
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
 * 5. Guarantees zero partial locking: if any participant or match doc fails, entire commit aborts.
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
 * Supports both 2-way and 3-way match documents.
 */
async function recoverExistingMatch(
  callerUid: string,
  matchId: string,
  firestoreClient: FirestoreClient,
  now?: Date
): Promise<FindAndLockMatchSuccess | null> {
  const rawMatch = await firestoreClient.getMatchDoc(matchId);
  if (!rawMatch) {
    return null;
  }

  const fields = rawMatch.fields ?? {};
  const status = readStringField(fields, "status")?.toUpperCase();
  const expiresAt = readStringField(fields, "expiresAt");
  const currentTime = now ?? new Date();
  const isExpired = expiresAt ? currentTime.getTime() > new Date(expiresAt).getTime() : false;

  if (
    status === "CANCELLED" ||
    status === "COMPLETED" ||
    status === "EXPIRED" ||
    isExpired ||
    (status !== undefined && status !== "PENDING_CONFIRMATION" && status !== "CONFIRMED")
  ) {
    return null;
  }

  const nurseAUid = readStringField(fields, "nurseAUid");
  const nurseBUid = readStringField(fields, "nurseBUid");
  const nurseCUid = readStringField(fields, "nurseCUid");
  const nurseACurrentHospitalId = readStringField(fields, "nurseACurrentHospitalId");
  const nurseBCurrentHospitalId = readStringField(fields, "nurseBCurrentHospitalId");
  const nurseCCurrentHospitalId = readStringField(fields, "nurseCCurrentHospitalId");
  const nurseADestinationHospitalId = readStringField(fields, "nurseADestinationHospitalId");
  const nurseBDestinationHospitalId = readStringField(fields, "nurseBDestinationHospitalId");
  const nurseCDestinationHospitalId = readStringField(fields, "nurseCDestinationHospitalId");
  const nurseAGrade = readStringField(fields, "nurseAGrade");
  const nurseBGrade = readStringField(fields, "nurseBGrade");
  const nurseCGrade = readStringField(fields, "nurseCGrade");
  const isSameGrade = readBooleanField(fields, "isSameGrade");
  const isAllSameGrade = readBooleanField(fields, "isAllSameGrade");
  const nurseAPreferenceRank = readIntegerField(fields, "nurseAPreferenceRank");
  const nurseBPreferenceRank = readIntegerField(fields, "nurseBPreferenceRank");
  const nurseCPreferenceRank = readIntegerField(fields, "nurseCPreferenceRank");
  const combinedPreferenceRank = readIntegerField(fields, "combinedPreferenceRank");
  const priorityReason = readStringField(fields, "priorityReason");
  const createdAt = readStringField(fields, "createdAt");

  const is3Way = Boolean(nurseCUid);

  if (is3Way) {
    if (
      !nurseAUid ||
      !nurseBUid ||
      !nurseCUid ||
      !nurseACurrentHospitalId ||
      !nurseBCurrentHospitalId ||
      !nurseCCurrentHospitalId ||
      !nurseADestinationHospitalId ||
      !nurseBDestinationHospitalId ||
      !nurseCDestinationHospitalId ||
      !nurseAGrade ||
      !nurseBGrade ||
      !nurseCGrade ||
      isAllSameGrade === undefined ||
      nurseAPreferenceRank === undefined ||
      nurseBPreferenceRank === undefined ||
      nurseCPreferenceRank === undefined ||
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

    if (callerUid !== nurseAUid && callerUid !== nurseBUid && callerUid !== nurseCUid) {
      throw new MatchServiceError(
        "Caller is not a participant of the referenced match",
        409,
        "MATCH_STATE_INCOMPLETE"
      );
    }

    const match: ThreeWayMatch = {
      nurseAUid,
      nurseBUid,
      nurseCUid,
      nurseACurrentHospitalId,
      nurseBCurrentHospitalId,
      nurseCCurrentHospitalId,
      nurseADestinationHospitalId,
      nurseBDestinationHospitalId,
      nurseCDestinationHospitalId,
      nurseAGrade,
      nurseBGrade,
      nurseCGrade,
      isAllSameGrade,
      nurseAPreferenceRank,
      nurseBPreferenceRank,
      nurseCPreferenceRank,
      combinedPreferenceRank,
      priorityReason
    };

    return {
      matched: true,
      matchId,
      matchType: "THREE_WAY",
      match,
      createdAt,
      expiresAt
    };
  }

  // 2-Way Match recovery
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
    matchType: "DIRECT_2_WAY",
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
    const existingMatch = await recoverExistingMatch(
      cleanCallerUid,
      caller.currentMatchId.trim(),
      firestoreClient,
      options.now ? options.now() : undefined
    );
    if (existingMatch) {
      return existingMatch;
    }

    const referencedMatchDoc = await firestoreClient.getMatchDoc(caller.currentMatchId.trim());
    if (referencedMatchDoc) {
      const matchStatus = readStringField(referencedMatchDoc.fields ?? {}, "status")?.toUpperCase();
      if (matchStatus === "COMPLETED") {
        throw new MatchServiceError(
          "Caller transfer has already been completed",
          409,
          "MATCH_ALREADY_COMPLETED"
        );
      }
    }

    // Stale or terminal match: reset caller request to SEARCHING and unlock
    const staleMatchId = caller.currentMatchId.trim();
    const resetWrite: FirestoreWrite = {
      update: {
        name: getTransferRequestDocPath(firestoreClient.getProjectId(), cleanCallerUid),
        fields: {
          locked: { booleanValue: false },
          currentMatchId: { nullValue: null },
          status: { stringValue: "SEARCHING" },
          updatedAt: { integerValue: Date.now().toString() }
        }
      },
      updateMask: {
        fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"]
      },
      currentDocument: caller.updateTime
        ? { updateTime: caller.updateTime }
        : { exists: true }
    };

    try {
      await firestoreClient.commitAtomicMatch([resetWrite]);
      caller.locked = false;
      caller.currentMatchId = null;
      caller.status = "SEARCHING";
    } catch {
      const rechecked = await firestoreClient.getRequestDoc(cleanCallerUid);
      if (rechecked) {
        if (
          rechecked.locked &&
          rechecked.currentMatchId &&
          rechecked.currentMatchId.trim() !== staleMatchId
        ) {
          const recoveredNewer = await recoverExistingMatch(
            cleanCallerUid,
            rechecked.currentMatchId.trim(),
            firestoreClient,
            options.now ? options.now() : undefined
          );
          if (recoveredNewer) return recoveredNewer;
        }
        Object.assign(caller, rechecked);
      }
    }
  }

  validateCallerRequest(caller);

  const candidateLimit = options.candidateLimit ?? 100;
  const batchSize = 100;
  const rawCandidates: CandidateRequest[] = [];
  let offset = 0;

  while (rawCandidates.length < candidateLimit) {
    const currentBatchLimit = Math.min(batchSize, candidateLimit - rawCandidates.length);
    const batch = await firestoreClient.querySearchingCandidates(currentBatchLimit, offset);
    if (!batch || batch.length === 0) break;
    rawCandidates.push(...batch);
    if (batch.length < currentBatchLimit) break;
    offset += batch.length;
  }

  const pool = rawCandidates.filter(
    candidate =>
      candidate.firebaseUid.trim() !== cleanCallerUid &&
      !candidate.locked &&
      (!candidate.currentMatchId || candidate.currentMatchId.trim().length === 0)
  );

  // 1. Direct 2-Way matching has ABSOLUTE priority
  const best2WayMatch = findBestMatch(caller, pool);

  const matchId = options.generateMatchId
    ? options.generateMatchId()
    : crypto.randomUUID();

  const now = options.now ? options.now() : new Date();
  const nowIso = now.toISOString();
  const expiresAt = new Date(now.getTime() + options.expirationHours! * 3600 * 1000);
  const expiresAtIso = expiresAt.toISOString();
  const projectId = firestoreClient.getProjectId();

  if (best2WayMatch) {
    const candidate = pool.find(
      candidateRequest =>
        candidateRequest.firebaseUid.trim() === best2WayMatch.nurseBUid.trim()
    );
    if (!candidate) {
      return {
        matched: false,
        message: "Candidate no longer in pool"
      };
    }

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
        name: getTransferRequestDocPath(projectId, best2WayMatch.nurseBUid),
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
          nurseAUid: { stringValue: best2WayMatch.nurseAUid },
          nurseBUid: { stringValue: best2WayMatch.nurseBUid },
          nurseACurrentHospitalId: { stringValue: best2WayMatch.nurseACurrentHospitalId },
          nurseBCurrentHospitalId: { stringValue: best2WayMatch.nurseBCurrentHospitalId },
          nurseADestinationHospitalId: { stringValue: best2WayMatch.nurseADestinationHospitalId },
          nurseBDestinationHospitalId: { stringValue: best2WayMatch.nurseBDestinationHospitalId },
          nurseAGrade: { stringValue: best2WayMatch.nurseAGrade },
          nurseBGrade: { stringValue: best2WayMatch.nurseBGrade },
          isSameGrade: { booleanValue: best2WayMatch.isSameGrade },
          nurseAPreferenceRank: { integerValue: best2WayMatch.nurseAPreferenceRank.toString() },
          nurseBPreferenceRank: { integerValue: best2WayMatch.nurseBPreferenceRank.toString() },
          combinedPreferenceRank: { integerValue: best2WayMatch.combinedPreferenceRank.toString() },
          priorityReason: { stringValue: best2WayMatch.priorityReason },
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
          const latestCaller = await firestoreClient.getRequestDoc(cleanCallerUid);
          if (
            latestCaller?.locked &&
            latestCaller.currentMatchId &&
            latestCaller.currentMatchId.trim().length > 0 &&
            latestCaller.status.trim().toUpperCase() === "MATCHED"
          ) {
            const recovered = await recoverExistingMatch(
              cleanCallerUid,
              latestCaller.currentMatchId.trim(),
              firestoreClient,
              options.now ? options.now() : undefined
            );
            if (recovered) {
              return recovered;
            }
          }

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
      matchType: "DIRECT_2_WAY",
      match: best2WayMatch,
      createdAt: nowIso,
      expiresAt: expiresAtIso
    };
  }

  // 2. Only if NO direct 2-way match, search for a valid 3-way cycle
  const best3WayCycle = findBestThreeWayCycle(caller, pool);
  if (!best3WayCycle) {
    return {
      matched: false,
      message: "No compatible match found"
    };
  }

  const candidateB = pool.find(
    candidateRequest =>
      candidateRequest.firebaseUid.trim() === best3WayCycle.nurseBUid.trim()
  );
  const candidateC = pool.find(
    candidateRequest =>
      candidateRequest.firebaseUid.trim() === best3WayCycle.nurseCUid.trim()
  );

  if (!candidateB || !candidateC) {
    return {
      matched: false,
      message: "Candidate no longer in pool"
    };
  }

  const writeCaller3Way: FirestoreWrite = {
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

  const writeCandidateB: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, best3WayCycle.nurseBUid),
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
    currentDocument: candidateB.updateTime
      ? { updateTime: candidateB.updateTime }
      : { exists: true }
  };

  const writeCandidateC: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, best3WayCycle.nurseCUid),
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
    currentDocument: candidateC.updateTime
      ? { updateTime: candidateC.updateTime }
      : { exists: true }
  };

  const writeMatch3Way: FirestoreWrite = {
    update: {
      name: getMatchDocPath(projectId, matchId),
      fields: {
        nurseAUid: { stringValue: best3WayCycle.nurseAUid },
        nurseBUid: { stringValue: best3WayCycle.nurseBUid },
        nurseCUid: { stringValue: best3WayCycle.nurseCUid },
        nurseACurrentHospitalId: { stringValue: best3WayCycle.nurseACurrentHospitalId },
        nurseBCurrentHospitalId: { stringValue: best3WayCycle.nurseBCurrentHospitalId },
        nurseCCurrentHospitalId: { stringValue: best3WayCycle.nurseCCurrentHospitalId },
        nurseADestinationHospitalId: { stringValue: best3WayCycle.nurseADestinationHospitalId },
        nurseBDestinationHospitalId: { stringValue: best3WayCycle.nurseBDestinationHospitalId },
        nurseCDestinationHospitalId: { stringValue: best3WayCycle.nurseCDestinationHospitalId },
        nurseAGrade: { stringValue: best3WayCycle.nurseAGrade },
        nurseBGrade: { stringValue: best3WayCycle.nurseBGrade },
        nurseCGrade: { stringValue: best3WayCycle.nurseCGrade },
        isAllSameGrade: { booleanValue: best3WayCycle.isAllSameGrade },
        nurseAPreferenceRank: { integerValue: best3WayCycle.nurseAPreferenceRank.toString() },
        nurseBPreferenceRank: { integerValue: best3WayCycle.nurseBPreferenceRank.toString() },
        nurseCPreferenceRank: { integerValue: best3WayCycle.nurseCPreferenceRank.toString() },
        combinedPreferenceRank: { integerValue: best3WayCycle.combinedPreferenceRank.toString() },
        priorityReason: { stringValue: best3WayCycle.priorityReason },
        status: { stringValue: "PENDING_CONFIRMATION" },
        acceptedByA: { booleanValue: false },
        acceptedByB: { booleanValue: false },
        acceptedByC: { booleanValue: false },
        rejectedByA: { booleanValue: false },
        rejectedByB: { booleanValue: false },
        rejectedByC: { booleanValue: false },
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
    await firestoreClient.commitAtomicMatch([
      writeCaller3Way,
      writeCandidateB,
      writeCandidateC,
      writeMatch3Way
    ]);
  } catch (err: unknown) {
    if (err instanceof FirestoreError) {
      const isConcurrencyConflict =
        err.statusCode === 409 ||
        err.code === "ABORTED" ||
        err.code === "FAILED_PRECONDITION";

      if (isConcurrencyConflict) {
        const latestCaller = await firestoreClient.getRequestDoc(cleanCallerUid);
        if (
          latestCaller?.locked &&
          latestCaller.currentMatchId &&
          latestCaller.currentMatchId.trim().length > 0 &&
          latestCaller.status.trim().toUpperCase() === "MATCHED"
        ) {
          const recovered = await recoverExistingMatch(
            cleanCallerUid,
            latestCaller.currentMatchId.trim(),
            firestoreClient,
            options.now ? options.now() : undefined
          );
          if (recovered) {
            return recovered;
          }
        }

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
    matchType: "THREE_WAY",
    match: best3WayCycle,
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

export interface SweepExpiredMatchesOptions {
  batchSize?: number;
  now?: () => Date;
}

export interface SweepExpiredMatchesResult {
  scanned: number;
  expired: number;
  unlockedParticipants: number;
  errors: number;
}

/**
 * Sweeps active PENDING_CONFIRMATION matches that have passed their expiration deadline.
 * Marks expired matches as EXPIRED and unlocks/resets their participants back to SEARCHING.
 *
 * Invariants:
 * - Scans only PENDING_CONFIRMATION matches where now > expiresAt.
 * - Atomically marks match EXPIRED and resets participants (A, B, and C if 3-way).
 * - Preconditions guard participants against race conditions; never overrides newer matches.
 * - Safe error handling: skips/logs errors per match without halting the entire sweep.
 */
export async function sweepExpiredMatches(
  firestoreClient: FirestoreClient,
  options?: SweepExpiredMatchesOptions
): Promise<SweepExpiredMatchesResult> {
  const batchSize = options?.batchSize ?? 100;
  const now = options?.now ? options.now() : new Date();

  const pendingMatches = await firestoreClient.queryPendingConfirmationMatches(batchSize);

  let scanned = 0;
  let expired = 0;
  let unlockedParticipants = 0;
  let errors = 0;

  for (const rawMatch of pendingMatches) {
    scanned += 1;
    try {
      const matchName = rawMatch.name;
      const matchId = matchName ? matchName.split("/").pop() || "" : "";
      if (!matchId) continue;

      const fields = rawMatch.fields ?? {};
      const status = readStringField(fields, "status")?.toUpperCase();
      if (status !== "PENDING_CONFIRMATION") continue;

      const expiresAtStr = readStringField(fields, "expiresAt");
      if (!expiresAtStr) continue;

      const expiresAt = new Date(expiresAtStr);
      if (now.getTime() <= expiresAt.getTime()) {
        // Not expired yet
        continue;
      }

      // Match has expired
      const nurseAUid = readStringField(fields, "nurseAUid");
      const nurseBUid = readStringField(fields, "nurseBUid");
      const nurseCUid = readStringField(fields, "nurseCUid");

      const participantUids: string[] = [];
      if (nurseAUid) participantUids.push(nurseAUid);
      if (nurseBUid) participantUids.push(nurseBUid);
      if (nurseCUid) participantUids.push(nurseCUid);

      const writeMatch: FirestoreWrite = {
        update: {
          name: getMatchDocPath(firestoreClient.getProjectId(), matchId),
          fields: {
            status: { stringValue: "EXPIRED" },
            updatedAt: { integerValue: Date.now().toString() }
          }
        },
        updateMask: { fieldPaths: ["status", "updatedAt"] },
        currentDocument: rawMatch.updateTime
          ? { updateTime: rawMatch.updateTime }
          : { exists: true }
      };

      const writes: FirestoreWrite[] = [writeMatch];
      let participantsToUnlockCount = 0;

      for (const uid of participantUids) {
        try {
          const doc = await firestoreClient.getRequestDoc(uid);
          if (doc && doc.currentMatchId === matchId) {
            participantsToUnlockCount += 1;
            writes.push({
              update: {
                name: getTransferRequestDocPath(firestoreClient.getProjectId(), doc.firebaseUid),
                fields: {
                  locked: { booleanValue: false },
                  currentMatchId: { nullValue: null },
                  status: { stringValue: "SEARCHING" },
                  updatedAt: { integerValue: Date.now().toString() }
                }
              },
              updateMask: { fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"] },
              currentDocument: doc.updateTime
                ? { updateTime: doc.updateTime }
                : { exists: true }
            });
          }
        } catch (readErr: unknown) {
          throw new MatchServiceError(
            `Failed to read participant request ${uid} during expiry sweep: ${readErr instanceof Error ? readErr.message : "Unknown error"}`,
            500,
            "PARTICIPANT_READ_FAILED"
          );
        }
      }

      await firestoreClient.commitAtomicMatch(writes);
      expired += 1;
      unlockedParticipants += participantsToUnlockCount;
    } catch (err: unknown) {
      errors += 1;
      console.error("[SWEEP_EXPIRED_MATCH_ERROR]", {
        message: err instanceof Error ? err.message : String(err)
      });
    }
  }

  return {
    scanned,
    expired,
    unlockedParticipants,
    errors
  };
}
