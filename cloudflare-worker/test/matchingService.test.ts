import test, { describe } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import {
  findAndLockMatch,
  MatchServiceError,
  type FindAndLockMatchSuccess,
  type FindAndLockNoMatch
} from "../src/matching/matchingService.ts";
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
        createdAt: { stringValue: "2026-10-02T10:00:02Z" },
        expiresAt: { stringValue: "2026-10-04T10:00:02Z" }
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
});
