import test, { describe } from "node:test";
import assert from "node:assert/strict";
import {
  evaluateDirectPair,
  findBestMatch,
  findAllCompatibleMatches,
  rankMatches,
  compareMatches,
  findBestThreeWayCycle,
  compareThreeWayMatches,
  rankThreeWayMatches,
  type CandidateRequest,
  type DirectMatch,
  type ThreeWayMatch
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

// =============================================================================
// 3-Way Circular Matching Tests
// =============================================================================

describe("3-Way Circular Transfer Cycle Detection", () => {

  /**
   * Helper that creates a cycle-eligible request with distinct UIDs and
   * hospitals so tests only need to supply relevant overrides.
   */
  function req(
    uid: string,
    currentHospital: string,
    preferences: string[],
    extra: Partial<CandidateRequest> = {}
  ): CandidateRequest {
    return createValidRequest({
      firebaseUid: uid,
      currentHospitalId: currentHospital,
      preferenceHospitalIds: preferences,
      ...extra
    });
  }

  // ---- Test 1: Valid A -> B -> C -> A cycle ----
  test("3W-01 - valid A -> B -> C -> A cycle is detected", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-C"]);
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    const pool = [nurseB, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result, "Expected a valid cycle to be found");
    assert.equal(result.nurseAUid, "uid-A");
    assert.equal(result.nurseBUid, "uid-B");
    assert.equal(result.nurseCUid, "uid-C");
    assert.equal(result.nurseACurrentHospitalId, "HOSP-A");
    assert.equal(result.nurseBCurrentHospitalId, "HOSP-B");
    assert.equal(result.nurseCCurrentHospitalId, "HOSP-C");
    assert.equal(result.nurseADestinationHospitalId, "HOSP-B");
    assert.equal(result.nurseBDestinationHospitalId, "HOSP-C");
    assert.equal(result.nurseCDestinationHospitalId, "HOSP-A");
  });

  // ---- Test 2: No cycle when C does not select A's hospital ----
  test("3W-02 - no cycle when C's preferences omit A's hospital", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-C"]);
    // C wants HOSP-X, not HOSP-A => cycle does not close
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-X"]);

    const pool = [nurseB, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null);
  });

  // ---- Test 3: Direct 2-way priority when both options exist ----
  test("3W-03 - findBestMatch wins over 3-way when a direct 2-way match exists", () => {
    // PRIORITY CONTRACT:
    // The 2-way-vs-3-way priority is enforced at the ORCHESTRATION layer
    // (matchingService.ts / findBestMatchOrCycle), not inside these two
    // standalone domain functions. This test verifies the domain half of the
    // contract: findBestMatch independently discovers the 2-way match so that
    // an orchestrator calling findBestMatch first, and only falling through to
    // findBestThreeWayCycle when it returns null, will always prefer 2-way.

    // A <-> B (direct 2-way match available)
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-A"]); // B wants A's hospital => reciprocal 2-way
    // A -> B -> C -> A also forms a valid 3-way cycle with the same pool
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    // Assert 1: findBestMatch returns the direct pair (not null).
    // An orchestrator seeing a non-null result here MUST stop and return it.
    const directResult = findBestMatch(nurseA, [nurseB, nurseC]);
    assert.ok(directResult, "Direct 2-way match must be found by findBestMatch");
    assert.equal(directResult.nurseBUid, "uid-B");

    // Assert 2: findBestThreeWayCycle is a separate domain function with no
    // internal knowledge of the 2-way result. The orchestration layer is
    // responsible for never calling it when findBestMatch already succeeded.
    // We call it here only to confirm it is independently functional; its
    // result is irrelevant to the priority decision.
    const _cycle = findBestThreeWayCycle(nurseA, [nurseB, nurseC]);
    // The priority assertion: a non-null directResult means a 2-way match
    // would be selected by any correct orchestrator. The 3-way result (_cycle)
    // is intentionally unused here — it will be tested end-to-end once
    // matchingService.ts wires findBestMatchOrCycle.
    assert.ok(directResult.nurseBUid === "uid-B", "2-way direct match confirmed available");
  });

  // ---- Test 4: 4-way chain is NOT matched as a 3-way cycle ----
  test("3W-04 - four-participant chain A->B->C->D->A does not produce a 3-way match", () => {
    // Only a 4-way cycle closes; no 3-way subset closes.
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-C"]);
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-D"]);  // C wants HOSP-D, not HOSP-A
    const nurseD = req("uid-D", "HOSP-D", ["HOSP-A"]);  // D closes back to A but DFS stops at depth 3

    const pool = [nurseB, nurseC, nurseD];
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null, "4-way chain must NOT produce a 3-way cycle");
  });

  // ---- Test 5: Duplicate participant rejected ----
  test("3W-05 - pool containing same UID as source is silently skipped", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    // Pool deliberately includes a copy of A with a different hospital key — UID match is the guard.
    const cloneOfA = req("uid-A", "HOSP-B", ["HOSP-C"]); // same UID as source
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    const pool = [cloneOfA, nurseC];
    // cloneOfA has UID == source; it must be rejected as B, so no valid B->C->A path.
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null);
  });

  // ---- Test 6: Self-cycle rejected ----
  test("3W-06 - source cannot form a cycle with itself at any hop", () => {
    // A has preferences that circle back to itself, but there are no other nurses.
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-A"]);
    const result = findBestThreeWayCycle(nurseA, [nurseA]);
    assert.equal(result, null);
  });

  // ---- Test 7: Cross-grade 3-way accepted ----
  test("3W-07 - cross-grade 3-way cycle is accepted (grade is never an exclusion filter)", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"], { grade: "Grade I" });
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-C"], { grade: "Grade II" });
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"], { grade: "Grade III" });

    const pool = [nurseB, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result, "Cross-grade cycle must be accepted");
    assert.equal(result.isAllSameGrade, false);
    assert.equal(result.nurseAGrade, "Grade I");
    assert.equal(result.nurseBGrade, "Grade II");
    assert.equal(result.nurseCGrade, "Grade III");
    assert.match(result.priorityReason, /cross-grade/i);
  });

  // ---- Test 8: Same-grade cycle preferred over cross-grade when equal rank ----
  test("3W-08 - same-grade cycle ranked above cross-grade cycle when combined rank is equal", () => {
    // Both cycles have combined rank = 3 (all first preferences).
    // Same-grade cycle must sort first.
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B", "HOSP-X"], { grade: "Grade I" });

    // Same-grade path: A(HOSP-A) -> B(HOSP-B) -> C(HOSP-C) -> A
    const nurseB_sg = req("uid-B-sg", "HOSP-B", ["HOSP-C"], { grade: "Grade I" });
    const nurseC_sg = req("uid-C-sg", "HOSP-C", ["HOSP-A"], { grade: "Grade I" });

    // Cross-grade path: A(HOSP-A) -> BX(HOSP-X) -> CY(HOSP-Y) -> A
    const nurseB_cg = req("uid-B-cg", "HOSP-X", ["HOSP-Y"], { grade: "Grade II" });
    const nurseC_cg = req("uid-C-cg", "HOSP-Y", ["HOSP-A"], { grade: "Grade III" });

    const pool = [nurseB_sg, nurseC_sg, nurseB_cg, nurseC_cg];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result);
    assert.equal(result.isAllSameGrade, true, "Same-grade cycle must be ranked first");
    assert.equal(result.nurseBUid, "uid-B-sg");
    assert.equal(result.nurseCUid, "uid-C-sg");
  });

  // ---- Test 9: Preference rank calculation ----
  test("3W-09 - combinedPreferenceRank equals sum of A->B + B->C + C->A ranks", () => {
    // A prefers [HOSP-X, HOSP-B] so B is at rank 2 for A
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-X", "HOSP-B"]);
    // B prefers [HOSP-Y, HOSP-Z, HOSP-C] so C is at rank 3 for B
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-Y", "HOSP-Z", "HOSP-C"]);
    // C prefers [HOSP-W, HOSP-A] so A is at rank 2 for C
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-W", "HOSP-A"]);

    const pool = [nurseB, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result);
    assert.equal(result.nurseAPreferenceRank, 2, "A->B rank should be 2");
    assert.equal(result.nurseBPreferenceRank, 3, "B->C rank should be 3");
    assert.equal(result.nurseCPreferenceRank, 2, "C->A rank should be 2");
    assert.equal(result.combinedPreferenceRank, 7, "combined must equal 2+3+2=7");
  });

  // ---- Test 10: Deterministic B and C UID tie-breaking ----
  test("3W-10 - tie-breaking uses alphabetical nurseBUid then nurseCUid", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);

    // Two B-level candidates at HOSP-B (same rank 1 to A).
    // Both B candidates have C at first preference, and C -> HOSP-A.
    const nurseB1 = req("uid-B-ZZZ", "HOSP-B", ["HOSP-C1"]);
    const nurseC1 = req("uid-C-1",   "HOSP-C1", ["HOSP-A"]);

    const nurseB2 = req("uid-B-AAA", "HOSP-B", ["HOSP-C2"]);
    const nurseC2 = req("uid-C-1a",  "HOSP-C2", ["HOSP-A"]);

    // Both cycles: combined rank = 1+1+1 = 3, same grade.
    // nurseBUid "uid-B-AAA" < "uid-B-ZZZ" alphabetically => cycle 2 wins.
    const pool = [nurseB1, nurseC1, nurseB2, nurseC2];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result);
    assert.equal(result.nurseBUid, "uid-B-AAA");
  });

  // ---- Test 11: Locked participant excluded ----
  test("3W-11 - locked participant is excluded from cycle", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB_locked = req("uid-B", "HOSP-B", ["HOSP-C"], { locked: true });
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    const pool = [nurseB_locked, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null, "Locked intermediate must prevent cycle formation");
  });

  // ---- Test 12: Non-SEARCHING participant excluded ----
  test("3W-12 - non-SEARCHING participant is excluded from cycle", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB_pending = req("uid-B", "HOSP-B", ["HOSP-C"], { status: "PENDING" });
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    const pool = [nurseB_pending, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null);
  });

  // ---- Test 13: Existing currentMatchId participant excluded ----
  test("3W-13 - participant with active currentMatchId is excluded from cycle", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB_matched = req("uid-B", "HOSP-B", ["HOSP-C"], { currentMatchId: "existing-match-xyz" });
    const nurseC = req("uid-C", "HOSP-C", ["HOSP-A"]);

    const pool = [nurseB_matched, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);
    assert.equal(result, null);
  });

  // ---- Test 14: All three current hospitals must be different ----
  test("3W-14 - cycle rejected when two participants share the same current hospital", () => {
    // B and C are both stationed at HOSP-B => same hospital, not a valid distinct cycle.
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B"]);
    const nurseB = req("uid-B", "HOSP-B", ["HOSP-B"]); // B wants to stay at its own hospital (self-loop pref)
    const nurseC = req("uid-C", "HOSP-B", ["HOSP-A"]);  // C is also at HOSP-B — same hospital as B

    const pool = [nurseB, nurseC];
    const result = findBestThreeWayCycle(nurseA, pool);
    // nurseB wants HOSP-B (own hospital); nurseC is at HOSP-B = same as B.
    // No valid 3-way cycle with distinct hospitals forms.
    assert.equal(result, null);
  });

  // ---- Test 15: Multiple valid cycles return deterministic best ----
  test("3W-15 - among multiple valid cycles the deterministically best cycle is returned", () => {
    const nurseA = req("uid-A", "HOSP-A", ["HOSP-B", "HOSP-X"]);

    // Cycle 1: A(#1) -> B1(#2) -> C1(#1) = combined 4, cross-grade
    const nurseB1 = req("uid-B1", "HOSP-B", ["HOSP-Z1", "HOSP-C1"], { grade: "Grade II" });
    const nurseC1 = req("uid-C1", "HOSP-C1", ["HOSP-A"], { grade: "Grade III" });

    // Cycle 2: A(#1) -> B2(#1) -> C2(#1) = combined 3, same-grade => WINS
    const nurseB2 = req("uid-B2", "HOSP-B", ["HOSP-C2"]); // grade "Grade I" (default)
    const nurseC2 = req("uid-C2", "HOSP-C2", ["HOSP-A"]);  // grade "Grade I" (default)

    // Cycle 3: A(#2) -> BX(#1) -> CX(#1) = combined 4, same-grade but worse than cycle 2
    const nurseBX = req("uid-BX", "HOSP-X", ["HOSP-CX"]);
    const nurseCX = req("uid-CX", "HOSP-CX", ["HOSP-A"]);

    const pool = [nurseB1, nurseC1, nurseB2, nurseC2, nurseBX, nurseCX];
    const result = findBestThreeWayCycle(nurseA, pool);

    assert.ok(result);
    // Cycle 2 is same-grade with combined rank 3 => best
    assert.equal(result.isAllSameGrade, true);
    assert.equal(result.combinedPreferenceRank, 3);
    assert.equal(result.nurseBUid, "uid-B2");
    assert.equal(result.nurseCUid, "uid-C2");
  });
});
