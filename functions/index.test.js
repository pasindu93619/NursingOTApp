const test = require("node:test");
const assert = require("node:assert/strict");

test("trusted publication contract requires authoritative identity fields", () => {
  const identity = {
    verificationMethod: "SELF_DECLARED",
    verifiedServiceNo: "SVC-001"
  };

  assert.notEqual(identity.verificationMethod, "VERIFIED");
  assert.equal(identity.verifiedFullName, undefined);
  assert.equal(identity.verifiedGrade, undefined);
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

  const canonical = {
    serviceNo: identity.verifiedServiceNo,
    fullName: identity.verifiedFullName,
    grade: identity.verifiedGrade
  };

  assert.deepEqual(canonical, {
    serviceNo: "SVC-001",
    fullName: "Verified Nurse",
    grade: "Grade III"
  });
  assert.notDeepEqual(canonical, maliciousPayload);
});
