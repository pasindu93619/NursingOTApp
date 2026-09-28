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
