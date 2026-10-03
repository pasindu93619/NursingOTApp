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
        updatedAt: { stringValue: new Date().toISOString() }
      }
    },
    updateMask: { fieldPaths: [acceptedKey, rejectedKey, "status", "updatedAt"] },
    currentDocument: {
      exists: true,
      ...(updateTime ? { updateTime } : {})
    }
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

  const cleanCallerUid = callerUid.trim();

  // 1. Fetch caller's transfer request
  const caller = await firestoreClient.getRequestDoc(cleanCallerUid);
  if (!caller) {
    throw new MatchServiceError(
      "No transfer request found for caller",
      404,
      "CALLER_NOT_FOUND"
    );
  }

  // 2. Validate caller eligibility
  validateCallerRequest(caller);

  // 3. Fetch searching candidate pool (cross-grade candidates included)
  const candidateLimit = options?.candidateLimit ?? 100;
  const rawCandidates = await firestoreClient.querySearchingCandidates(candidateLimit);

  // Filter out caller and any locked/matched requests
  const pool = rawCandidates.filter(
    c =>
      c.firebaseUid.trim() !== cleanCallerUid &&
      !c.locked &&
      (!c.currentMatchId || c.currentMatchId.trim().length === 0)
  );

  // 4. Evaluate candidates deterministically using pure matching engine
  const bestMatch = findBestMatch(caller, pool);
  if (!bestMatch) {
    return {
      matched: false,
      message: "No compatible match found"
    };
  }

  // 5. Locate candidate doc for precondition check
  const candidate = pool.find(c => c.firebaseUid.trim() === bestMatch.nurseBUid.trim());
  if (!candidate) {
    return {
      matched: false,
      message: "Candidate no longer in pool"
    };
  }

  // 6. Generate match identifiers and timestamps
  const matchId = options?.generateMatchId
    ? options.generateMatchId()
    : crypto.randomUUID();

  const now = options?.now ? options.now() : new Date();
  const nowIso = now.toISOString();

  const expiresAt = new Date(now.getTime() + expirationHours * 3600 * 1000);
  const expiresAtIso = expiresAt.toISOString();

  const projectId = firestoreClient.getProjectId();

  // 7. Build atomic writes with preconditions
  const writeCaller: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, cleanCallerUid),
      fields: {
        locked: { booleanValue: true },
        currentMatchId: { stringValue: matchId },
        status: { stringValue: "MATCHED" },
        updatedAt: { stringValue: nowIso }
      }
    },
    updateMask: {
      fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"]
    },
    currentDocument: {
      exists: true,
      ...(caller.updateTime ? { updateTime: caller.updateTime } : {})
    }
  };

  const writeCandidate: FirestoreWrite = {
    update: {
      name: getTransferRequestDocPath(projectId, bestMatch.nurseBUid),
      fields: {
        locked: { booleanValue: true },
        currentMatchId: { stringValue: matchId },
        status: { stringValue: "MATCHED" },
        updatedAt: { stringValue: nowIso }
      }
    },
    updateMask: {
      fieldPaths: ["locked", "currentMatchId", "status", "updatedAt"]
    },
    currentDocument: {
      exists: true,
      ...(candidate.updateTime ? { updateTime: candidate.updateTime } : {})
    }
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
        updatedAt: { stringValue: nowIso }
      }
    },
    currentDocument: {
      exists: false
    }
  };

  // 8. Commit writes atomically
  try {
    await firestoreClient.commitAtomicMatch([writeCaller, writeCandidate, writeMatch]);
  } catch (err: unknown) {
    if (err instanceof FirestoreError) {
      if (err.statusCode === 409 || err.statusCode === 400) {
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
