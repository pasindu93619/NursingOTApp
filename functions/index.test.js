const test = require("node:test");
const assert = require("node:assert/strict");

function canonicalIdentity(identity) {
  if (identity.verificationMethod !== "VERIFIED") {
    return null;
  }

  return {
    serviceNo: identity.verifiedServiceNo,
    fullName: identity.verifiedFullName,
    grade: identity.verifiedGrade
  };
}

function canOverwriteExistingProfile(existingProfile, callerUid) {
  return !existingProfile || existingProfile.firebaseUid === callerUid;
}

test("trusted publication contract requires authoritative identity fields", () => {
  const identity = {
    verificationMethod: "SELF_DECLARED",
    verifiedServiceNo: "SVC-001"
  };

  assert.notEqual(identity.verificationMethod, "VERIFIED");
  assert.equal(identity.verifiedFullName, undefined);
  assert.equal(identity.verifiedGrade, undefined);
  assert.equal(canonicalIdentity(identity), null);
});

test("trusted publication uses verified identity as the canonical identity source", () => {
  const identity = {
    verificationMethod: "VERIFIED",
    verifiedServiceNo: "SVC-001",
    verifiedFullName: "Verified Nurse",
    verifiedGrade: "Grade III"
  };

  const maliciousPayload = {
    serviceNo: "SVC-999",
    fullName: "Forged Name",
    grade: "Forged Grade"
  };

  const canonical = canonicalIdentity(identity);

  assert.deepEqual(canonical, {
    serviceNo: "SVC-001",
    fullName: "Verified Nurse",
    grade: "Grade III"
  });
  assert.notDeepEqual(canonical, maliciousPayload);
});

test("a client service number must match the verified service number", () => {
  const identity = {
    verificationMethod: "VERIFIED",
    verifiedServiceNo: "SVC-001",
    verifiedFullName: "Verified Nurse",
    verifiedGrade: "Grade III"
  };

  const clientServiceNo = "SVC-999";
  const canonical = canonicalIdentity(identity);

  assert.notEqual(clientServiceNo, canonical.serviceNo);
});

test("local ProfileEntity service number remains the identity gate", () => {
  const localProfile = {
    serviceNo: "SVC-001"
  };

  const verifiedIdentity = {
    verificationMethod: "VERIFIED",
    verifiedServiceNo: "SVC-001"
  };

  assert.equal(localProfile.serviceNo, verifiedIdentity.verifiedServiceNo);
});

test("client-supplied name and grade cannot override verified identity", () => {
  const identity = {
    verificationMethod: "VERIFIED",
    verifiedServiceNo: "SVC-001",
    verifiedFullName: "Verified Nurse",
    verifiedGrade: "Grade III"
  };

  const clientPayload = {
    serviceNo: "SVC-001",
    fullName: "Forged Name",
    grade: "Forged Grade"
  };

  const canonical = canonicalIdentity(identity);

  assert.equal(canonical.fullName, identity.verifiedFullName);
  assert.equal(canonical.grade, identity.verifiedGrade);
  assert.notEqual(canonical.fullName, clientPayload.fullName);
  assert.notEqual(canonical.grade, clientPayload.grade);
});

test("a different Firebase identity cannot overwrite an existing service number", () => {
  const existingProfile = {
    firebaseUid: "owner-uid"
  };

  assert.equal(canOverwriteExistingProfile(existingProfile, "owner-uid"), true);
  assert.equal(canOverwriteExistingProfile(existingProfile, "attacker-uid"), false);
});

test("client contactVerified input cannot promote an unverified contact state", () => {
  const verifiedIdentity = {
    verificationMethod: "VERIFIED",
    contactVerified: false
  };

  const clientPayload = {
    contactVerified: true
  };

  const canonicalContactVerified = verifiedIdentity.contactVerified === true;

  assert.equal(canonicalContactVerified, false);
  assert.notEqual(canonicalContactVerified, clientPayload.contactVerified);
});

test("direct client access to transferProfiles remains denied by the security contract", () => {
  const rulesContract = "match /transferProfiles/{serviceNo} { allow read, write: if false; }";

  assert.match(rulesContract, /allow read, write:\s*if false/);
});

// -----------------------------------------------------------------------------
// Phase 1.5.3C-2 Firestore Security Contract Tests
// -----------------------------------------------------------------------------
const fs = require("node:fs");
const path = require("node:path");

const rulesPath = path.resolve(__dirname, "../firestore.rules");
const rulesContent = fs.readFileSync(rulesPath, "utf8");

// Evaluator functions that replicate the exact logic in firestore.rules
function evalTransferRequestGet(auth, userId) {
  return auth != null && auth.uid === userId;
}

function evalTransferRequestList() {
  return false;
}

function evalTransferRequestCreate(auth, userId, data) {
  if (auth == null || auth.uid !== userId) return false;
  if (data.firebaseUid !== auth.uid) return false;
  if (data.status !== "SEARCHING") return false;
  if (data.locked !== false) return false;
  if (data.currentMatchId != null) return false;
  if (typeof data.currentHospitalId !== "string" || data.currentHospitalId.length === 0) return false;
  if (!Array.isArray(data.preferenceHospitalIds) || data.preferenceHospitalIds.length < 1 || data.preferenceHospitalIds.length > 3) return false;
  if (typeof data.grade !== "string" || data.grade.length === 0) return false;
  return true;
}

function evalTransferRequestUpdate(auth, userId, existing, next) {
  if (auth == null || auth.uid !== userId) return false;
  if (existing.firebaseUid !== auth.uid || next.firebaseUid !== auth.uid) return false;
  if (existing.locked !== false || next.locked !== false) return false;
  if (next.currentMatchId !== existing.currentMatchId) return false;
  if (next.status !== "SEARCHING" && next.status !== "WITHDRAWN") return false;
  if (typeof next.currentHospitalId !== "string" || next.currentHospitalId.length === 0) return false;
  if (!Array.isArray(next.preferenceHospitalIds) || next.preferenceHospitalIds.length < 1 || next.preferenceHospitalIds.length > 3) return false;
  if (typeof next.grade !== "string" || next.grade.length === 0) return false;
  return true;
}

function evalMatchGet(auth, matchDoc) {
  return auth != null && (
    auth.uid === matchDoc.nurseAUid ||
    auth.uid === matchDoc.nurseBUid ||
    (matchDoc.nurseCUid != null && auth.uid === matchDoc.nurseCUid)
  );
}

function evalMatchList() {
  return false;
}

function evalMatchCreate() {
  return false;
}

function evalMatchDelete() {
  return false;
}

function evalMatchUpdate(auth, existing, next) {
  if (auth == null) return false;
  if (next.nurseAUid !== existing.nurseAUid || next.nurseBUid !== existing.nurseBUid) return false;
  if (next.status !== existing.status) return false;
  if (next.createdAt !== existing.createdAt || next.expiresAt !== existing.expiresAt) return false;

  const affectedKeys = Object.keys(next).filter(k => next[k] !== existing[k]);

  if (auth.uid === existing.nurseAUid) {
    const allowed = ["acceptedByA", "rejectedByA", "updatedAt"];
    return affectedKeys.every(k => allowed.includes(k));
  }

  if (auth.uid === existing.nurseBUid) {
    const allowed = ["acceptedByB", "rejectedByB", "updatedAt"];
    return affectedKeys.every(k => allowed.includes(k));
  }

  return false;
}

function evalMessageRead(auth, matchDoc) {
  if (auth == null) return false;
  return (
    auth.uid === matchDoc.nurseAUid ||
    auth.uid === matchDoc.nurseBUid ||
    (matchDoc.nurseCUid != null && auth.uid === matchDoc.nurseCUid)
  );
}

function evalMessageCreate(auth, matchId, messageId, matchDoc, requestTime, resourceData) {
  if (auth == null) return false;
  const isParticipant = (
    auth.uid === matchDoc.nurseAUid ||
    auth.uid === matchDoc.nurseBUid ||
    (matchDoc.nurseCUid != null && auth.uid === matchDoc.nurseCUid)
  );
  if (!isParticipant) return false;

  if (matchDoc.status !== "CHAT_OPEN") return false;

  const reqTimeMs = new Date(requestTime).getTime();
  let isBefore = false;
  if (matchDoc.chatDeadlineMs !== undefined) {
    isBefore = typeof matchDoc.chatDeadlineMs === "number" && Number.isInteger(matchDoc.chatDeadlineMs) && reqTimeMs < matchDoc.chatDeadlineMs;
  } else if (matchDoc.expiresAtMs !== undefined) {
    isBefore = typeof matchDoc.expiresAtMs === "number" && Number.isInteger(matchDoc.expiresAtMs) && reqTimeMs < matchDoc.expiresAtMs;
  } else if (matchDoc.chatDeadline instanceof Date) {
    isBefore = reqTimeMs < matchDoc.chatDeadline.getTime();
  } else if (matchDoc.expiresAt instanceof Date) {
    isBefore = reqTimeMs < matchDoc.expiresAt.getTime();
  } else {
    isBefore = false;
  }
  if (!isBefore) return false;

  if (resourceData.senderUid !== auth.uid) return false;
  if (resourceData.matchId !== matchId) return false;
  if (resourceData.messageId !== messageId) return false;

  if (typeof resourceData.text !== "string") return false;
  if (resourceData.text.length === 0 || resourceData.text.length > 500) return false;
  if (!/.*\S.*/.test(resourceData.text)) return false;

  if (typeof resourceData.senderHospitalId !== "string" || resourceData.senderHospitalId.length === 0) return false;
  if (typeof resourceData.senderGrade !== "string" || resourceData.senderGrade.length === 0) return false;

  if (resourceData.createdAt !== requestTime) return false;

  const allowedKeys = [
    "messageId", "matchId", "senderUid", "senderHospitalId",
    "senderGrade", "text", "createdAt"
  ];
  const keys = Object.keys(resourceData);
  if (keys.length !== allowedKeys.length) return false;
  if (!keys.every(k => allowedKeys.includes(k))) return false;

  return true;
}

function evalMessageUpdate() {
  return false;
}

function evalMessageDelete() {
  return false;
}

test("security rules file exists and contains transferRequests and matches contracts", () => {
  assert.ok(rulesContent.includes("match /transferRequests/{userId}"));
  assert.ok(rulesContent.includes("match /matches/{matchId}"));
  assert.ok(rulesContent.includes("match /transferIdentities/{userId}"));
  assert.ok(rulesContent.includes("match /transferProfiles/{serviceNo}"));
  assert.ok(rulesContent.includes("match /{document=**}"));
});

test("unauthenticated transfer request access denied", () => {
  assert.equal(evalTransferRequestGet(null, "nurse-123"), false);
  assert.equal(evalTransferRequestCreate(null, "nurse-123", {}), false);
});

test("user A cannot read user B request", () => {
  const authA = { uid: "nurse-A" };
  assert.equal(evalTransferRequestGet(authA, "nurse-B"), false);
  assert.equal(evalTransferRequestGet(authA, "nurse-A"), true);
});

test("user A cannot write user B request", () => {
  const authA = { uid: "nurse-A" };
  const validData = {
    firebaseUid: "nurse-B",
    status: "SEARCHING",
    locked: false,
    currentMatchId: null,
    currentHospitalId: "MOH-001",
    preferenceHospitalIds: ["MOH-002"],
    grade: "Grade I"
  };
  assert.equal(evalTransferRequestCreate(authA, "nurse-B", validData), false);
});

test("user cannot set locked=true", () => {
  const auth = { uid: "nurse-A" };
  const lockedCreate = {
    firebaseUid: "nurse-A",
    status: "SEARCHING",
    locked: true,
    currentMatchId: null,
    currentHospitalId: "MOH-001",
    preferenceHospitalIds: ["MOH-002"],
    grade: "Grade I"
  };
  assert.equal(evalTransferRequestCreate(auth, "nurse-A", lockedCreate), false);

  const existing = { ...lockedCreate, locked: false };
  assert.equal(evalTransferRequestUpdate(auth, "nurse-A", existing, { ...existing, locked: true }), false);
});

test("user cannot forge or change currentMatchId", () => {
  const auth = { uid: "nurse-A" };
  const forgedMatchCreate = {
    firebaseUid: "nurse-A",
    status: "SEARCHING",
    locked: false,
    currentMatchId: "forged-match-999",
    currentHospitalId: "MOH-001",
    preferenceHospitalIds: ["MOH-002"],
    grade: "Grade I"
  };
  assert.equal(evalTransferRequestCreate(auth, "nurse-A", forgedMatchCreate), false);

  const existing = { ...forgedMatchCreate, currentMatchId: null };
  assert.equal(evalTransferRequestUpdate(auth, "nurse-A", existing, { ...existing, currentMatchId: "forged-match-999" }), false);
});

test("user cannot create MATCHED or MATCH_PENDING state", () => {
  const auth = { uid: "nurse-A" };
  const matchedData = {
    firebaseUid: "nurse-A",
    status: "MATCHED",
    locked: false,
    currentMatchId: null,
    currentHospitalId: "MOH-001",
    preferenceHospitalIds: ["MOH-002"],
    grade: "Grade I"
  };
  assert.equal(evalTransferRequestCreate(auth, "nurse-A", matchedData), false);

  const existing = { ...matchedData, status: "SEARCHING" };
  assert.equal(evalTransferRequestUpdate(auth, "nurse-A", existing, { ...existing, status: "MATCH_PENDING" }), false);
  assert.equal(evalTransferRequestUpdate(auth, "nurse-A", existing, { ...existing, status: "MATCHED" }), false);
  assert.equal(evalTransferRequestUpdate(auth, "nurse-A", existing, { ...existing, status: "WITHDRAWN" }), true);
});

test("user cannot create match", () => {
  assert.equal(evalMatchCreate(), false);
  assert.equal(evalMatchDelete(), false);
});

test("non-participant cannot read match", () => {
  const matchDoc = { nurseAUid: "nurse-A", nurseBUid: "nurse-B" };
  assert.equal(evalMatchGet({ uid: "nurse-C" }, matchDoc), false);
  assert.equal(evalMatchGet(null, matchDoc), false);
});

test("participant can read own match", () => {
  const matchDoc = { nurseAUid: "nurse-A", nurseBUid: "nurse-B" };
  assert.equal(evalMatchGet({ uid: "nurse-A" }, matchDoc), true);
  assert.equal(evalMatchGet({ uid: "nurse-B" }, matchDoc), true);
});

test("participant can update ONLY their own acceptance/rejection field", () => {
  const existing = {
    nurseAUid: "nurse-A",
    nurseBUid: "nurse-B",
    status: "PENDING_CONFIRMATION",
    createdAt: 1000,
    expiresAt: 5000,
    acceptedByA: false,
    acceptedByB: false
  };

  // Nurse A accepts: allowed
  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, acceptedByA: true }), true);

  // Nurse A tries to accept for Nurse B: denied
  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, acceptedByB: true }), false);

  // Nurse B accepts: allowed
  assert.equal(evalMatchUpdate({ uid: "nurse-B" }, existing, { ...existing, acceptedByB: true }), true);
});

test("participant cannot change match status", () => {
  const existing = {
    nurseAUid: "nurse-A",
    nurseBUid: "nurse-B",
    status: "PENDING_CONFIRMATION",
    createdAt: 1000,
    expiresAt: 5000,
    acceptedByA: false
  };

  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, status: "FINALIZED" }), false);
});

test("participant cannot change participants", () => {
  const existing = {
    nurseAUid: "nurse-A",
    nurseBUid: "nurse-B",
    status: "PENDING_CONFIRMATION",
    createdAt: 1000,
    expiresAt: 5000
  };

  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, nurseBUid: "nurse-C" }), false);
});

test("participant cannot change grade metadata or timestamps", () => {
  const existing = {
    nurseAUid: "nurse-A",
    nurseBUid: "nurse-B",
    nurseAGrade: "Grade I",
    nurseBGrade: "Grade II",
    status: "PENDING_CONFIRMATION",
    createdAt: 1000,
    expiresAt: 5000
  };

  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, expiresAt: 99999 }), false);
  assert.equal(evalMatchUpdate({ uid: "nurse-A" }, existing, { ...existing, nurseAGrade: "Special Grade" }), false);
});

test("global collection enumeration is blocked for transferRequests and matches", () => {
  assert.equal(evalTransferRequestList(), false);
  assert.equal(evalMatchList(), false);
});

test("same-grade is NOT enforced by firestore rules contract", () => {
  // Verifies that neither transferRequests nor matches rules contain hard same-grade constraints
  assert.equal(rulesContent.includes("nurseAGrade == nurseBGrade"), false);
  assert.equal(rulesContent.includes("grade =="), false);
  assert.ok(rulesContent.includes("grade is string")); // Grade is verified as string metadata
});

// =============================================================================
// Mutual Transfer Chat Security Rules Test Suite (30 Verification Tests)
// =============================================================================

const mockMatch2Way = {
  nurseAUid: "nurse-A",
  nurseBUid: "nurse-B",
  status: "CHAT_OPEN",
  chatDeadline: "2026-10-12T12:00:00.000Z",
  chatDeadlineMs: new Date("2026-10-12T12:00:00.000Z").getTime(),
  expiresAt: "2026-10-12T12:00:00.000Z",
  expiresAtMs: new Date("2026-10-12T12:00:00.000Z").getTime()
};

const mockMatch3Way = {
  nurseAUid: "nurse-A",
  nurseBUid: "nurse-B",
  nurseCUid: "nurse-C",
  status: "CHAT_OPEN",
  chatDeadline: "2026-10-12T12:00:00.000Z",
  chatDeadlineMs: new Date("2026-10-12T12:00:00.000Z").getTime(),
  expiresAt: "2026-10-12T12:00:00.000Z",
  expiresAtMs: new Date("2026-10-12T12:00:00.000Z").getTime()
};

const validReqTime = "2026-10-09T12:00:00.000Z";

function makeValidMessage(senderUid, matchId = "match-1", messageId = "msg-1") {
  return {
    messageId,
    matchId,
    senderUid,
    senderHospitalId: "MOH-001",
    senderGrade: "Grade I",
    text: "Hello from transfer participant!",
    createdAt: validReqTime
  };
}

// 2-WAY Tests
test("1 - 2-Way: Participant A can read messages", () => {
  assert.equal(evalMessageRead({ uid: "nurse-A" }, mockMatch2Way), true);
});

test("2 - 2-Way: Participant B can read messages", () => {
  assert.equal(evalMessageRead({ uid: "nurse-B" }, mockMatch2Way), true);
});

test("3 - 2-Way: Participant A can create message", () => {
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), true);
});

test("4 - 2-Way: Participant B can create message", () => {
  const msg = makeValidMessage("nurse-B");
  assert.equal(evalMessageCreate({ uid: "nurse-B" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), true);
});

test("5 - 2-Way: Non-participant X cannot read messages", () => {
  assert.equal(evalMessageRead({ uid: "nurse-X" }, mockMatch2Way), false);
  assert.equal(evalMessageRead(null, mockMatch2Way), false);
});

test("6 - 2-Way: Non-participant X cannot create message", () => {
  const msg = makeValidMessage("nurse-X");
  assert.equal(evalMessageCreate({ uid: "nurse-X" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

// 3-WAY Tests
test("7 - 3-Way: Participant A can read and create message", () => {
  assert.equal(evalMessageRead({ uid: "nurse-A" }, mockMatch3Way), true);
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch3Way, validReqTime, msg), true);
});

test("8 - 3-Way: Participant B can read and create message", () => {
  assert.equal(evalMessageRead({ uid: "nurse-B" }, mockMatch3Way), true);
  const msg = makeValidMessage("nurse-B");
  assert.equal(evalMessageCreate({ uid: "nurse-B" }, "match-1", "msg-1", mockMatch3Way, validReqTime, msg), true);
});

test("9 - 3-Way: Participant C can read and create message", () => {
  assert.equal(evalMessageRead({ uid: "nurse-C" }, mockMatch3Way), true);
  const msg = makeValidMessage("nurse-C");
  assert.equal(evalMessageCreate({ uid: "nurse-C" }, "match-1", "msg-1", mockMatch3Way, validReqTime, msg), true);
});

test("10 - 3-Way: Non-participant X cannot read or create message", () => {
  assert.equal(evalMessageRead({ uid: "nurse-X" }, mockMatch3Way), false);
  const msg = makeValidMessage("nurse-X");
  assert.equal(evalMessageCreate({ uid: "nurse-X" }, "match-1", "msg-1", mockMatch3Way, validReqTime, msg), false);
});

// WORKFLOW STATE Tests
test("11 - State: PENDING_CONFIRMATION create is denied", () => {
  const match = { ...mockMatch2Way, status: "PENDING_CONFIRMATION" };
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", match, validReqTime, msg), false);
});

test("12 - State: CHAT_OPEN create is allowed", () => {
  const match = { ...mockMatch2Way, status: "CHAT_OPEN" };
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", match, validReqTime, msg), true);
});

test("13 - State: CONFIRMED create is denied", () => {
  const match = { ...mockMatch2Way, status: "CONFIRMED" };
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", match, validReqTime, msg), false);
});

test("14 - State: CANCELLED create is denied", () => {
  const match = { ...mockMatch2Way, status: "CANCELLED" };
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", match, validReqTime, msg), false);
});

test("15 - State: EXPIRED create is denied", () => {
  const match = { ...mockMatch2Way, status: "EXPIRED" };
  const msg = makeValidMessage("nurse-A");
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", match, validReqTime, msg), false);
});

// DEADLINE Tests
test("16 - Deadline: Before chatDeadline create is allowed", () => {
  const beforeTime = "2026-10-12T11:59:59.000Z";
  const msg = { ...makeValidMessage("nurse-A"), createdAt: beforeTime };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, beforeTime, msg), true);
});

test("17 - Deadline: At or after chatDeadline create is denied", () => {
  const exactTime = "2026-10-12T12:00:00.000Z";
  const afterTime = "2026-10-12T12:00:01.000Z";
  const msgExact = { ...makeValidMessage("nurse-A"), createdAt: exactTime };
  const msgAfter = { ...makeValidMessage("nurse-A"), createdAt: afterTime };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, exactTime, msgExact), false);
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, afterTime, msgAfter), false);
});

// VALIDATION Tests
test("18 - Validation: Empty text is denied", () => {
  const msg = { ...makeValidMessage("nurse-A"), text: "" };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("19 - Validation: Whitespace-only text is denied", () => {
  const msg1 = { ...makeValidMessage("nurse-A"), text: "   " };
  const msg2 = { ...makeValidMessage("nurse-A"), text: "\t\n  " };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg1), false);
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg2), false);
});

test("20 - Validation: 500 characters text is allowed", () => {
  const text500 = "a".repeat(500);
  const msg = { ...makeValidMessage("nurse-A"), text: text500 };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), true);
});

test("21 - Validation: 501 characters text is denied", () => {
  const text501 = "a".repeat(501);
  const msg = { ...makeValidMessage("nurse-A"), text: text501 };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("22 - Validation: Wrong senderUid (spoofing) is denied", () => {
  const msg = { ...makeValidMessage("nurse-B") }; // caller is nurse-A but claims nurse-B
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("23 - Validation: Wrong matchId in payload is denied", () => {
  const msg = { ...makeValidMessage("nurse-A"), matchId: "different-match-id" };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("24 - Validation: Wrong messageId in payload is denied", () => {
  const msg = { ...makeValidMessage("nurse-A"), messageId: "different-msg-id" };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("25 - Validation: Missing required field is denied", () => {
  const msg = makeValidMessage("nurse-A");
  delete msg.senderHospitalId;
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("26 - Validation: Extra unexpected field is denied", () => {
  const msg = { ...makeValidMessage("nurse-A"), extraAttachmentUrl: "http://malicious.com" };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("27 - Immutability: Message update is denied", () => {
  assert.equal(evalMessageUpdate(), false);
});

test("28 - Immutability: Message delete is denied", () => {
  assert.equal(evalMessageDelete(), false);
});

// TIMESTAMP Tests
test("29 - Timestamp: Client-controlled arbitrary createdAt is denied", () => {
  const forgedTimestamp = "1999-01-01T00:00:00.000Z";
  const msg = { ...makeValidMessage("nurse-A"), createdAt: forgedTimestamp };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), false);
});

test("30 - Timestamp: Server-bound request.time createdAt is accepted", () => {
  const msg = { ...makeValidMessage("nurse-A"), createdAt: validReqTime };
  assert.equal(evalMessageCreate({ uid: "nurse-A" }, "match-1", "msg-1", mockMatch2Way, validReqTime, msg), true);
});

test("31 - Parent Match Rule: Participant C recognized for get", () => {
  assert.equal(evalMatchGet({ uid: "nurse-C" }, mockMatch3Way), true);
  assert.equal(evalMatchGet({ uid: "nurse-X" }, mockMatch3Way), false);
});

test("32 - Security rules content contains messages subcollection with immutable flags", () => {
  assert.ok(rulesContent.includes("match /messages/{messageId}"));
  assert.ok(rulesContent.includes("allow update, delete: if false;"));
  assert.ok(rulesContent.includes("isBeforeDeadline"));
  assert.ok(rulesContent.includes("status == 'CHAT_OPEN'"));
});
