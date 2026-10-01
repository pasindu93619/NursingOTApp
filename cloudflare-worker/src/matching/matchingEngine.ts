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
