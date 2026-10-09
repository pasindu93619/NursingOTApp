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

export const CHAT_DURATION_HOURS = 72; // 3 days for CHAT_OPEN phase
export const SLIDING_WINDOW_HOURS = 24; // 24 hours after first response

/**
 * Responds to a matched transfer request with an ACCEPT, REJECT, or CONFIRM decision.
 *
 * Workflow State Machine:
 * 1. PENDING_CONFIRMATION:
 *    - REJECT -> CANCELLED (releases all participants to SEARCHING)
 *    - ACCEPT:
 *      - If first accept, sliding window triggers: firstResponseAt recorded, expiresAt tightened to min(expiresAt, now + 24h).
 *      - If all participants accept (2 in 2-way, 3 in 3-way) -> transitions to CHAT_OPEN.
 *        chatDeadline is established (now + 72h) and expiresAt is updated to chatDeadline.
 *      - Otherwise remains PENDING_CONFIRMATION awaiting remaining responses.
 * 2. CHAT_OPEN:
 *    - REJECT / LEAVE -> CANCELLED (releases all participants to SEARCHING)
 *    - CONFIRM only:
 *      - Marks caller's confirmedBy flag.
 *      - If all participants confirm -> transitions to CONFIRMED.
 *      - Otherwise remains CHAT_OPEN awaiting remaining confirmations.
 *    - ACCEPT is invalid here; accepting a match is never final confirmation.
 */
export async function respondToMatch(
  callerUid: string,
  matchId: string,
  decision: "ACCEPT" | "REJECT" | "CONFIRM" | "LEAVE",
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
  const firstResponseAtStr = getString("firstResponseAt");
  const chatDeadlineStr = getString("chatDeadline");
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
  let confirmedKey: string;

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
      confirmedKey = "confirmedByA";
    } else if (isParticipantB) {
      acceptedKey = "acceptedByB";
      rejectedKey = "rejectedByB";
      confirmedKey = "confirmedByB";
    } else {
      acceptedKey = "acceptedByC";
      rejectedKey = "rejectedByC";
      confirmedKey = "confirmedByC";
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
    confirmedKey = isParticipantA ? "confirmedByA" : "confirmedByB";
  }

  // 3. Verify match is actionable
  if (status !== "PENDING_CONFIRMATION" && status !== "CHAT_OPEN") {
    throw new MatchServiceError("Match is not awaiting confirmation or chat finalization", 409, "INVALID_STATUS");
  }

  // Enforce the two distinct decision stages at the trusted backend boundary.
  // Stage 1: ACCEPT/REJECT while the proposed match is pending.
  // Stage 2: CONFIRM/REJECT/LEAVE after the coordination chat opens.
  // Never reinterpret ACCEPT as final agreement, or CONFIRM as initial acceptance.
  if (status === "PENDING_CONFIRMATION" && decision === "CONFIRM") {
    throw new MatchServiceError(
      "Final confirmation is available only after all nurses accept and the chat opens",
      400,
      "INVALID_DECISION"
    );
  }
  if (status === "CHAT_OPEN" && decision === "ACCEPT") {
    throw new MatchServiceError(
      "The match has already been accepted; use CONFIRM after discussion to finalize it",
      400,
      "INVALID_DECISION"
    );
  }

  const now = new Date();
  const expiresAt = new Date(expiresAtStr);
  if (now > expiresAt) {
    throw new MatchServiceError("Match deadline has expired", 410, "EXPIRED");
  }

  // 4. Verify participant response eligibility per state
  if (status === "PENDING_CONFIRMATION") {
    if (decision === "LEAVE") {
      throw new MatchServiceError("Cannot LEAVE before chat is open; use REJECT instead", 400, "INVALID_DECISION");
    }
    const alreadyAccepted = getBool(acceptedKey);
    const alreadyRejected = getBool(rejectedKey);
    if (alreadyAccepted || alreadyRejected) {
      throw new MatchServiceError("Participant has already responded", 409, "ALREADY_RESPONDED");
    }
  } else if (status === "CHAT_OPEN") {
    if (decision === "REJECT" || decision === "LEAVE") {
      // Allowed to reject/leave in CHAT_OPEN
    } else {
      // CONFIRM or ACCEPT
      const alreadyConfirmed = getBool(confirmedKey);
      if (alreadyConfirmed) {
        throw new MatchServiceError("Participant has already confirmed", 409, "ALREADY_CONFIRMED");
      }
    }
  }

  // 5. Determine new status, deadlines, and flags
  let newStatus = status;
  let newExpiresAtStr = expiresAtStr;
  let newFirstResponseAtStr = firstResponseAtStr;
  let newChatDeadlineStr = chatDeadlineStr;

  const matchFieldsToUpdate: Record<string, { stringValue?: string; booleanValue?: boolean; integerValue?: string }> = {
    updatedAt: { integerValue: Date.now().toString() }
  };
  const updateMaskFieldPaths: string[] = ["updatedAt"];

  if (decision === "REJECT" || decision === "LEAVE") {
    newStatus = "CANCELLED";
    matchFieldsToUpdate[rejectedKey] = { booleanValue: true };
    matchFieldsToUpdate[acceptedKey] = { booleanValue: false };
    matchFieldsToUpdate.status = { stringValue: newStatus };
    updateMaskFieldPaths.push(rejectedKey, acceptedKey, "status");
  } else if (status === "PENDING_CONFIRMATION") {
    // ACCEPT decision in PENDING_CONFIRMATION
    matchFieldsToUpdate[acceptedKey] = { booleanValue: true };
    matchFieldsToUpdate[rejectedKey] = { booleanValue: false };
    updateMaskFieldPaths.push(acceptedKey, rejectedKey);

    // Sliding window check: If this is the first response, set firstResponseAt and tighten deadline
    const anyPriorResponse =
      (isParticipantA ? false : Boolean(getBool("acceptedByA"))) ||
      (isParticipantB ? false : Boolean(getBool("acceptedByB"))) ||
      (hasParticipantC && !isParticipantC ? Boolean(getBool("acceptedByC")) : false);

    if (!firstResponseAtStr && !anyPriorResponse) {
      newFirstResponseAtStr = now.toISOString();
      matchFieldsToUpdate.firstResponseAt = { stringValue: newFirstResponseAtStr };
      matchFieldsToUpdate.firstResponseAtMs = { integerValue: now.getTime().toString() };
      updateMaskFieldPaths.push("firstResponseAt", "firstResponseAtMs");

      const slidingDeadline = new Date(now.getTime() + SLIDING_WINDOW_HOURS * 3600 * 1000);
      if (slidingDeadline < expiresAt) {
        newExpiresAtStr = slidingDeadline.toISOString();
        matchFieldsToUpdate.expiresAt = { stringValue: newExpiresAtStr };
        matchFieldsToUpdate.expiresAtMs = { integerValue: slidingDeadline.getTime().toString() };
        updateMaskFieldPaths.push("expiresAt", "expiresAtMs");
      }
    }

    // Check if all participants have now accepted
    let allAccepted = false;
    if (hasParticipantC) {
      const acceptedA = isParticipantA ? true : Boolean(getBool("acceptedByA"));
      const acceptedB = isParticipantB ? true : Boolean(getBool("acceptedByB"));
      const acceptedC = isParticipantC ? true : Boolean(getBool("acceptedByC"));
      const anyRejected =
        Boolean(getBool("rejectedByA")) ||
        Boolean(getBool("rejectedByB")) ||
        Boolean(getBool("rejectedByC"));

      if (anyRejected) {
        newStatus = "CANCELLED";
      } else if (acceptedA && acceptedB && acceptedC) {
        allAccepted = true;
      }
    } else {
      const acceptedA = isParticipantA ? true : Boolean(getBool("acceptedByA"));
      const acceptedB = isParticipantB ? true : Boolean(getBool("acceptedByB"));
      const anyRejected = Boolean(getBool("rejectedByA")) || Boolean(getBool("rejectedByB"));

      if (anyRejected) {
        newStatus = "CANCELLED";
      } else if (acceptedA && acceptedB) {
        allAccepted = true;
      }
    }

    if (newStatus !== "CANCELLED") {
      if (allAccepted) {
        // Transition to CHAT_OPEN
        newStatus = "CHAT_OPEN";
        const chatDeadline = new Date(now.getTime() + CHAT_DURATION_HOURS * 3600 * 1000);
        newChatDeadlineStr = chatDeadline.toISOString();
        newExpiresAtStr = newChatDeadlineStr;

        matchFieldsToUpdate.status = { stringValue: newStatus };
        matchFieldsToUpdate.chatDeadline = { stringValue: newChatDeadlineStr };
        matchFieldsToUpdate.chatDeadlineMs = { integerValue: chatDeadline.getTime().toString() };
        matchFieldsToUpdate.expiresAt = { stringValue: newExpiresAtStr };
        matchFieldsToUpdate.expiresAtMs = { integerValue: chatDeadline.getTime().toString() };
        matchFieldsToUpdate.confirmedByA = { booleanValue: false };
        matchFieldsToUpdate.confirmedByB = { booleanValue: false };
        updateMaskFieldPaths.push("status", "chatDeadline", "chatDeadlineMs", "expiresAt", "expiresAtMs", "confirmedByA", "confirmedByB");

        if (hasParticipantC) {
          matchFieldsToUpdate.confirmedByC = { booleanValue: false };
          updateMaskFieldPaths.push("confirmedByC");
        }
      } else {
        newStatus = "PENDING_CONFIRMATION";
        matchFieldsToUpdate.status = { stringValue: newStatus };
        updateMaskFieldPaths.push("status");
      }
    } else {
      matchFieldsToUpdate.status = { stringValue: newStatus };
      updateMaskFieldPaths.push("status");
    }
  } else if (status === "CHAT_OPEN") {
    // Decision is CONFIRM or ACCEPT in CHAT_OPEN
    matchFieldsToUpdate[confirmedKey] = { booleanValue: true };
    updateMaskFieldPaths.push(confirmedKey);

    let allConfirmed = false;
    if (hasParticipantC) {
      const confirmedA = isParticipantA ? true : Boolean(getBool("confirmedByA"));
      const confirmedB = isParticipantB ? true : Boolean(getBool("confirmedByB"));
      const confirmedC = isParticipantC ? true : Boolean(getBool("confirmedByC"));
      if (confirmedA && confirmedB && confirmedC) {
        allConfirmed = true;
      }
    } else {
      const confirmedA = isParticipantA ? true : Boolean(getBool("confirmedByA"));
      const confirmedB = isParticipantB ? true : Boolean(getBool("confirmedByB"));
      if (confirmedA && confirmedB) {
        allConfirmed = true;
      }
    }

    if (allConfirmed) {
      newStatus = "CONFIRMED";
      matchFieldsToUpdate.status = { stringValue: newStatus };
      updateMaskFieldPaths.push("status");
    } else {
      newStatus = "CHAT_OPEN";
    }
  }

  // 6. Build Firestore write with precondition on updateTime
  const writeMatch: FirestoreWrite = {
    update: {
      name: getMatchDocPath(firestoreClient.getProjectId(), matchId),
      fields: matchFieldsToUpdate
    },
    updateMask: { fieldPaths: updateMaskFieldPaths },
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
          const freshExpiresAt = readStringField(freshFields, "expiresAt") ?? newExpiresAtStr;
          const freshChatDeadline = readStringField(freshFields, "chatDeadline") ?? newChatDeadlineStr;
          const freshFirstResponseAt = readStringField(freshFields, "firstResponseAt") ?? newFirstResponseAtStr;
          const freshAccepted = readBooleanField(freshFields, acceptedKey);
          const freshRejected = readBooleanField(freshFields, rejectedKey);
          const freshConfirmed = readBooleanField(freshFields, confirmedKey);

          if (
            (decision === "ACCEPT" && freshAccepted) ||
            ((decision === "REJECT" || decision === "LEAVE") && freshRejected) ||
            (decision === "CONFIRM" && freshConfirmed)
          ) {
            return {
              matchId,
              newStatus: freshStatus,
              expiresAt: freshExpiresAt,
              chatDeadline: freshChatDeadline,
              firstResponseAt: freshFirstResponseAt,
              decisionApplied: true
            };
          }

          if (freshStatus === "CANCELLED" || freshStatus === "CONFIRMED") {
            return {
              matchId,
              newStatus: freshStatus,
              expiresAt: freshExpiresAt,
              chatDeadline: freshChatDeadline,
              firstResponseAt: freshFirstResponseAt,
              decisionApplied:
                (decision === "ACCEPT" && (freshStatus === "CHAT_OPEN" || freshStatus === "CONFIRMED")) ||
                (decision === "CONFIRM" && freshStatus === "CONFIRMED") ||
                ((decision === "REJECT" || decision === "LEAVE") && freshStatus === "CANCELLED")
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
    expiresAt: newExpiresAtStr,
    chatDeadline: newChatDeadlineStr,
    firstResponseAt: newFirstResponseAtStr,
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
    (status !== undefined &&
      status !== "PENDING_CONFIRMATION" &&
      status !== "CHAT_OPEN" &&
      status !== "CONFIRMED")
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
          expiresAtMs: { integerValue: expiresAt.getTime().toString() },
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
        expiresAtMs: { integerValue: expiresAt.getTime().toString() },
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
 * Sweeps active PENDING_CONFIRMATION and CHAT_OPEN matches that have passed their expiration deadline.
 * Marks expired matches as EXPIRED and unlocks/resets their participants back to SEARCHING.
 *
 * Invariants:
 * - Scans PENDING_CONFIRMATION and CHAT_OPEN matches where now > expiresAt.
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
      if (status !== "PENDING_CONFIRMATION" && status !== "CHAT_OPEN") continue;

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
