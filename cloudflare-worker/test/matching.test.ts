import test, { describe } from "node:test";
import assert from "node:assert/strict";
import {
  evaluateDirectPair,
  findBestMatch,
  findAllCompatibleMatches,
  rankMatches,
  compareMatches,
  type CandidateRequest,
  type DirectMatch
} from "../src/matching/matchingEngine.ts";

function createValidRequest(overrides: Partial<CandidateRequest> = {}): CandidateRequest {
  return {
    firebaseUid: "nurse-A",
    status: "SEARCHING",
    locked: false,
    currentMatchId: null,
    currentHospitalId: "HOSP-001",
    preferenceHospitalIds: ["HOSP-002", "HOSP-003"],
    grade: "Grade I",
    ...overrides
  };
}

describe("Pure 2-Way Mutual Transfer Matching Engine", () => {
  // Test 1: Direct 2-way compatible pair
  test("1 - direct 2-way compatible pair succeeds with accurate mapping", () => {
    const nurseA = createValidRequest({
      firebaseUid: "nurse-A",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"],
      grade: "Grade I"
    });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, true);
    assert.ok(result.match);
    assert.equal(result.match.nurseAUid, "nurse-A");
    assert.equal(result.match.nurseBUid, "nurse-B");
    assert.equal(result.match.nurseACurrentHospitalId, "HOSP-001");
    assert.equal(result.match.nurseBCurrentHospitalId, "HOSP-002");
    assert.equal(result.match.nurseADestinationHospitalId, "HOSP-002");
    assert.equal(result.match.nurseBDestinationHospitalId, "HOSP-001");
    assert.equal(result.match.nurseAPreferenceRank, 1);
    assert.equal(result.match.nurseBPreferenceRank, 1);
    assert.equal(result.match.combinedPreferenceRank, 2);
    assert.equal(result.match.isSameGrade, true);
  });

  // Test 2: Same hospital rejected
  test("2 - same hospital rejected even if preferences include it", () => {
    const nurseA = createValidRequest({
      firebaseUid: "nurse-A",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-001"]
    });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, false);
    assert.match(result.ineligibilityReason || "", /same hospital/i);
  });

  // Test 3: One-way preference rejected
  test("3 - one-way preference rejected (A wants B, but B does NOT want A)", () => {
    const nurseA = createValidRequest({
      firebaseUid: "nurse-A",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"]
    });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-003"] // Does NOT want HOSP-001
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, false);
    assert.match(result.ineligibilityReason || "", /Candidate nurse did not select source's current hospital/i);
  });

  // Test 4: Both-way preference accepted
  test("4 - both-way preference accepted when mutually selected", () => {
    const nurseA = createValidRequest({
      firebaseUid: "nurse-A",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-003", "HOSP-002"] // HOSP-002 is rank 2
    });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"] // HOSP-001 is rank 1
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, true);
    assert.equal(result.match?.nurseAPreferenceRank, 2);
    assert.equal(result.match?.nurseBPreferenceRank, 1);
    assert.equal(result.match?.combinedPreferenceRank, 3);
  });

  // Test 5: Inactive request rejected
  test("5 - inactive request rejected (status != SEARCHING)", () => {
    const nurseA = createValidRequest({ status: "PENDING" });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const resultA = evaluateDirectPair(nurseA, nurseB);
    assert.equal(resultA.isCompatible, false);
    assert.match(resultA.ineligibilityReason || "", /Source request status is 'PENDING'/i);

    const activeA = createValidRequest();
    const inactiveB = createValidRequest({
      firebaseUid: "nurse-B",
      status: "WITHDRAWN",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });
    const resultB = evaluateDirectPair(activeA, inactiveB);
    assert.equal(resultB.isCompatible, false);
    assert.match(resultB.ineligibilityReason || "", /Candidate request status is 'WITHDRAWN'/i);
  });

  // Test 6: Locked request rejected
  test("6 - locked request rejected (locked == true)", () => {
    const nurseA = createValidRequest({ locked: true });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const resultA = evaluateDirectPair(nurseA, nurseB);
    assert.equal(resultA.isCompatible, false);
    assert.match(resultA.ineligibilityReason || "", /Source request is already locked/i);

    const unlockedA = createValidRequest();
    const lockedB = createValidRequest({
      firebaseUid: "nurse-B",
      locked: true,
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });
    const resultB = evaluateDirectPair(unlockedA, lockedB);
    assert.equal(resultB.isCompatible, false);
    assert.match(resultB.ineligibilityReason || "", /Candidate request is already locked/i);
  });

  // Test 7: currentMatchId already present rejected
  test("7 - currentMatchId already present rejected", () => {
    const nurseA = createValidRequest({ currentMatchId: "match-123" });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, false);
    assert.match(result.ineligibilityReason || "", /already has an active matchId/i);
  });

  // Test 8: Self-match rejected
  test("8 - self-match rejected (same firebaseUid)", () => {
    const nurseA = createValidRequest({ firebaseUid: "nurse-same" });
    const nurseB = createValidRequest({
      firebaseUid: "nurse-same",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const result = evaluateDirectPair(nurseA, nurseB);
    assert.equal(result.isCompatible, false);
    assert.match(result.ineligibilityReason || "", /Self-match is rejected/i);
  });

  // Test 9: Same-grade candidate preferred over cross-grade candidate
  test("9 - same-grade candidate preferred over cross-grade candidate", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"],
      grade: "Grade I"
    });

    const crossGradeCandidate = createValidRequest({
      firebaseUid: "nurse-cross",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade II"
    });

    const sameGradeCandidate = createValidRequest({
      firebaseUid: "nurse-same-grade",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    // Even if crossGradeCandidate appears first in pool
    const pool = [crossGradeCandidate, sameGradeCandidate];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-same-grade");
    assert.equal(best.isSameGrade, true);
  });

  // Test 10: Cross-grade candidate accepted when no same-grade candidate exists
  test("10 - cross-grade candidate accepted when no same-grade candidate exists", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"],
      grade: "Grade I"
    });

    const crossGradeCandidate = createValidRequest({
      firebaseUid: "nurse-cross",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade III"
    });

    const pool = [crossGradeCandidate];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-cross");
    assert.equal(best.isSameGrade, false);
    assert.equal(best.nurseAGrade, "Grade I");
    assert.equal(best.nurseBGrade, "Grade III");
  });

  // Test 11: First preference outranks second preference
  test("11 - first preference outranks second preference among same-grade candidates", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002", "HOSP-003"],
      grade: "Grade I"
    });

    // Candidate at 2nd preference hospital (HOSP-003)
    const secondPrefCandidate = createValidRequest({
      firebaseUid: "nurse-second-pref",
      currentHospitalId: "HOSP-003",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    // Candidate at 1st preference hospital (HOSP-002)
    const firstPrefCandidate = createValidRequest({
      firebaseUid: "nurse-first-pref",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const pool = [secondPrefCandidate, firstPrefCandidate];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-first-pref");
    assert.equal(best.nurseAPreferenceRank, 1);
  });

  // Test 12: Second preference outranks third preference
  test("12 - second preference outranks third preference", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002", "HOSP-003", "HOSP-004"],
      grade: "Grade I"
    });

    const thirdPrefCandidate = createValidRequest({
      firebaseUid: "nurse-third",
      currentHospitalId: "HOSP-004",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const secondPrefCandidate = createValidRequest({
      firebaseUid: "nurse-second",
      currentHospitalId: "HOSP-003",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const pool = [thirdPrefCandidate, secondPrefCandidate];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-second");
    assert.equal(best.nurseAPreferenceRank, 2);
  });

  // Test 13: Mutual rank combination is deterministic
  test("13 - mutual rank combination is deterministic (lower combined sum preferred)", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002", "HOSP-003"],
      grade: "Grade I"
    });

    // Pair 1: Source rank 1, Candidate rank 2 -> Combined = 3
    const candidateRank2 = createValidRequest({
      firebaseUid: "nurse-candidate-rank2",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-999", "HOSP-001"], // HOSP-001 is rank 2
      grade: "Grade I"
    });

    // Pair 2: Source rank 1, Candidate rank 1 -> Combined = 2
    const candidateRank1 = createValidRequest({
      firebaseUid: "nurse-candidate-rank1",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"], // HOSP-001 is rank 1
      grade: "Grade I"
    });

    const pool = [candidateRank2, candidateRank1];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-candidate-rank1");
    assert.equal(best.combinedPreferenceRank, 2);
  });

  // Test 14: Deterministic tie-breaking
  test("14 - deterministic tie-breaking uses alphabetical candidate UID", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"],
      grade: "Grade I"
    });

    // Both candidates have identical ranks (rank 1 + rank 1 = 2) and same grade
    const candidateZ = createValidRequest({
      firebaseUid: "nurse-ZZZ",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const candidateA = createValidRequest({
      firebaseUid: "nurse-AAA",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const pool = [candidateZ, candidateA];
    const best = findBestMatch(source, pool);

    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-AAA");
  });

  // Test 15: Invalid/blank grade rejected
  test("15 - invalid or blank grade rejected for both source and candidate", () => {
    const blankGradeSource = createValidRequest({ grade: "" });
    const normalCandidate = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const resA = evaluateDirectPair(blankGradeSource, normalCandidate);
    assert.equal(resA.isCompatible, false);
    assert.match(resA.ineligibilityReason || "", /Missing or blank grade/i);

    const normalSource = createValidRequest();
    const blankGradeCandidate = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "   "
    });

    const resB = evaluateDirectPair(normalSource, blankGradeCandidate);
    assert.equal(resB.isCompatible, false);
    assert.match(resB.ineligibilityReason || "", /Missing or blank grade/i);
  });

  // Test 16: Empty preferences rejected
  test("16 - empty or blank hospital preferences rejected", () => {
    const emptyPrefs = createValidRequest({ preferenceHospitalIds: [] });
    const normalCandidate = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"]
    });

    const res = evaluateDirectPair(emptyPrefs, normalCandidate);
    assert.equal(res.isCompatible, false);
    assert.match(res.ineligibilityReason || "", /Preferences list cannot be empty/i);
  });

  // Test 17: Duplicate hospital preference handling
  test("17 - duplicate hospital preference handling uses first occurrence for rank", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002", "HOSP-002"], // duplicate
      grade: "Grade I"
    });
    const candidate = createValidRequest({
      firebaseUid: "nurse-B",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Grade I"
    });

    const res = evaluateDirectPair(source, candidate);
    assert.equal(res.isCompatible, true);
    assert.equal(res.match?.nurseAPreferenceRank, 1); // 1st occurrence
  });

  // Test 18: No compatible candidate returns no-match result
  test("18 - no compatible candidate returns null from findBestMatch and empty list from findAll", () => {
    const source = createValidRequest({
      firebaseUid: "nurse-source",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"]
    });

    const incompatibleCandidate = createValidRequest({
      firebaseUid: "nurse-other",
      currentHospitalId: "HOSP-999", // Not in source's preferences
      preferenceHospitalIds: ["HOSP-888"]
    });

    const pool = [incompatibleCandidate];
    const best = findBestMatch(source, pool);
    assert.equal(best, null);

    const all = findAllCompatibleMatches(source, pool);
    assert.equal(all.length, 0);
  });

  // Test 19: Engine NEVER excludes a candidate solely because grades differ
  test("19 - engine NEVER excludes a candidate solely because grades differ (cross-grade is valid)", () => {
    const nurseGrade1 = createValidRequest({
      firebaseUid: "nurse-1",
      currentHospitalId: "HOSP-001",
      preferenceHospitalIds: ["HOSP-002"],
      grade: "Grade I"
    });
    const nurseSpecialGrade = createValidRequest({
      firebaseUid: "nurse-special",
      currentHospitalId: "HOSP-002",
      preferenceHospitalIds: ["HOSP-001"],
      grade: "Special Grade"
    });

    const evalResult = evaluateDirectPair(nurseGrade1, nurseSpecialGrade);
    assert.equal(evalResult.isCompatible, true);
    assert.ok(evalResult.match);
    assert.equal(evalResult.match.isSameGrade, false);
    assert.equal(evalResult.match.nurseAGrade, "Grade I");
    assert.equal(evalResult.match.nurseBGrade, "Special Grade");

    // Pool with ONLY cross-grade candidate returns the match
    const best = findBestMatch(nurseGrade1, [nurseSpecialGrade]);
    assert.ok(best);
    assert.equal(best.nurseBUid, "nurse-special");
    assert.equal(best.isSameGrade, false);
  });
});
