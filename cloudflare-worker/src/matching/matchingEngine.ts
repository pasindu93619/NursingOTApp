/**
 * Pure Deterministic 2-Way Mutual Transfer Matching Engine.
 *
 * Scope: Pure domain module with zero external dependencies, zero I/O,
 * zero Firestore access, and zero HTTP networking.
 *
 * Business Rules:
 * 1. Direct 2-way matching only (A -> B and B -> A).
 * 2. Same-grade compatible candidates are prioritized/preferred.
 * 3. Different-grade candidates remain eligible if no same-grade compatible partner exists.
 * 4. Grade is NEVER an exclusionary filter (no hard grade gate).
 * 5. Deterministic ranking: Same-grade priority -> Lower combined preference rank -> Deterministic UID tie-breaker.
 * 6. Expiration deadlines (48h/72h) are deliberately NOT handled here; deadlines are managed by the server transaction layer.
 */

export interface CandidateRequest {
  firebaseUid: string;
  status: string;
  locked: boolean;
  currentMatchId?: string | null;
  currentHospitalId: string;
  preferenceHospitalIds: string[];
  grade: string;
  updateTime?: string;
}

export interface DirectMatch {
  nurseAUid: string;
  nurseBUid: string;
  nurseACurrentHospitalId: string;
  nurseBCurrentHospitalId: string;
  nurseADestinationHospitalId: string;
  nurseBDestinationHospitalId: string;
  nurseAGrade: string;
  nurseBGrade: string;
  isSameGrade: boolean;
  nurseAPreferenceRank: number; // 1-based (1 = 1st preference, 2 = 2nd, 3 = 3rd)
  nurseBPreferenceRank: number; // 1-based (1 = 1st preference, 2 = 2nd, 3 = 3rd)
  combinedPreferenceRank: number; // nurseAPreferenceRank + nurseBPreferenceRank
  priorityReason: string;
}

export interface MatchEvaluationResult {
  isCompatible: boolean;
  ineligibilityReason?: string;
  match?: DirectMatch;
}

/**
 * Validates basic field requirements of a transfer request.
 */
function validateCandidateRequest(req: CandidateRequest): string | null {
  if (!req.firebaseUid || req.firebaseUid.trim().length === 0) {
    return "Missing or blank firebaseUid";
  }
  if (!req.grade || req.grade.trim().length === 0) {
    return "Missing or blank grade";
  }
  if (!req.currentHospitalId || req.currentHospitalId.trim().length === 0) {
    return "Missing or blank currentHospitalId";
  }
  if (!Array.isArray(req.preferenceHospitalIds) || req.preferenceHospitalIds.length === 0) {
    return "Preferences list cannot be empty";
  }
  if (req.preferenceHospitalIds.some(id => !id || id.trim().length === 0)) {
    return "Preferences list contains empty or blank hospital ID";
  }
  return null;
}

/**
 * Evaluates whether two transfer requests form a compatible direct 2-way match.
 *
 * Invariants:
 * - A and B must be distinct nurses (A.firebaseUid != B.firebaseUid).
 * - A and B must both have status == 'SEARCHING'.
 * - Neither request may be locked (locked == false).
 * - Neither request may have an active match (currentMatchId == null/empty).
 * - A and B must currently be posted at different hospitals (A.currentHospitalId != B.currentHospitalId).
 * - B's current hospital must be in A's ranked preferences.
 * - A's current hospital must be in B's ranked preferences.
 * - Both must have valid, non-blank grades.
 *
 * Grade equality is NOT required for compatibility.
 */
export function evaluateDirectPair(
  source: CandidateRequest,
  candidate: CandidateRequest
): MatchEvaluationResult {
  const sourceValidation = validateCandidateRequest(source);
  if (sourceValidation) {
    return { isCompatible: false, ineligibilityReason: `Source invalid: ${sourceValidation}` };
  }

  const candidateValidation = validateCandidateRequest(candidate);
  if (candidateValidation) {
    return { isCompatible: false, ineligibilityReason: `Candidate invalid: ${candidateValidation}` };
  }

  const sourceUid = source.firebaseUid.trim();
  const candidateUid = candidate.firebaseUid.trim();

  // 1. Self-match rejection
  if (sourceUid === candidateUid) {
    return { isCompatible: false, ineligibilityReason: "Self-match is rejected" };
  }

  // 2. Active status requirement
  if (source.status.trim().toUpperCase() !== "SEARCHING") {
    return { isCompatible: false, ineligibilityReason: `Source request status is '${source.status}', expected 'SEARCHING'` };
  }
  if (candidate.status.trim().toUpperCase() !== "SEARCHING") {
    return { isCompatible: false, ineligibilityReason: `Candidate request status is '${candidate.status}', expected 'SEARCHING'` };
  }

  // 3. Lock check
  if (source.locked) {
    return { isCompatible: false, ineligibilityReason: "Source request is already locked" };
  }
  if (candidate.locked) {
    return { isCompatible: false, ineligibilityReason: "Candidate request is already locked" };
  }

  // 4. Current match ID check
  if (source.currentMatchId && source.currentMatchId.trim().length > 0) {
    return { isCompatible: false, ineligibilityReason: "Source request already has an active matchId" };
  }
  if (candidate.currentMatchId && candidate.currentMatchId.trim().length > 0) {
    return { isCompatible: false, ineligibilityReason: "Candidate request already has an active matchId" };
  }

  // 5. Same hospital rejection
  const sourceHospital = source.currentHospitalId.trim();
  const candidateHospital = candidate.currentHospitalId.trim();
  if (sourceHospital === candidateHospital) {
    return { isCompatible: false, ineligibilityReason: "Both nurses are currently posted at the same hospital" };
  }

  // 6. Mutual preference check
  // Clean preferences (first occurrence defines rank)
  const sourcePrefs = source.preferenceHospitalIds.map(h => h.trim());
  const candidatePrefs = candidate.preferenceHospitalIds.map(h => h.trim());

  const sourcePrefIndex = sourcePrefs.indexOf(candidateHospital);
  if (sourcePrefIndex === -1) {
    return { isCompatible: false, ineligibilityReason: "Source nurse did not select candidate's current hospital" };
  }

  const candidatePrefIndex = candidatePrefs.indexOf(sourceHospital);
  if (candidatePrefIndex === -1) {
    return { isCompatible: false, ineligibilityReason: "Candidate nurse did not select source's current hospital" };
  }

  const rankA = sourcePrefIndex + 1; // 1-based
  const rankB = candidatePrefIndex + 1; // 1-based
  const combinedRank = rankA + rankB;

  const sourceGrade = source.grade.trim();
  const candidateGrade = candidate.grade.trim();
  const isSameGrade = sourceGrade.toLowerCase() === candidateGrade.toLowerCase();

  const priorityReason = isSameGrade
    ? `Same-grade match (${sourceGrade}), combined preference rank ${combinedRank} (A: #${rankA}, B: #${rankB})`
    : `Cross-grade match (${sourceGrade} <-> ${candidateGrade}), combined preference rank ${combinedRank} (A: #${rankA}, B: #${rankB})`;

  const match: DirectMatch = {
    nurseAUid: sourceUid,
    nurseBUid: candidateUid,
    nurseACurrentHospitalId: sourceHospital,
    nurseBCurrentHospitalId: candidateHospital,
    nurseADestinationHospitalId: candidateHospital,
    nurseBDestinationHospitalId: sourceHospital,
    nurseAGrade: sourceGrade,
    nurseBGrade: candidateGrade,
    isSameGrade,
    nurseAPreferenceRank: rankA,
    nurseBPreferenceRank: rankB,
    combinedPreferenceRank: combinedRank,
    priorityReason
  };

  return {
    isCompatible: true,
    match
  };
}

/**
 * Deterministic match comparator.
 *
 * Sort order:
 * 1. Same-grade candidates first (`isSameGrade: true` before `false`).
 * 2. Lower combined preference rank first (`combinedPreferenceRank` ascending).
 * 3. Lower source nurse rank first (`nurseAPreferenceRank` ascending).
 * 4. Deterministic candidate UID tie-breaker (`nurseBUid` alphabetical ascending).
 */
export function compareMatches(a: DirectMatch, b: DirectMatch): number {
  // 1. Same-grade priority
  if (a.isSameGrade !== b.isSameGrade) {
    return a.isSameGrade ? -1 : 1;
  }

  // 2. Combined preference rank (lower is better, e.g. 1+1=2 vs 1+2=3)
  if (a.combinedPreferenceRank !== b.combinedPreferenceRank) {
    return a.combinedPreferenceRank - b.combinedPreferenceRank;
  }

  // 3. Nurse A's individual preference rank
  if (a.nurseAPreferenceRank !== b.nurseAPreferenceRank) {
    return a.nurseAPreferenceRank - b.nurseAPreferenceRank;
  }

  // 4. Deterministic UID tie-breaker
  return a.nurseBUid.localeCompare(b.nurseBUid);
}

/**
 * Sorts an array of direct matches deterministically.
 */
export function rankMatches(matches: DirectMatch[]): DirectMatch[] {
  return [...matches].sort(compareMatches);
}

/**
 * Finds all compatible direct 2-way candidates from a pool, ranked deterministically.
 */
export function findAllCompatibleMatches(
  source: CandidateRequest,
  pool: CandidateRequest[]
): DirectMatch[] {
  const compatibleMatches: DirectMatch[] = [];

  for (const candidate of pool) {
    const evaluation = evaluateDirectPair(source, candidate);
    if (evaluation.isCompatible && evaluation.match) {
      compatibleMatches.push(evaluation.match);
    }
  }

  return rankMatches(compatibleMatches);
}

/**
 * Evaluates candidate pool and returns the single highest-priority compatible match.
 * Returns null if no compatible candidate exists in the pool.
 */
export function findBestMatch(
  source: CandidateRequest,
  pool: CandidateRequest[]
): DirectMatch | null {
  const ranked = findAllCompatibleMatches(source, pool);
  return ranked.length > 0 ? ranked[0] : null;
}

// =============================================================================
// 3-Way Circular Matching Engine
// =============================================================================
// Pure deterministic domain module with zero external dependencies and zero I/O.
//
// Business Rules:
// 1. Direct 2-way matching has ABSOLUTE priority; findBestThreeWayCycle is only
//    invoked when findBestMatch(source, pool) returns null.
// 2. A valid 3-way cycle is: A -> B -> C -> A.
// 3. DFS depth is hard-capped at 3 nurses; no 4-way cycles are evaluated.
// 4. Same-grade cycles are prioritized; cross-grade cycles remain eligible.
// 5. Grade is NEVER an exclusion filter.
// 6. All eligibility checks from the 2-way engine apply to each participant.
// =============================================================================

/**
 * Represents a valid 3-way circular mutual transfer cycle A -> B -> C -> A.
 *
 * Naming follows the existing DirectMatch convention where "nurseA" is the
 * request initiator and nurses B and C are the subsequent cycle participants.
 */
export interface ThreeWayMatch {
  nurseAUid: string;
  nurseBUid: string;
  nurseCUid: string;

  nurseACurrentHospitalId: string;
  nurseBCurrentHospitalId: string;
  nurseCCurrentHospitalId: string;

  /** A travels to B's current hospital */
  nurseADestinationHospitalId: string;
  /** B travels to C's current hospital */
  nurseBDestinationHospitalId: string;
  /** C travels to A's current hospital */
  nurseCDestinationHospitalId: string;

  nurseAGrade: string;
  nurseBGrade: string;
  nurseCGrade: string;

  /** true iff all three grades are equal (case-insensitive) */
  isAllSameGrade: boolean;

  /** 1-based rank of A's preference for B's hospital */
  nurseAPreferenceRank: number;
  /** 1-based rank of B's preference for C's hospital */
  nurseBPreferenceRank: number;
  /** 1-based rank of C's preference for A's hospital */
  nurseCPreferenceRank: number;

  /** nurseAPreferenceRank + nurseBPreferenceRank + nurseCPreferenceRank */
  combinedPreferenceRank: number;

  priorityReason: string;
}

/**
 * Determines whether a candidate is eligible to participate in a cycle hop.
 *
 * Mirrors the individual-request checks from evaluateDirectPair without
 * requiring a second candidate (since the cross-check between two participants
 * is handled at the DFS path level).
 *
 * Returns null when eligible; returns a rejection reason string otherwise.
 */
function isCandidateEligibleForCycle(candidate: CandidateRequest): string | null {
  const validationError = validateCandidateRequest(candidate);
  if (validationError) return validationError;

  if (candidate.status.trim().toUpperCase() !== "SEARCHING") {
    return `Candidate request status is '${candidate.status}', expected 'SEARCHING'`;
  }
  if (candidate.locked) {
    return "Candidate request is already locked";
  }
  if (candidate.currentMatchId && candidate.currentMatchId.trim().length > 0) {
    return "Candidate request already has an active matchId";
  }
  return null;
}

/**
 * Deterministic comparator for ThreeWayMatch, mirroring compareMatches semantics.
 *
 * Sort order:
 * 1. All-three same grade first (`isAllSameGrade: true` before `false`).
 * 2. Lower combined preference rank ascending.
 * 3. Lower A->B preference rank ascending.
 * 4. Alphabetical nurseBUid tie-breaker.
 * 5. Alphabetical nurseCUid tie-breaker.
 */
export function compareThreeWayMatches(a: ThreeWayMatch, b: ThreeWayMatch): number {
  // 1. All-same-grade priority
  if (a.isAllSameGrade !== b.isAllSameGrade) {
    return a.isAllSameGrade ? -1 : 1;
  }

  // 2. Combined preference rank (lower is better)
  if (a.combinedPreferenceRank !== b.combinedPreferenceRank) {
    return a.combinedPreferenceRank - b.combinedPreferenceRank;
  }

  // 3. A->B individual rank
  if (a.nurseAPreferenceRank !== b.nurseAPreferenceRank) {
    return a.nurseAPreferenceRank - b.nurseAPreferenceRank;
  }

  // 4. Deterministic nurseBUid tie-breaker
  const bCmp = a.nurseBUid.localeCompare(b.nurseBUid);
  if (bCmp !== 0) return bCmp;

  // 5. Deterministic nurseCUid tie-breaker
  return a.nurseCUid.localeCompare(b.nurseCUid);
}

/**
 * Sorts an array of 3-way matches deterministically.
 */
export function rankThreeWayMatches(matches: ThreeWayMatch[]): ThreeWayMatch[] {
  return [...matches].sort(compareThreeWayMatches);
}

/**
 * Finds the best valid 3-way cycle for the given source nurse in the pool.
 *
 * The search follows a strict DFS limited to depth 3 (A -> B -> C -> A).
 * It uses a hospital-to-candidates index for O(1) hop lookups.
 *
 * Returns null when no valid cycle exists.
 *
 * IMPORTANT: Callers MUST first call findBestMatch(source, pool). This
 * function should only be invoked when findBestMatch returns null, to
 * preserve the absolute 2-way priority business rule.
 */
export function findBestThreeWayCycle(
  source: CandidateRequest,
  pool: CandidateRequest[]
): ThreeWayMatch | null {
  // Validate source up-front (same checks applied to every participant)
  if (isCandidateEligibleForCycle(source) !== null) return null;

  const sourceUid = source.firebaseUid.trim();
  const sourceHospital = source.currentHospitalId.trim();
  const sourcePrefs = source.preferenceHospitalIds.map(h => h.trim());

  // Build a hospital -> eligible candidates index for O(1) hop lookups.
  // Only include candidates that pass basic eligibility (status, lock, matchId, grade, hospital).
  const hospitalIndex = new Map<string, CandidateRequest[]>();
  for (const candidate of pool) {
    if (isCandidateEligibleForCycle(candidate) !== null) continue;
    const hospId = candidate.currentHospitalId.trim();
    const existing = hospitalIndex.get(hospId);
    if (existing) {
      existing.push(candidate);
    } else {
      hospitalIndex.set(hospId, [candidate]);
    }
  }

  const validCycles: ThreeWayMatch[] = [];

  // --- Depth 1: A -> B ---
  // For each hospital that A wants, look up candidates stationed there.
  for (let rankAIdx = 0; rankAIdx < sourcePrefs.length; rankAIdx++) {
    const bHospital = sourcePrefs[rankAIdx];
    if (!bHospital) continue;

    const bCandidates = hospitalIndex.get(bHospital) ?? [];

    for (const candidateB of bCandidates) {
      const bUid = candidateB.firebaseUid.trim();
      const bHosp = candidateB.currentHospitalId.trim();

      // Reject source as B
      if (bUid === sourceUid) continue;
      // Reject same current hospital (already guaranteed by index, but explicit)
      if (bHosp === sourceHospital) continue;

      const bPrefs = candidateB.preferenceHospitalIds.map(h => h.trim());

      // --- Depth 2: B -> C ---
      for (let rankBIdx = 0; rankBIdx < bPrefs.length; rankBIdx++) {
        const cHospital = bPrefs[rankBIdx];
        if (!cHospital) continue;

        // C must be at a hospital different from both A's and B's
        if (cHospital === sourceHospital) continue;
        if (cHospital === bHosp) continue;

        const cCandidates = hospitalIndex.get(cHospital) ?? [];

        for (const candidateC of cCandidates) {
          const cUid = candidateC.firebaseUid.trim();
          const cHosp = candidateC.currentHospitalId.trim();

          // Reject duplicates
          if (cUid === sourceUid) continue;
          if (cUid === bUid) continue;
          // Reject same current hospitals
          if (cHosp === sourceHospital) continue;
          if (cHosp === bHosp) continue;

          // --- Depth 3 (Cycle Closure): C -> A ---
          // C must have A's current hospital in their preferences.
          const cPrefs = candidateC.preferenceHospitalIds.map(h => h.trim());
          const rankCIdx = cPrefs.indexOf(sourceHospital);
          if (rankCIdx === -1) continue; // Cycle does not close back to A

          // Valid cycle found: A -> B -> C -> A
          const rankA = rankAIdx + 1; // 1-based
          const rankB = rankBIdx + 1; // 1-based
          const rankC = rankCIdx + 1; // 1-based
          const combinedRank = rankA + rankB + rankC;

          const gradeA = source.grade.trim();
          const gradeB = candidateB.grade.trim();
          const gradeC = candidateC.grade.trim();
          const isAllSameGrade =
            gradeA.toLowerCase() === gradeB.toLowerCase() &&
            gradeA.toLowerCase() === gradeC.toLowerCase();

          const priorityReason = isAllSameGrade
            ? `All-same-grade 3-way cycle (${gradeA}), combined preference rank ${combinedRank} (A: #${rankA}, B: #${rankB}, C: #${rankC})`
            : `Cross-grade 3-way cycle (${gradeA} / ${gradeB} / ${gradeC}), combined preference rank ${combinedRank} (A: #${rankA}, B: #${rankB}, C: #${rankC})`;

          validCycles.push({
            nurseAUid: sourceUid,
            nurseBUid: bUid,
            nurseCUid: cUid,
            nurseACurrentHospitalId: sourceHospital,
            nurseBCurrentHospitalId: bHosp,
            nurseCCurrentHospitalId: cHosp,
            nurseADestinationHospitalId: bHosp,
            nurseBDestinationHospitalId: cHosp,
            nurseCDestinationHospitalId: sourceHospital,
            nurseAGrade: gradeA,
            nurseBGrade: gradeB,
            nurseCGrade: gradeC,
            isAllSameGrade,
            nurseAPreferenceRank: rankA,
            nurseBPreferenceRank: rankB,
            nurseCPreferenceRank: rankC,
            combinedPreferenceRank: combinedRank,
            priorityReason
          });
        }
      }
    }
  }

  if (validCycles.length === 0) return null;
  return rankThreeWayMatches(validCycles)[0];
}
