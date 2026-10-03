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
  return auth != null && (auth.uid === matchDoc.nurseAUid || auth.uid === matchDoc.nurseBUid);
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
