import test, { describe } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import {
  findAndLockMatch,
  sweepExpiredMatches,
  MatchServiceError,
  type FindAndLockMatchSuccess,
  type FindAndLockNoMatch
} from "../src/matching/matchingService.ts";
import type { ThreeWayMatch, DirectMatch } from "../src/matching/matchingEngine.ts";
import {
  FirestoreClient,
  FirestoreError,
  type HttpTransport,
  type TokenProvider,
  type FirestoreRawDocument,
  type FirestoreRunQueryItem,
  type FirestoreCommitResponse,
  type FirestoreWrite
} from "../src/firestore/firestoreClient.ts";
import type { Env, GoogleJwk } from "../src/types.ts";

const TEST_PROJECT_ID = "nursing-super-app-test";
const MOCK_TOKEN = "mock-service-account-token";
const mockTokenProvider: TokenProvider = async () => MOCK_TOKEN;

function stringToBase64Url(str: string): string {
  return Buffer.from(str, "utf8").toString("base64url");
}

function uint8ArrayToBase64Url(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString("base64url");
}

async function createSignedTestJwt(
  header: Record<string, unknown>,
  payload: Record<string, unknown>,
  privateKey: CryptoKey
): Promise<string> {
  const headerB64 = stringToBase64Url(JSON.stringify(header));
  const payloadB64 = stringToBase64Url(JSON.stringify(payload));
  const dataToSign = new TextEncoder().encode(`${headerB64}.${payloadB64}`);

  const signatureBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    dataToSign
  );

  const signatureB64 = uint8ArrayToBase64Url(new Uint8Array(signatureBuffer));
  return `${headerB64}.${payloadB64}.${signatureB64}`;
}

function createMockTransport(
  routes: Record<string, (req: Request) => Promise<Response> | Response>
): HttpTransport {
  return async (input, init) => {
    const req = input instanceof Request ? input : new Request(input, init);
    const url = new URL(req.url);
    const key = `${req.method} ${url.pathname}`;
    const handler = routes[key] || routes[url.pathname];

    if (!handler) {
      throw new Error(`Unexpected outgoing HTTP request in test: ${req.method} ${req.url}`);
    }

    return handler(req);
  };
}

function createMockRawDoc(
  uid: string,
  hospital: string,
  prefs: string[],
  grade: string,
  options: {
    status?: string;
    locked?: boolean;
    currentMatchId?: string | null;
    updateTime?: string;
  } = {}
): FirestoreRawDocument {
  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/${uid}`,
    updateTime: options.updateTime ?? "2026-10-02T10:00:00.000Z",
    fields: {
      firebaseUid: { stringValue: uid },
      status: { stringValue: options.status ?? "SEARCHING" },
      locked: { booleanValue: options.locked ?? false },
      currentMatchId: options.currentMatchId ? { stringValue: options.currentMatchId } : { nullValue: null },
      currentHospitalId: { stringValue: hospital },
      preferenceHospitalIds: {
        arrayValue: {
          values: prefs.map(p => ({ stringValue: p }))
        }
      },
      grade: { stringValue: grade }
    }
  };
}

function createMock2WayMatchDoc(
  matchId: string,
  nurseAUid: string,
  nurseBUid: string,
  nurseACurrentHospitalId = "HOSP-001",
  nurseBCurrentHospitalId = "HOSP-002",
  nurseADestinationHospitalId = "HOSP-002",
  nurseBDestinationHospitalId = "HOSP-001",
  options: {
    status?: string;
    expiresAt?: string;
    updateTime?: string;
  } = {}
): FirestoreRawDocument {
  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/${matchId}`,
    updateTime: options.updateTime ?? "2026-10-02T10:00:00.000Z",
    fields: {
      nurseAUid: { stringValue: nurseAUid },
      nurseBUid: { stringValue: nurseBUid },
      nurseACurrentHospitalId: { stringValue: nurseACurrentHospitalId },
      nurseBCurrentHospitalId: { stringValue: nurseBCurrentHospitalId },
      nurseADestinationHospitalId: { stringValue: nurseADestinationHospitalId },
      nurseBDestinationHospitalId: { stringValue: nurseBDestinationHospitalId },
      nurseAGrade: { stringValue: "Grade I" },
      nurseBGrade: { stringValue: "Grade I" },
      isSameGrade: { booleanValue: true },
      nurseAPreferenceRank: { integerValue: "1" },
      nurseBPreferenceRank: { integerValue: "1" },
      combinedPreferenceRank: { integerValue: "2" },
      priorityReason: { stringValue: "Direct 2-way match" },
      status: { stringValue: options.status ?? "PENDING_CONFIRMATION" },
      createdAt: { stringValue: "2026-10-02T10:00:00.000Z" },
      expiresAt: { stringValue: options.expiresAt ?? new Date(Date.now() + 48 * 3600 * 1000).toISOString() },
      updatedAt: { stringValue: options.updateTime ?? "2026-10-02T10:00:00.000Z" }
    }
  };
}

function createMock3WayMatchDoc(
  matchId: string,
  nurseAUid: string,
  nurseBUid: string,
  nurseCUid: string,
  nurseACurrentHospitalId = "HOSP-001",
  nurseBCurrentHospitalId = "HOSP-002",
  nurseCCurrentHospitalId = "HOSP-003",
  nurseADestinationHospitalId = "HOSP-002",
  nurseBDestinationHospitalId = "HOSP-003",
  nurseCDestinationHospitalId = "HOSP-001",
  options: {
    status?: string;
    expiresAt?: string;
    updateTime?: string;
  } = {}
): FirestoreRawDocument {
  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/${matchId}`,
    updateTime: options.updateTime ?? "2026-10-02T10:00:00.000Z",
    fields: {
      nurseAUid: { stringValue: nurseAUid },
      nurseBUid: { stringValue: nurseBUid },
      nurseCUid: { stringValue: nurseCUid },
      nurseACurrentHospitalId: { stringValue: nurseACurrentHospitalId },
      nurseBCurrentHospitalId: { stringValue: nurseBCurrentHospitalId },
      nurseCCurrentHospitalId: { stringValue: nurseCCurrentHospitalId },
      nurseADestinationHospitalId: { stringValue: nurseADestinationHospitalId },
      nurseBDestinationHospitalId: { stringValue: nurseBDestinationHospitalId },
      nurseCDestinationHospitalId: { stringValue: nurseCDestinationHospitalId },
      nurseAGrade: { stringValue: "Grade I" },
      nurseBGrade: { stringValue: "Grade I" },
      nurseCGrade: { stringValue: "Grade I" },
      isAllSameGrade: { booleanValue: true },
      nurseAPreferenceRank: { integerValue: "1" },
      nurseBPreferenceRank: { integerValue: "1" },
      nurseCPreferenceRank: { integerValue: "1" },
      combinedPreferenceRank: { integerValue: "3" },
      priorityReason: { stringValue: "All-same-grade 3-way cycle" },
      status: { stringValue: options.status ?? "PENDING_CONFIRMATION" },
      createdAt: { stringValue: "2026-10-02T10:00:00.000Z" },
      expiresAt: { stringValue: options.expiresAt ?? new Date(Date.now() + 48 * 3600 * 1000).toISOString() },
      updatedAt: { stringValue: options.updateTime ?? "2026-10-02T10:00:00.000Z" }
    }
  };
}

describe("Server-Side Matching & Concurrency-Safe Locking (C4 Step 3)", () => {
  // 1. Caller not found in Firestore
  test("1 - findAndLockMatch throws CALLER_NOT_FOUND if caller has no transfer request", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify({ error: "Document not found" }), { status: 404 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CALLER_NOT_FOUND");
        assert.equal(err.statusCode, 404);
        return true;
      }
    );
  });

  // 2. Caller request is locked
  test("2 - findAndLockMatch throws CALLER_LOCKED if caller transfer request is already locked", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      locked: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CALLER_LOCKED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 3. Caller request already has active matchId
  test("3 - findAndLockMatch throws CALLER_ALREADY_MATCHED if caller has currentMatchId", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      currentMatchId: "match-existing-999"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CALLER_ALREADY_MATCHED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 4. Caller request status is not SEARCHING
  test("4 - findAndLockMatch throws INVALID_STATUS if caller status is not SEARCHING", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      status: "WITHDRAWN"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "INVALID_STATUS");
        assert.equal(err.statusCode, 400);
        return true;
      }
    );
  });

  // 5. Caller request has missing required fields
  test("5 - findAndLockMatch throws INVALID_REQUEST if caller has missing grade or preferences", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", [], "");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "INVALID_REQUEST");
        assert.equal(err.statusCode, 400);
        return true;
      }
    );
  });

  // 6. No compatible candidate in pool
  test("6 - findAndLockMatch returns matched: false when no compatible candidate exists", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    // Candidate wants HOSP-999, not HOSP-001
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-999"], "Grade I");

    const queryItems: FirestoreRunQueryItem[] = [{ document: candidateDoc }];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, false);
    assert.equal((result as FindAndLockNoMatch).message, "No compatible match found");
  });

  // 7. Self-match candidate is ignored
  test("7 - findAndLockMatch ignores caller self-request in candidate pool", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    const queryItems: FirestoreRunQueryItem[] = [{ document: callerDoc }];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, false);
  });

  // 8. Candidates with locked=true or active matchId are excluded
  test("8 - findAndLockMatch excludes candidates who are locked or already matched", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    const lockedCandidate = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I", {
      locked: true
    });
    const matchedCandidate = createMockRawDoc("nurse-c", "HOSP-002", ["HOSP-001"], "Grade I", {
      currentMatchId: "match-456"
    });

    const queryItems: FirestoreRunQueryItem[] = [
      { document: lockedCandidate },
      { document: matchedCandidate }
    ];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, false);
  });

  // 9. Same-grade prioritized over cross-grade candidate
  test("9 - findAndLockMatch prioritizes same-grade candidate over cross-grade candidate", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    const crossGradeCandidate = createMockRawDoc("nurse-cross", "HOSP-002", ["HOSP-001"], "Grade II", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    const sameGradeCandidate = createMockRawDoc("nurse-same", "HOSP-002", ["HOSP-001"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });

    const queryItems: FirestoreRunQueryItem[] = [
      { document: crossGradeCandidate },
      { document: sameGradeCandidate }
    ];

    let capturedWrites: FirestoreWrite[] = [];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:05:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      expirationHours: 48,
      generateMatchId: () => "match-priority-test"
    });

    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.match.nurseBUid, "nurse-same");
    assert.equal(success.match.isSameGrade, true);
    assert.equal(capturedWrites.length, 3);
  });

  // 10. Cross-grade candidate matched when no same-grade candidate exists
  test("10 - findAndLockMatch accepts cross-grade candidate when no same-grade candidate exists", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    const crossGradeCandidate = createMockRawDoc("nurse-cross", "HOSP-002", ["HOSP-001"], "Grade II", {
      updateTime: "2026-10-02T10:00:00Z"
    });

    const queryItems: FirestoreRunQueryItem[] = [{ document: crossGradeCandidate }];
    let capturedWrites: FirestoreWrite[] = [];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:05:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      expirationHours: 48,
      generateMatchId: () => "match-cross-test"
    });

    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.match.nurseBUid, "nurse-cross");
    assert.equal(success.match.isSameGrade, false);
    assert.equal(capturedWrites.length, 3);
  });

  // 11. Atomic locking payload structure and preconditions
  test("11 - atomic locking creates accurate 3-write payload with updateTime preconditions", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00.123Z"
    });
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I", {
      updateTime: "2026-10-02T10:00:00.456Z"
    });

    const queryItems: FirestoreRunQueryItem[] = [{ document: candidateDoc }];
    let capturedWrites: FirestoreWrite[] = [];

    const fixedNow = new Date("2026-10-02T12:00:00.000Z");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:01.000Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      now: () => fixedNow,
      expirationHours: 48,
      generateMatchId: () => "match-atomic-123"
    });

    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.matchId, "match-atomic-123");
    assert.equal(success.createdAt, "2026-10-02T12:00:00.000Z");
    assert.equal(success.expiresAt, "2026-10-04T12:00:00.000Z"); // +48 hours

    assert.equal(capturedWrites.length, 3);

    // Write 1: Caller lock
    const write1 = capturedWrites[0];
    assert.ok(write1.update);
    assert.ok(write1.update.name.endsWith("/transferRequests/nurse-a"));
    assert.deepEqual(write1.update.fields.locked, { booleanValue: true });
    assert.deepEqual(write1.update.fields.currentMatchId, { stringValue: "match-atomic-123" });
    assert.deepEqual(write1.update.fields.status, { stringValue: "MATCHED" });
    assert.deepEqual(write1.update.fields.updatedAt, { integerValue: fixedNow.getTime().toString() });
    assert.deepEqual(write1.currentDocument, {
      updateTime: "2026-10-02T10:00:00.123Z"
    });

    // Write 2: Candidate lock
    const write2 = capturedWrites[1];
    assert.ok(write2.update);
    assert.ok(write2.update.name.endsWith("/transferRequests/nurse-b"));
    assert.deepEqual(write2.update.fields.locked, { booleanValue: true });
    assert.deepEqual(write2.update.fields.currentMatchId, { stringValue: "match-atomic-123" });
    assert.deepEqual(write2.update.fields.status, { stringValue: "MATCHED" });
    assert.deepEqual(write2.currentDocument, {
      updateTime: "2026-10-02T10:00:00.456Z"
    });

    // Write 3: Match creation
    const write3 = capturedWrites[2];
    assert.ok(write3.update);
    assert.ok(write3.update.name.endsWith("/matches/match-atomic-123"));
    assert.deepEqual(write3.update.fields.nurseAUid, { stringValue: "nurse-a" });
    assert.deepEqual(write3.update.fields.nurseBUid, { stringValue: "nurse-b" });
    assert.deepEqual(write3.update.fields.status, { stringValue: "PENDING_CONFIRMATION" });
    assert.deepEqual(write3.update.fields.acceptedByA, { booleanValue: false });
    assert.deepEqual(write3.update.fields.acceptedByB, { booleanValue: false });
    assert.deepEqual(write3.update.fields.rejectedByA, { booleanValue: false });
    assert.deepEqual(write3.update.fields.rejectedByB, { booleanValue: false });
    assert.deepEqual(write3.update.fields.createdAt, { stringValue: "2026-10-02T12:00:00.000Z" });
    assert.deepEqual(write3.update.fields.expiresAt, { stringValue: "2026-10-04T12:00:00.000Z" });
    assert.deepEqual(write3.currentDocument, { exists: false });
  });

  // 12. If another worker wins the race, recover the already-created match.
  test("12 - commit conflict recovers an existing MATCHED caller and referenced match", async () => {
    const searchingCallerDoc = createMockRawDoc(
      "nurse-a",
      "HOSP-001",
      ["HOSP-002"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:00Z" }
    );
    const matchedCallerDoc = createMockRawDoc(
      "nurse-a",
      "HOSP-001",
      ["HOSP-002"],
      "Grade I",
      {
        updateTime: "2026-10-02T10:00:02Z",
        locked: true,
        currentMatchId: "winner-match-1",
        status: "MATCHED"
      }
    );
    const candidateDoc = createMockRawDoc(
      "nurse-b",
      "HOSP-002",
      ["HOSP-001"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:00Z" }
    );
    const matchDoc = {
      name: `${TEST_PROJECT_ID}/databases/(default)/documents/matches/winner-match-1`,
      updateTime: "2026-10-02T10:00:02Z",
      fields: {
        nurseAUid: { stringValue: "nurse-a" },
        nurseBUid: { stringValue: "nurse-b" },
        nurseACurrentHospitalId: { stringValue: "HOSP-001" },
        nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
        nurseADestinationHospitalId: { stringValue: "HOSP-002" },
        nurseBDestinationHospitalId: { stringValue: "HOSP-001" },
        nurseAGrade: { stringValue: "Grade I" },
        nurseBGrade: { stringValue: "Grade I" },
        isSameGrade: { booleanValue: true },
        nurseAPreferenceRank: { integerValue: "1" },
        nurseBPreferenceRank: { integerValue: "1" },
        combinedPreferenceRank: { integerValue: "2" },
        priorityReason: { stringValue: "Same-grade match (Grade I), combined preference rank 2 (A: #1, B: #1)" },
        status: { stringValue: "PENDING_CONFIRMATION" },
        createdAt: { stringValue: new Date().toISOString() },
        expiresAt: { stringValue: new Date(Date.now() + 48 * 3600 * 1000).toISOString() }
      }
    };

    let callerReadCount = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () => {
        callerReadCount += 1;
        return new Response(
          JSON.stringify(callerReadCount === 1 ? searchingCallerDoc : matchedCallerDoc),
          { status: 200 }
        );
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(
          JSON.stringify({
            error: {
              code: 409,
              message: "Document was updated concurrently",
              status: "ABORTED"
            }
          }),
          { status: 409 }
        ),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/winner-match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });

    assert.equal(result.matched, true);
    assert.equal((result as FindAndLockMatchSuccess).matchId, "winner-match-1");
    assert.equal((result as FindAndLockMatchSuccess).match.nurseBUid, "nurse-b");
    assert.equal(callerReadCount, 2);
  });

  // 12b. If no winner exists after the conflict, retry once with fresh reads/preconditions.
  test("12b - commit conflict retries once and succeeds with fresh reads", async () => {
    const callerDoc = createMockRawDoc(
      "nurse-a",
      "HOSP-001",
      ["HOSP-002"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:00Z" }
    );
    const candidateDoc = createMockRawDoc(
      "nurse-b",
      "HOSP-002",
      ["HOSP-001"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:01Z" }
    );

    let commitCount = 0;
    let callerReadCount = 0;
    let queryCount = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () => {
        callerReadCount += 1;
        return new Response(JSON.stringify(callerDoc), { status: 200 });
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () => {
        queryCount += 1;
        return new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 });
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCount += 1;
        if (commitCount === 1) {
          return new Response(
            JSON.stringify({ error: { code: 409, message: "Document was updated concurrently", status: "ABORTED" } }),
            { status: 409 }
          );
        }
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:03Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      expirationHours: 48,
      generateMatchId: () => "retry-match-1"
    });

    assert.equal(result.matched, true);
    assert.equal((result as FindAndLockMatchSuccess).matchId, "retry-match-1");
    assert.equal(commitCount, 2);
    assert.equal(callerReadCount, 3);
    assert.equal(queryCount, 2);
  });

  // 12c. Two failed attempts remain a genuine conflict; never fabricate a match.
  test("12c - repeated commit conflicts return MATCH_CONFLICT after one bounded retry", async () => {
    const callerDoc = createMockRawDoc(
      "nurse-a",
      "HOSP-001",
      ["HOSP-002"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:00Z" }
    );
    const candidateDoc = createMockRawDoc(
      "nurse-b",
      "HOSP-002",
      ["HOSP-001"],
      "Grade I",
      { updateTime: "2026-10-02T10:00:01Z" }
    );

    let commitCount = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCount += 1;
        return new Response(
          JSON.stringify({ error: { code: 409, message: "Document was updated concurrently", status: "ABORTED" } }),
          { status: 409 }
        );
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MATCH_CONFLICT");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );

    assert.equal(commitCount, 2);
  });

  // 13. Expiration duration must be explicitly configured (no implicit 48h or 72h default)
  test("13 - findAndLockMatch throws CONFIG_ERROR when expirationHours is omitted (no implicit default)", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    // Calling findAndLockMatch with no options or missing expirationHours
    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CONFIG_ERROR");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 14. Non-positive or NaN expiration duration throws CONFIG_ERROR
  test("14 - findAndLockMatch throws CONFIG_ERROR when expirationHours is non-positive or NaN", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 0 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CONFIG_ERROR");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: -12 });
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "CONFIG_ERROR");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 15. Custom explicit expiration values work accurately (e.g. 72h or 24h)
  test("15 - custom explicit expiration durations (72h and 24h) work accurately when provided", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I");

    const queryItems: FirestoreRunQueryItem[] = [{ document: candidateDoc }];
    const fixedNow = new Date("2026-10-02T00:00:00.000Z");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T00:00:01.000Z" }), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    // Test 72h explicitly
    const result72 = await findAndLockMatch("nurse-a", client, {
      now: () => fixedNow,
      expirationHours: 72
    });
    assert.equal(result72.matched, true);
    assert.equal((result72 as FindAndLockMatchSuccess).expiresAt, "2026-10-05T00:00:00.000Z"); // +72h

    // Test 24h explicitly
    const result24 = await findAndLockMatch("nurse-a", client, {
      now: () => fixedNow,
      expirationHours: 24
    });
    assert.equal(result24.matched, true);
    assert.equal((result24 as FindAndLockMatchSuccess).expiresAt, "2026-10-03T00:00:00.000Z"); // +24h
  });
});

describe("Cloudflare Worker Endpoint: POST /api/matching/find-and-lock", async () => {
  // Generate a test RSA key pair for token verification
  const keyPair = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256"
    },
    true,
    ["sign", "verify"]
  );

  const publicJwk = (await crypto.subtle.exportKey("jwk", keyPair.publicKey)) as GoogleJwk;
  publicJwk.kid = "test-kid-endpoint";
  publicJwk.alg = "RS256";

  const defaultHeader = {
    alg: "RS256",
    typ: "JWT",
    kid: "test-kid-endpoint"
  };

  const nowSeconds = Math.floor(Date.now() / 1000);
  const defaultPayload = {
    iss: `https://securetoken.google.com/${TEST_PROJECT_ID}`,
    aud: TEST_PROJECT_ID,
    sub: "verified-nurse-caller-777",
    iat: nowSeconds - 60,
    exp: nowSeconds + 3600,
    auth_time: nowSeconds - 60
  };

  const validEnv: Env = {
    FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
    ENVIRONMENT: "test",
    MATCH_EXPIRATION_HOURS: "48"
  };

  // Intercept global fetch so JWKS calls and Firestore calls are strictly mocked
  const originalFetch = globalThis.fetch;

  // 16. Unauthenticated request (missing Authorization header)
  test("16 - Worker returns 401 Unauthorized when Authorization header is missing", async () => {
    const request = new Request("https://worker.local/api/matching/find-and-lock", {
      method: "POST"
    });

    const response = await worker.fetch(request, validEnv);
    assert.equal(response.status, 401);
    const body = (await response.json()) as { error: string };
    assert.equal(body.error, "Unauthorized");
  });

  // 17. Invalid method (GET) returns 405 Method Not Allowed
  test("17 - Worker returns 405 MethodNotAllowed when using GET", async () => {
    const request = new Request("https://worker.local/api/matching/find-and-lock", {
      method: "GET"
    });

    const response = await worker.fetch(request, validEnv);
    assert.equal(response.status, 405);
    const body = (await response.json()) as { error: string };
    assert.equal(body.error, "MethodNotAllowed");
  });

  // 18. Worker returns 500 ConfigError when MATCH_EXPIRATION_HOURS is missing from env
  test("18 - Worker returns 500 ConfigError when MATCH_EXPIRATION_HOURS is missing from env (no implicit default)", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      throw new Error(`Unexpected URL: ${url}`);
    }) as typeof fetch;

    try {
      const envWithoutHours: Env = {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        ENVIRONMENT: "test"
        // MATCH_EXPIRATION_HOURS intentionally omitted
      };

      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const response = await worker.fetch(request, envWithoutHours);
      assert.equal(response.status, 500);
      const body = (await response.json()) as { error: string; message: string };
      assert.equal(body.error, "ConfigError");
      assert.ok(body.message.includes("MATCH_EXPIRATION_HOURS must be explicitly configured"));
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 19. Worker returns 500 ConfigError when MATCH_EXPIRATION_HOURS is non-numeric in env
  test("19 - Worker returns 500 ConfigError when MATCH_EXPIRATION_HOURS is invalid in env", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      throw new Error(`Unexpected URL: ${url}`);
    }) as typeof fetch;

    try {
      const envInvalidHours: Env = {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        ENVIRONMENT: "test",
        MATCH_EXPIRATION_HOURS: "not-a-number"
      };

      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const response = await worker.fetch(request, envInvalidHours);
      assert.equal(response.status, 500);
      const body = (await response.json()) as { error: string; message: string };
      assert.equal(body.error, "ConfigError");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 20. Security Invariant: Body UID cannot override verified JWT UID
  test("20 - Worker strictly uses JWT UID and ignores any body-supplied UID", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const callerDoc = createMockRawDoc("verified-nurse-caller-777", "HOSP-001", ["HOSP-002"], "Grade I");
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I");

    let requestedDocUid = "";

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

      // Google JWKS
      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        });
      }

      // Firestore get transferRequests/{uid}
      if (url.includes("/transferRequests/")) {
        const segments = url.split("/");
        requestedDocUid = segments[segments.length - 1];
        return new Response(JSON.stringify(callerDoc), { status: 200 });
      }

      // Firestore runQuery
      if (url.includes(":runQuery")) {
        return new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 });
      }

      // Firestore commit
      if (url.includes(":commit")) {
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:00Z" }), { status: 200 });
      }

      throw new Error(`Unexpected URL in test: ${url}`);
    }) as typeof fetch;

    try {
      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        // Attacker attempts to forge identity
        body: JSON.stringify({ uid: "attacker-impersonated-uid" })
      });

      const response = await worker.fetch(request, validEnv);
      assert.equal(response.status, 200);

      // Verify the document fetched was for the JWT sub, NOT the body uid
      assert.equal(requestedDocUid, "verified-nurse-caller-777");
      assert.notEqual(requestedDocUid, "attacker-impersonated-uid");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 21. Successful matching and locking via Worker HTTP endpoint (with configured 48h)
  test("21 - Worker executes matching and returns 200 JSON with match details", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const callerDoc = createMockRawDoc("verified-nurse-caller-777", "HOSP-001", ["HOSP-002"], "Grade I");
    const candidateDoc = createMockRawDoc("nurse-target", "HOSP-002", ["HOSP-001"], "Grade I");

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      if (url.includes("/transferRequests/verified-nurse-caller-777")) {
        return new Response(JSON.stringify(callerDoc), { status: 200 });
      }
      if (url.includes(":runQuery")) {
        return new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 });
      }
      if (url.includes(":commit")) {
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:00Z" }), { status: 200 });
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as typeof fetch;

    try {
      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const response = await worker.fetch(request, validEnv);
      assert.equal(response.status, 200);

      const body = (await response.json()) as FindAndLockMatchSuccess;
      assert.equal(body.matched, true);
      assert.ok(body.matchId);
      assert.equal(body.match.nurseAUid, "verified-nurse-caller-777");
      assert.equal(body.match.nurseBUid, "nurse-target");
      assert.equal(body.match.isSameGrade, true);
      assert.ok(body.expiresAt);
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 22. Worker with configured 72-hour window applies 72 hours accurately
  test("22 - Worker endpoint applies explicit 72-hour configuration accurately", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const callerDoc = createMockRawDoc("verified-nurse-caller-777", "HOSP-001", ["HOSP-002"], "Grade I");
    const candidateDoc = createMockRawDoc("nurse-target", "HOSP-002", ["HOSP-001"], "Grade I");

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      if (url.includes("/transferRequests/verified-nurse-caller-777")) {
        return new Response(JSON.stringify(callerDoc), { status: 200 });
      }
      if (url.includes(":runQuery")) {
        return new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 });
      }
      if (url.includes(":commit")) {
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:00Z" }), { status: 200 });
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as typeof fetch;

    try {
      const env72: Env = {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        ENVIRONMENT: "test",
        MATCH_EXPIRATION_HOURS: "72"
      };

      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const response = await worker.fetch(request, env72);
      assert.equal(response.status, 200);

      const body = (await response.json()) as FindAndLockMatchSuccess;
      assert.equal(body.matched, true);
      const createdTime = new Date(body.createdAt).getTime();
      const expiresTime = new Date(body.expiresAt).getTime();
      const diffHours = (expiresTime - createdTime) / (3600 * 1000);
      assert.equal(diffHours, 72);
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 23. Worker returns 200 with matched: false when no partner is compatible
  test("23 - Worker returns 200 with matched: false when no compatible candidate exists", async () => {
    const token = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const callerDoc = createMockRawDoc("verified-nurse-caller-777", "HOSP-001", ["HOSP-002"], "Grade I");

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

      if (url.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      if (url.includes("/transferRequests/verified-nurse-caller-777")) {
        return new Response(JSON.stringify(callerDoc), { status: 200 });
      }
      if (url.includes(":runQuery")) {
        return new Response(JSON.stringify([]), { status: 200 }); // empty pool
      }

      throw new Error(`Unexpected URL: ${url}`);
    }) as typeof fetch;

    try {
      const request = new Request("https://worker.local/api/matching/find-and-lock", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const response = await worker.fetch(request, validEnv);
      assert.equal(response.status, 200);

      const body = (await response.json()) as FindAndLockNoMatch;
      assert.equal(body.matched, false);
      assert.equal(body.message, "No compatible match found");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // =========================================================================
  // 3-Way Matching Service Integration Tests (Phase 1.5.4)
  // =========================================================================

  // 24. Direct 2-way match remains preferred over 3-way cycle when both are available
  test("24 - Direct 2-way match remains preferred when both 2-way and 3-way are possible", async () => {
    // Caller: HOSP-001 -> wants HOSP-002
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    // Nurse B: HOSP-002 -> wants HOSP-001 (Direct 2-way!) AND HOSP-003
    const nurseBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001", "HOSP-003"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    // Nurse C: HOSP-003 -> wants HOSP-001 (would also form A -> B -> C -> A 3-way cycle)
    const nurseCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });

    const queryItems: FirestoreRunQueryItem[] = [
      { document: nurseBDoc },
      { document: nurseCDoc }
    ];

    let capturedWrites: FirestoreWrite[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      expirationHours: 48,
      generateMatchId: () => "match-direct-priority"
    });

    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.matchType, "DIRECT_2_WAY");
    assert.equal((success.match as DirectMatch).nurseBUid, "nurse-b");
    assert.equal(capturedWrites.length, 3, "2-way direct match must produce exactly 3 writes");
  });

  // 25. Valid 3-way cycle creates exactly four atomic writes locking A, B, C and match doc
  test("25 - Valid 3-way cycle creates exactly four atomic writes and locks A, B, C", async () => {
    // A: HOSP-001 -> wants HOSP-002
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00.111Z"
    });
    // B: HOSP-002 -> wants HOSP-003 (NOT HOSP-001, so NO 2-way match)
    const nurseBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-003"], "Grade I", {
      updateTime: "2026-10-02T10:00:00.222Z"
    });
    // C: HOSP-003 -> wants HOSP-001 (closes cycle C -> A)
    const nurseCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade I", {
      updateTime: "2026-10-02T10:00:00.333Z"
    });

    const queryItems: FirestoreRunQueryItem[] = [
      { document: nurseBDoc },
      { document: nurseCDoc }
    ];

    let capturedWrites: FirestoreWrite[] = [];
    const fixedNow = new Date("2026-10-02T12:00:00.000Z");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:01Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, {
      now: () => fixedNow,
      expirationHours: 48,
      generateMatchId: () => "match-3way-test"
    });

    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.matchType, "THREE_WAY");
    assert.equal(success.matchId, "match-3way-test");
    assert.equal(success.createdAt, "2026-10-02T12:00:00.000Z");
    assert.equal(success.expiresAt, "2026-10-04T12:00:00.000Z"); // 48h expiration

    // 4 atomic writes check
    assert.equal(capturedWrites.length, 4, "Must create exactly 4 atomic writes");

    // Write 1: Caller A locked
    const writeA = capturedWrites[0];
    assert.ok(writeA.update.name.endsWith("/transferRequests/nurse-a"));
    assert.deepEqual(writeA.update.fields.locked, { booleanValue: true });
    assert.deepEqual(writeA.update.fields.status, { stringValue: "MATCHED" });
    assert.deepEqual(writeA.update.fields.currentMatchId, { stringValue: "match-3way-test" });
    assert.deepEqual(writeA.currentDocument, { updateTime: "2026-10-02T10:00:00.111Z" });

    // Write 2: Candidate B locked
    const writeB = capturedWrites[1];
    assert.ok(writeB.update.name.endsWith("/transferRequests/nurse-b"));
    assert.deepEqual(writeB.update.fields.locked, { booleanValue: true });
    assert.deepEqual(writeB.update.fields.status, { stringValue: "MATCHED" });
    assert.deepEqual(writeB.update.fields.currentMatchId, { stringValue: "match-3way-test" });
    assert.deepEqual(writeB.currentDocument, { updateTime: "2026-10-02T10:00:00.222Z" });

    // Write 3: Candidate C locked
    const writeC = capturedWrites[2];
    assert.ok(writeC.update.name.endsWith("/transferRequests/nurse-c"));
    assert.deepEqual(writeC.update.fields.locked, { booleanValue: true });
    assert.deepEqual(writeC.update.fields.status, { stringValue: "MATCHED" });
    assert.deepEqual(writeC.update.fields.currentMatchId, { stringValue: "match-3way-test" });
    assert.deepEqual(writeC.currentDocument, { updateTime: "2026-10-02T10:00:00.333Z" });

    // Write 4: Match document with all A/B/C fields
    const writeMatch = capturedWrites[3];
    assert.ok(writeMatch.update.name.endsWith("/matches/match-3way-test"));
    assert.deepEqual(writeMatch.update.fields.nurseAUid, { stringValue: "nurse-a" });
    assert.deepEqual(writeMatch.update.fields.nurseBUid, { stringValue: "nurse-b" });
    assert.deepEqual(writeMatch.update.fields.nurseCUid, { stringValue: "nurse-c" });
    assert.deepEqual(writeMatch.update.fields.nurseACurrentHospitalId, { stringValue: "HOSP-001" });
    assert.deepEqual(writeMatch.update.fields.nurseBCurrentHospitalId, { stringValue: "HOSP-002" });
    assert.deepEqual(writeMatch.update.fields.nurseCCurrentHospitalId, { stringValue: "HOSP-003" });
    assert.deepEqual(writeMatch.update.fields.nurseADestinationHospitalId, { stringValue: "HOSP-002" });
    assert.deepEqual(writeMatch.update.fields.nurseBDestinationHospitalId, { stringValue: "HOSP-003" });
    assert.deepEqual(writeMatch.update.fields.nurseCDestinationHospitalId, { stringValue: "HOSP-001" });
    assert.deepEqual(writeMatch.update.fields.nurseAGrade, { stringValue: "Grade I" });
    assert.deepEqual(writeMatch.update.fields.nurseBGrade, { stringValue: "Grade I" });
    assert.deepEqual(writeMatch.update.fields.nurseCGrade, { stringValue: "Grade I" });
    assert.deepEqual(writeMatch.update.fields.isAllSameGrade, { booleanValue: true });
    assert.deepEqual(writeMatch.update.fields.acceptedByA, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.acceptedByB, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.acceptedByC, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.rejectedByA, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.rejectedByB, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.rejectedByC, { booleanValue: false });
    assert.deepEqual(writeMatch.update.fields.status, { stringValue: "PENDING_CONFIRMATION" });
    assert.deepEqual(writeMatch.currentDocument, { exists: false });
  });

  // 26. Cross-grade 3-way cycle is allowed and preserves grade details
  test("26 - Cross-grade 3-way cycle is accepted and isAllSameGrade is false", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    const nurseBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-003"], "Grade II");
    const nurseCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade III");

    const queryItems: FirestoreRunQueryItem[] = [
      { document: nurseBDoc },
      { document: nurseCDoc }
    ];

    let capturedWrites: FirestoreWrite[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        capturedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    const match = success.match as ThreeWayMatch;
    assert.equal(match.isAllSameGrade, false);
    assert.equal(match.nurseAGrade, "Grade I");
    assert.equal(match.nurseBGrade, "Grade II");
    assert.equal(match.nurseCGrade, "Grade III");
    assert.deepEqual(capturedWrites[3].update.fields.isAllSameGrade, { booleanValue: false });
  });

  // 27. Four-way cycle is never passed to service as a valid 3-way match
  test("27 - Four-way cycle A->B->C->D->A returns matched: false", async () => {
    // A -> B -> C -> D -> A (no 3-way subset closes)
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    const nurseBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-003"], "Grade I");
    const nurseCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-004"], "Grade I");
    const nurseDDoc = createMockRawDoc("nurse-d", "HOSP-004", ["HOSP-001"], "Grade I");

    const queryItems: FirestoreRunQueryItem[] = [
      { document: nurseBDoc },
      { document: nurseCDoc },
      { document: nurseDDoc }
    ];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify(queryItems), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, false);
  });

  // 28. Concurrent modification of participant B or C aborts atomic commit and triggers conflict recovery
  test("28 - Concurrent modification of participant in 3-way commit aborts atomically and recovers", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    const nurseBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-003"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });
    const nurseCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade I", {
      updateTime: "2026-10-02T10:00:00Z"
    });

    const matchedCallerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      updateTime: "2026-10-02T10:00:02Z",
      locked: true,
      currentMatchId: "winning-3way-match",
      status: "MATCHED"
    });

    const winningMatchDoc = {
      name: `${TEST_PROJECT_ID}/databases/(default)/documents/matches/winning-3way-match`,
      updateTime: "2026-10-02T10:00:02Z",
      fields: {
        nurseAUid: { stringValue: "nurse-a" },
        nurseBUid: { stringValue: "nurse-b" },
        nurseCUid: { stringValue: "nurse-c" },
        nurseACurrentHospitalId: { stringValue: "HOSP-001" },
        nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
        nurseCCurrentHospitalId: { stringValue: "HOSP-003" },
        nurseADestinationHospitalId: { stringValue: "HOSP-002" },
        nurseBDestinationHospitalId: { stringValue: "HOSP-003" },
        nurseCDestinationHospitalId: { stringValue: "HOSP-001" },
        nurseAGrade: { stringValue: "Grade I" },
        nurseBGrade: { stringValue: "Grade I" },
        nurseCGrade: { stringValue: "Grade I" },
        isAllSameGrade: { booleanValue: true },
        nurseAPreferenceRank: { integerValue: "1" },
        nurseBPreferenceRank: { integerValue: "1" },
        nurseCPreferenceRank: { integerValue: "1" },
        combinedPreferenceRank: { integerValue: "3" },
        priorityReason: { stringValue: "All-same-grade 3-way cycle" },
        status: { stringValue: "PENDING_CONFIRMATION" },
        createdAt: { stringValue: new Date().toISOString() },
        expiresAt: { stringValue: new Date(Date.now() + 48 * 3600 * 1000).toISOString() }
      }
    };

    let callerReadCount = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () => {
        callerReadCount += 1;
        return new Response(
          JSON.stringify(callerReadCount === 1 ? callerDoc : matchedCallerDoc),
          { status: 200 }
        );
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: nurseBDoc }, { document: nurseCDoc }]), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(
          JSON.stringify({
            error: {
              code: 409,
              message: "Document was updated concurrently (Precondition failed)",
              status: "ABORTED"
            }
          }),
          { status: 409 }
        ),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/winning-3way-match`]: () =>
        new Response(JSON.stringify(winningMatchDoc), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.matchId, "winning-3way-match");
    assert.equal(success.matchType, "THREE_WAY");
    assert.equal((success.match as ThreeWayMatch).nurseCUid, "nurse-c");
  });

  // 29. Caller already MATCHED with a 3-way match recovers cleanly on initial invocation (no duplicate match)
  test("29 - Already MATCHED caller pointing to a 3-way match recovers cleanly on first read", async () => {
    const matchedCallerDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade I", {
      locked: true,
      currentMatchId: "existing-3way-match-123",
      status: "MATCHED"
    });

    const existingMatchDoc = {
      name: `${TEST_PROJECT_ID}/databases/(default)/documents/matches/existing-3way-match-123`,
      updateTime: "2026-10-02T10:00:00Z",
      fields: {
        nurseAUid: { stringValue: "nurse-a" },
        nurseBUid: { stringValue: "nurse-b" },
        nurseCUid: { stringValue: "nurse-c" },
        nurseACurrentHospitalId: { stringValue: "HOSP-001" },
        nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
        nurseCCurrentHospitalId: { stringValue: "HOSP-003" },
        nurseADestinationHospitalId: { stringValue: "HOSP-002" },
        nurseBDestinationHospitalId: { stringValue: "HOSP-003" },
        nurseCDestinationHospitalId: { stringValue: "HOSP-001" },
        nurseAGrade: { stringValue: "Grade I" },
        nurseBGrade: { stringValue: "Grade I" },
        nurseCGrade: { stringValue: "Grade I" },
        isAllSameGrade: { booleanValue: true },
        nurseAPreferenceRank: { integerValue: "1" },
        nurseBPreferenceRank: { integerValue: "1" },
        nurseCPreferenceRank: { integerValue: "1" },
        combinedPreferenceRank: { integerValue: "3" },
        priorityReason: { stringValue: "All-same-grade 3-way cycle" },
        status: { stringValue: "PENDING_CONFIRMATION" },
        createdAt: { stringValue: new Date().toISOString() },
        expiresAt: { stringValue: new Date(Date.now() + 48 * 3600 * 1000).toISOString() }
      }
    };

    let commitCalled = false;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`]: () =>
        new Response(JSON.stringify(matchedCallerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/existing-3way-match-123`]: () =>
        new Response(JSON.stringify(existingMatchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCalled = true;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-c", client, { expirationHours: 48 });
    assert.equal(result.matched, true);
    const success = result as FindAndLockMatchSuccess;
    assert.equal(success.matchType, "THREE_WAY");
    assert.equal(success.matchId, "existing-3way-match-123");
    assert.equal((success.match as ThreeWayMatch).nurseAUid, "nurse-a");
    assert.equal((success.match as ThreeWayMatch).nurseBUid, "nurse-b");
    assert.equal((success.match as ThreeWayMatch).nurseCUid, "nurse-c");
    assert.equal(commitCalled, false, "Must never attempt a new commit when recovering existing match");
  });

  // 30. Stale CANCELLED match resets caller and allows new matching
  test("30 - Stale CANCELLED match resets caller to SEARCHING and proceeds to match", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      locked: true,
      currentMatchId: "stale-cancelled-123",
      status: "MATCHED"
    });
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I", {
      status: "SEARCHING",
      locked: false
    });
    const staleCancelledMatchDoc = {
      name: `${TEST_PROJECT_ID}/databases/(default)/documents/matches/stale-cancelled-123`,
      fields: {
        nurseAUid: { stringValue: "nurse-a" },
        nurseBUid: { stringValue: "nurse-x" },
        status: { stringValue: "CANCELLED" },
        createdAt: { stringValue: "2026-10-01T10:00:00Z" },
        expiresAt: { stringValue: new Date(Date.now() + 48 * 3600 * 1000).toISOString() }
      }
    };

    let resetCommitted = false;
    let matchCommitted = false;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/stale-cancelled-123`]: () =>
        new Response(JSON.stringify(staleCancelledMatchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        if (body.writes.length === 1 && body.writes[0].updateMask.fieldPaths.includes("currentMatchId")) {
          resetCommitted = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 });
        }
        matchCommitted = true;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:02Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, true);
    assert.equal(resetCommitted, true, "Must commit reset of stale caller");
    assert.equal(matchCommitted, true, "Must commit new match");
    assert.equal((result as FindAndLockMatchSuccess).match.nurseBUid, "nurse-b");
  });

  // 31. Stale EXPIRED match resets caller and allows new matching
  test("31 - Stale EXPIRED match resets caller to SEARCHING and proceeds to match", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      locked: true,
      currentMatchId: "stale-expired-456",
      status: "MATCHED"
    });
    const candidateDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I", {
      status: "SEARCHING",
      locked: false
    });
    const staleExpiredMatchDoc = {
      name: `${TEST_PROJECT_ID}/databases/(default)/documents/matches/stale-expired-456`,
      fields: {
        nurseAUid: { stringValue: "nurse-a" },
        nurseBUid: { stringValue: "nurse-x" },
        status: { stringValue: "PENDING_CONFIRMATION" },
        createdAt: { stringValue: "2026-09-01T10:00:00Z" },
        expiresAt: { stringValue: "2026-09-03T10:00:00Z" } // Expired long ago
      }
    };

    let resetCommitted = false;
    let matchCommitted = false;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/stale-expired-456`]: () =>
        new Response(JSON.stringify(staleExpiredMatchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: candidateDoc }]), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        if (body.writes.length === 1 && body.writes[0].updateMask.fieldPaths.includes("currentMatchId")) {
          resetCommitted = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 });
        }
        matchCommitted = true;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:02Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
    assert.equal(result.matched, true);
    assert.equal(resetCommitted, true, "Must commit reset of expired caller");
    assert.equal(matchCommitted, true, "Must commit new match");
  });

  // 32. Candidate pool retrieval queries subsequent batches with offset beyond 100 boundary
  test("32 - Candidate pool retrieval queries beyond 100 items using bounded batch pagination", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    // Candidate in second batch (offset 100)
    const matchingCandidate = createMockRawDoc("nurse-target", "HOSP-002", ["HOSP-001"], "Grade I", {
      status: "SEARCHING",
      locked: false
    });

    // 100 non-matching candidates in first batch
    const firstBatchDocs = Array.from({ length: 100 }, (_, i) => ({
      document: createMockRawDoc(`nurse-filler-${i}`, "HOSP-999", ["HOSP-888"], "Grade I", {
        status: "SEARCHING",
        locked: false
      })
    }));

    let queryCalls = 0;
    const offsetsReceived: (number | undefined)[] = [];

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: async (req: Request) => {
        queryCalls += 1;
        const body = (await req.json()) as { structuredQuery: { limit: number; offset?: number } };
        offsetsReceived.push(body.structuredQuery.offset);

        if (queryCalls === 1) {
          // First batch: 100 items
          return new Response(JSON.stringify(firstBatchDocs), { status: 200 });
        }
        // Second batch: return the matching candidate
        return new Response(JSON.stringify([{ document: matchingCandidate }]), { status: 200 });
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:01Z" }), { status: 200 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48, candidateLimit: 150 });
    assert.equal(result.matched, true);
    assert.equal((result as FindAndLockMatchSuccess).match.nurseBUid, "nurse-target");
    assert.equal(queryCalls, 2, "Must query second batch");
    assert.equal(offsetsReceived[0], undefined);
    assert.equal(offsetsReceived[1], 100, "Second batch must request offset 100");
  });

  // 33. Referencing a COMPLETED match fails with 409 MATCH_ALREADY_COMPLETED and never resets caller
  test("33 - Referencing a COMPLETED match returns 409 MATCH_ALREADY_COMPLETED and never resets caller", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "completed-match-999"
    });

    const completedMatchDoc = createMock3WayMatchDoc(
      "completed-match-999",
      "nurse-a",
      "nurse-b",
      "nurse-c",
      "HOSP-001",
      "HOSP-002",
      "HOSP-003",
      "HOSP-002",
      "HOSP-003",
      "HOSP-001",
      { status: "COMPLETED" }
    );

    let commitCalled = false;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/completed-match-999`]: () =>
        new Response(JSON.stringify(completedMatchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCalled = true;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T10:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await findAndLockMatch("nurse-a", client, { expirationHours: 48 });
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 409);
        assert.equal(matchErr.code, "MATCH_ALREADY_COMPLETED");
        return true;
      }
    );

    assert.equal(commitCalled, false, "Must never commit a reset write for COMPLETED match caller");
  });

  // 34. Candidate limit configured via env / options is respected
  test("34 - Worker respects candidate limit when searching candidates", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I");
    let limitReceived = 0;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: async (req: Request) => {
        const body = (await req.json()) as { structuredQuery: { limit: number } };
        limitReceived = body.structuredQuery.limit;
        return new Response(JSON.stringify([]), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await findAndLockMatch("nurse-a", client, { expirationHours: 48, candidateLimit: 50 });
    assert.equal(result.matched, false);
    assert.equal(limitReceived, 50, "StructuredQuery limit must reflect configured candidateLimit");
  });

  // 35. Server-side sweepExpiredMatches marks expired matches as EXPIRED and unlocks participants
  test("35 - sweepExpiredMatches marks expired matches as EXPIRED and releases participants", async () => {
    const now = new Date("2026-10-03T12:00:00Z");

    // Match 1: 2-way expired
    const expiredMatch2Way = createMock2WayMatchDoc(
      "expired-match-1",
      "nurse-a",
      "nurse-b",
      "HOSP-001",
      "HOSP-002",
      "HOSP-002",
      "HOSP-001",
      {
        status: "PENDING_CONFIRMATION",
        expiresAt: "2026-10-03T10:00:00Z" // Expired 2 hours ago
      }
    );

    // Match 2: 3-way expired
    const expiredMatch3Way = createMock3WayMatchDoc(
      "expired-match-2",
      "nurse-c",
      "nurse-d",
      "nurse-e",
      "HOSP-003",
      "HOSP-004",
      "HOSP-005",
      "HOSP-004",
      "HOSP-005",
      "HOSP-003",
      {
        status: "PENDING_CONFIRMATION",
        expiresAt: "2026-10-03T11:00:00Z" // Expired 1 hour ago
      }
    );

    // Match 3: Still active / not expired
    const activeMatch = createMock2WayMatchDoc(
      "active-match-3",
      "nurse-x",
      "nurse-y",
      "HOSP-006",
      "HOSP-007",
      "HOSP-007",
      "HOSP-006",
      {
        status: "PENDING_CONFIRMATION",
        expiresAt: "2026-10-03T14:00:00Z" // Expires in 2 hours
      }
    );

    const docA = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "expired-match-1"
    });
    const docB = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "expired-match-1"
    });
    const docC = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-004"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "expired-match-2"
    });
    const docD = createMockRawDoc("nurse-d", "HOSP-004", ["HOSP-005"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "expired-match-2"
    });
    // Participant E has moved on to a newer match (must NOT be reset)
    const docE = createMockRawDoc("nurse-e", "HOSP-005", ["HOSP-003"], "Grade I", {
      status: "MATCHED",
      locked: true,
      currentMatchId: "newer-match-777"
    });

    const committedWrites: FirestoreWrite[][] = [];

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(
          JSON.stringify([
            { document: expiredMatch2Way },
            { document: expiredMatch3Way },
            { document: activeMatch }
          ]),
          { status: 200 }
        ),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(docA), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(docB), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`]: () =>
        new Response(JSON.stringify(docC), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-d`]: () =>
        new Response(JSON.stringify(docD), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-e`]: () =>
        new Response(JSON.stringify(docE), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: FirestoreWrite[] };
        committedWrites.push(body.writes);
        return new Response(JSON.stringify({ commitTime: "2026-10-03T12:00:01Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const sweepResult = await sweepExpiredMatches(client, { now: () => now });

    assert.equal(sweepResult.scanned, 3);
    assert.equal(sweepResult.expired, 2);
    // Unlocked: A, B (from match 1) + C, D (from match 2, E was skipped because of newer match)
    assert.equal(sweepResult.unlockedParticipants, 4);
    assert.equal(sweepResult.errors, 0);

    assert.equal(committedWrites.length, 2, "Must commit 2 batches (one per expired match)");

    // Batch 1: match 1 status -> EXPIRED, nurse-a unlocked, nurse-b unlocked
    assert.equal(committedWrites[0].length, 3);
    const match1Write = committedWrites[0][0];
    assert.equal(match1Write.update?.fields?.status?.stringValue, "EXPIRED");

    // Batch 2: match 2 status -> EXPIRED, nurse-c unlocked, nurse-d unlocked (E skipped)
    assert.equal(committedWrites[1].length, 3);
    const match2Write = committedWrites[1][0];
    assert.equal(match2Write.update?.fields?.status?.stringValue, "EXPIRED");
  });

  // 36. Endpoint POST /api/matching/sweep-expired executes sweep successfully
  test("36 - Endpoint POST /api/matching/sweep-expired triggers sweep successfully", async () => {
    const originalFetch = globalThis.fetch;
    let queryCalled = false;

    globalThis.fetch = (async (input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
      if (url.includes(":runQuery")) {
        queryCalled = true;
        return new Response(JSON.stringify([]), { status: 200 });
      }
      throw new Error(`Unexpected URL in test: ${url}`);
    }) as typeof fetch;

    try {
      const request = new Request("https://worker.local/api/matching/sweep-expired", {
        method: "POST"
      });

      const response = await worker.fetch(request, {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        MATCH_EXPIRATION_HOURS: "48"
      });

      assert.equal(response.status, 200);
      const body = (await response.json()) as { scanned: number; expired: number };
      assert.equal(body.scanned, 0);
      assert.equal(body.expired, 0);
      assert.equal(queryCalled, true);
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  // 37. sweepExpiredMatches handles participant read error gracefully without crashing sweep
  test("37 - sweepExpiredMatches records errors when participant read fails", async () => {
    const now = new Date("2026-10-03T12:00:00Z");
    const expiredMatch = createMock2WayMatchDoc(
      "expired-match-err",
      "nurse-a",
      "nurse-b",
      "HOSP-001",
      "HOSP-002",
      "HOSP-002",
      "HOSP-001",
      {
        status: "PENDING_CONFIRMATION",
        expiresAt: "2026-10-03T10:00:00Z"
      }
    );

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () =>
        new Response(JSON.stringify([{ document: expiredMatch }]), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response("Internal Server Error", { status: 500 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const sweepResult = await sweepExpiredMatches(client, { now: () => now });
    assert.equal(sweepResult.scanned, 1);
    assert.equal(sweepResult.expired, 0);
    assert.equal(sweepResult.errors, 1, "Must increment error counter when participant read fails");
  });
});
