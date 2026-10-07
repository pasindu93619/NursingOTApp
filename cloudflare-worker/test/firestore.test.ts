import test, { describe } from "node:test";
import assert from "node:assert/strict";
import {
  FirestoreClient,
  FirestoreError,
  toFirestoreValue,
  fromFirestoreValue,
  toFirestoreFields,
  candidateRequestFromDoc,
  getTransferRequestDocPath,
  getMatchDocPath,
  type HttpTransport,
  type TokenProvider,
  type FirestoreRawDocument,
  type FirestoreWrite
} from "../src/firestore/firestoreClient.ts";

const TEST_PROJECT_ID = "nursing-super-app-test";
const MOCK_TOKEN = "mock-test-access-token-xyz-secret";
const mockTokenProvider: TokenProvider = async () => MOCK_TOKEN;

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

describe("Firestore REST Client (Mock-Only Offline Test Suite)", () => {
  // Test 1: Firestore document deserialization
  test("1 - Firestore document deserialization converts raw REST format to CandidateRequest", () => {
    const rawDoc: FirestoreRawDocument = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-1`,
      fields: {
        firebaseUid: { stringValue: "nurse-1" },
        status: { stringValue: "SEARCHING" },
        locked: { booleanValue: false },
        currentMatchId: { nullValue: null },
        currentHospitalId: { stringValue: "HOSP-001" },
        preferenceHospitalIds: {
          arrayValue: {
            values: [
              { stringValue: "HOSP-002" },
              { stringValue: "HOSP-003" }
            ]
          }
        },
        grade: { stringValue: "Grade I" }
      }
    };

    const parsed = candidateRequestFromDoc(rawDoc);
    assert.equal(parsed.firebaseUid, "nurse-1");
    assert.equal(parsed.status, "SEARCHING");
    assert.equal(parsed.locked, false);
    assert.equal(parsed.currentMatchId, null);
    assert.equal(parsed.currentHospitalId, "HOSP-001");
    assert.deepEqual(parsed.preferenceHospitalIds, ["HOSP-002", "HOSP-003"]);
    assert.equal(parsed.grade, "Grade I");
  });

  // Test 2: Authorization header injection
  test("2 - Authorization header injection passes Bearer token from TokenProvider", async () => {
    let capturedAuthHeader: string | null = null;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-1`]: (req) => {
        capturedAuthHeader = req.headers.get("Authorization");
        return new Response(JSON.stringify({ name: "doc", fields: {} }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await client.getRequestDoc("nurse-1");
    assert.equal(capturedAuthHeader, `Bearer ${MOCK_TOKEN}`);
  });

  // Test 3: Correct transfer request document URL
  test("3 - Correct transfer request document URL path generated", () => {
    const path = getTransferRequestDocPath(TEST_PROJECT_ID, "nurse-abc-123");
    assert.equal(
      path,
      `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-abc-123`
    );

    const matchPath = getMatchDocPath(TEST_PROJECT_ID, "match-xyz-789");
    assert.equal(
      matchPath,
      `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-xyz-789`
    );
  });

  // Test 4: Successful getRequestDoc()
  test("4 - Successful getRequestDoc() returns parsed candidate request", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-10`]: () => {
        return new Response(
          JSON.stringify({
            name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-10`,
            fields: {
              firebaseUid: { stringValue: "nurse-10" },
              status: { stringValue: "SEARCHING" },
              locked: { booleanValue: false },
              currentHospitalId: { stringValue: "HOSP-A" },
              preferenceHospitalIds: {
                arrayValue: { values: [{ stringValue: "HOSP-B" }] }
              },
              grade: { stringValue: "Grade II" }
            }
          }),
          { status: 200 }
        );
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const doc = await client.getRequestDoc("nurse-10");
    assert.ok(doc);
    assert.equal(doc.firebaseUid, "nurse-10");
    assert.equal(doc.grade, "Grade II");
    assert.equal(doc.currentHospitalId, "HOSP-A");
  });

  // Test 5: 404 returns null
  test("5 - 404 returns null cleanly without throwing", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-missing`]: () => {
        return new Response(JSON.stringify({ error: "NOT_FOUND" }), { status: 404 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const result = await client.getRequestDoc("nurse-missing");
    assert.equal(result, null);
  });

  // Test 6: Non-404 HTTP error throws controlled error
  test("6 - Non-404 HTTP error throws controlled FirestoreError", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-err`]: () => {
        return new Response(JSON.stringify({ error: "PERMISSION_DENIED" }), { status: 403 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await client.getRequestDoc("nurse-err");
      },
      (err: FirestoreError) => {
        assert.equal(err.statusCode, 403);
        assert.match(err.message, /HTTP 403/);
        return true;
      }
    );
  });

  // Test 7: Structured searching query contains status == SEARCHING and locked == false
  test("7 - Structured searching query contains status == SEARCHING and locked == false", async () => {
    let capturedRequestBody: any = null;

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: async (req) => {
        capturedRequestBody = await req.json();
        return new Response(JSON.stringify([]), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await client.querySearchingCandidates(50);

    assert.ok(capturedRequestBody);
    const structuredQuery = capturedRequestBody.structuredQuery;
    assert.equal(structuredQuery.from[0].collectionId, "transferRequests");
    assert.equal(structuredQuery.limit, 50);

    const filters = structuredQuery.where.compositeFilter.filters;
    assert.equal(filters.length, 2);

    const statusFilter = filters.find((f: any) => f.fieldFilter.field.fieldPath === "status");
    assert.equal(statusFilter.fieldFilter.op, "EQUAL");
    assert.equal(statusFilter.fieldFilter.value.stringValue, "SEARCHING");

    const lockedFilter = filters.find((f: any) => f.fieldFilter.field.fieldPath === "locked");
    assert.equal(lockedFilter.fieldFilter.op, "EQUAL");
    assert.equal(lockedFilter.fieldFilter.value.booleanValue, false);
  });

  // Test 8: Searching candidates parse correctly
  test("8 - Searching candidates parse correctly from runQuery results", async () => {
    const queryResults = [
      {
        document: {
          name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c1`,
          fields: {
            firebaseUid: { stringValue: "nurse-c1" },
            status: { stringValue: "SEARCHING" },
            locked: { booleanValue: false },
            currentHospitalId: { stringValue: "HOSP-002" },
            preferenceHospitalIds: {
              arrayValue: { values: [{ stringValue: "HOSP-001" }] }
            },
            grade: { stringValue: "Grade I" }
          }
        }
      },
      {
        // runQuery may contain readTime metadata without document
        readTime: "2026-10-01T00:00:00Z"
      },
      {
        document: {
          name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c2`,
          fields: {
            firebaseUid: { stringValue: "nurse-c2" },
            status: { stringValue: "SEARCHING" },
            locked: { booleanValue: false },
            currentHospitalId: { stringValue: "HOSP-003" },
            preferenceHospitalIds: {
              arrayValue: { values: [{ stringValue: "HOSP-001" }] }
            },
            grade: { stringValue: "Grade II" }
          }
        }
      }
    ];

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: () => {
        return new Response(JSON.stringify(queryResults), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const candidates = await client.querySearchingCandidates();
    assert.equal(candidates.length, 2);
    assert.equal(candidates[0].firebaseUid, "nurse-c1");
    assert.equal(candidates[1].firebaseUid, "nurse-c2");
    assert.equal(candidates[1].grade, "Grade II");
  });

  // Test 9: Candidate query does NOT enforce grade equality
  test("9 - Candidate query does NOT enforce grade equality (cross-grade eligible)", async () => {
    let capturedRequestBody: any = null;

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:runQuery`]: async (req) => {
        capturedRequestBody = await req.json();
        return new Response(JSON.stringify([]), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await client.querySearchingCandidates();

    const filters = capturedRequestBody.structuredQuery.where.compositeFilter.filters;
    const gradeFilter = filters.find((f: any) => f.fieldFilter?.field?.fieldPath === "grade");
    assert.equal(gradeFilter, undefined, "Structured query must NOT contain a grade filter");
  });

  // Test 10: Atomic commit payload contains caller-provided writes
  test("10 - Atomic commit payload contains caller-provided writes exactly", async () => {
    let capturedCommitBody: any = null;

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedCommitBody = await req.json();
        return new Response(
          JSON.stringify({
            commitTime: "2026-10-01T12:00:00Z",
            writeResults: [{ updateTime: "2026-10-01T12:00:00Z" }]
          }),
          { status: 200 }
        );
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    const writes: FirestoreWrite[] = [
      {
        update: {
          name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-A`,
          fields: {
            locked: { booleanValue: true },
            currentMatchId: { stringValue: "match-123" }
          }
        },
        currentDocument: { exists: true }
      },
      {
        update: {
          name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-B`,
          fields: {
            locked: { booleanValue: true },
            currentMatchId: { stringValue: "match-123" }
          }
        },
        currentDocument: { exists: true }
      },
      {
        update: {
          name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-123`,
          fields: {
            nurseAUid: { stringValue: "nurse-A" },
            nurseBUid: { stringValue: "nurse-B" },
            status: { stringValue: "PENDING_CONFIRMATION" }
          }
        }
      }
    ];

    const result = await client.commitAtomicMatch(writes);
    assert.equal(result.commitTime, "2026-10-01T12:00:00Z");
    assert.ok(capturedCommitBody);
    assert.equal(capturedCommitBody.writes.length, 3);
    assert.equal(
      capturedCommitBody.writes[0].update.name,
      `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-A`
    );
  });

  // Test 11: Atomic commit sends Authorization header
  test("11 - Atomic commit sends Authorization header from TokenProvider", async () => {
    let capturedAuth: string | null = null;

    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: (req) => {
        capturedAuth = req.headers.get("Authorization");
        return new Response(JSON.stringify({ commitTime: "2026-10-01T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await client.commitAtomicMatch([
      {
        update: {
          name: "dummy-name",
          fields: {}
        }
      }
    ]);

    assert.equal(capturedAuth, `Bearer ${MOCK_TOKEN}`);
  });

  // Test 12: Atomic commit failure is handled
  test("12 - Atomic commit failure throws controlled FirestoreError", async () => {
    const transport = createMockTransport({
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        return new Response(JSON.stringify({ error: "ABORTED" }), { status: 409 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await client.commitAtomicMatch([{ update: { name: "x", fields: {} } }]);
      },
      (err: FirestoreError) => {
        assert.equal(err.statusCode, 409);
        assert.match(err.message, /HTTP 409/);
        return true;
      }
    );
  });

  // Test 13: Token provider failure is handled
  test("13 - Token provider failure is handled and wrapped in FirestoreError", async () => {
    const failingTokenProvider: TokenProvider = async () => {
      throw new Error("Secret key decryption failure");
    };

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: failingTokenProvider,
      transport: createMockTransport({})
    });

    await assert.rejects(
      async () => {
        await client.getRequestDoc("nurse-1");
      },
      (err: FirestoreError) => {
        assert.match(err.message, /Authentication error: Secret key decryption failure/);
        return true;
      }
    );
  });

  // Test 14: Unexpected HTTP route fails immediately
  test("14 - Unexpected HTTP route fails immediately via mock transport", async () => {
    const strictTransport = createMockTransport({
      // No routes defined
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport: strictTransport
    });

    await assert.rejects(
      async () => {
        await client.getRequestDoc("any-nurse");
      },
      (err: Error) => {
        assert.match(err.message, /Unexpected outgoing HTTP request in test/);
        return true;
      }
    );
  });

  // Test 15: No real network access occurs
  test("15 - No real network access occurs (transport intercept is guaranteed)", async () => {
    let mockInvoked = false;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-offline`]: () => {
        mockInvoked = true;
        return new Response(JSON.stringify({ name: "mocked", fields: {} }), { status: 200 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await client.getRequestDoc("nurse-offline");
    assert.equal(mockInvoked, true);
  });

  // Test 16: Sensitive token values are not exposed in errors
  test("16 - Sensitive token values are not exposed in errors", async () => {
    const sensitiveToken = "super-secret-production-oauth-token-12345";
    const sensitiveTokenProvider: TokenProvider = async () => sensitiveToken;

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-sensitive`]: () => {
        return new Response("Internal Server Error", { status: 500 });
      }
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: sensitiveTokenProvider,
      transport
    });

    try {
      await client.getRequestDoc("nurse-sensitive");
      assert.fail("Should have thrown");
    } catch (err: unknown) {
      assert.ok(err instanceof FirestoreError);
      assert.equal(err.message.includes(sensitiveToken), false);
      assert.equal(err.message.includes("Bearer"), false);
    }
  });

  // Additional helper test for value conversions
  test("17 - Value conversion helpers support primitives, arrays, and objects", () => {
    const raw = {
      name: "Colombo Hospital",
      active: true,
      count: 42,
      ratio: 3.14,
      nil: null,
      tags: ["ot", "icu"]
    };

    const fsFields = toFirestoreFields(raw);
    assert.deepEqual(fsFields.name, { stringValue: "Colombo Hospital" });
    assert.deepEqual(fsFields.active, { booleanValue: true });
    assert.deepEqual(fsFields.count, { integerValue: "42" });
    assert.deepEqual(fsFields.ratio, { doubleValue: 3.14 });
    assert.deepEqual(fsFields.nil, { nullValue: null });
    assert.deepEqual(fsFields.tags, {
      arrayValue: {
        values: [{ stringValue: "ot" }, { stringValue: "icu" }]
      }
    });

    assert.equal(fromFirestoreValue(fsFields.name), "Colombo Hospital");
    assert.equal(fromFirestoreValue(fsFields.active), true);
    assert.equal(fromFirestoreValue(fsFields.count), 42);
    assert.equal(fromFirestoreValue(fsFields.ratio), 3.14);
    assert.equal(fromFirestoreValue(fsFields.nil), null);
    assert.deepEqual(fromFirestoreValue(fsFields.tags), ["ot", "icu"]);
  });
});
