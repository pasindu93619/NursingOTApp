import test, { describe } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import {
  respondToMatch,
  MatchServiceError
} from "../src/matching/matchingService.ts";
import {
  FirestoreClient,
  FirestoreError,
  type HttpTransport,
  type TokenProvider,
  type FirestoreRawDocument
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
      if (req.method === "GET" && url.pathname.includes("/transferRequests/")) {
        return new Response("Not found", { status: 404 });
      }
      throw new Error(`Unexpected outgoing HTTP request in test: ${req.method} ${req.url}`);
    }

    return handler(req);
  };
}

function createMockMatchDoc(
  matchId: string,
  nurseAUid: string,
  nurseBUid: string,
  options: {
    status?: string;
    acceptedByA?: boolean;
    acceptedByB?: boolean;
    rejectedByA?: boolean;
    rejectedByB?: boolean;
    expiresAt?: string;
    updateTime?: string;
  } = {}
): FirestoreRawDocument {
  const expiresAt = options.expiresAt ?? new Date(Date.now() + 48 * 3600 * 1000).toISOString();
  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/${matchId}`,
    updateTime: options.updateTime ?? "2026-10-02T12:00:00.000Z",
    fields: {
      nurseAUid: { stringValue: nurseAUid },
      nurseBUid: { stringValue: nurseBUid },
      nurseACurrentHospitalId: { stringValue: "HOSP-001" },
      nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
      status: { stringValue: options.status ?? "PENDING_CONFIRMATION" },
      acceptedByA: { booleanValue: options.acceptedByA ?? false },
      acceptedByB: { booleanValue: options.acceptedByB ?? false },
      rejectedByA: { booleanValue: options.rejectedByA ?? false },
      rejectedByB: { booleanValue: options.rejectedByB ?? false },
      expiresAt: { stringValue: expiresAt },
      createdAt: { stringValue: "2026-10-02T10:00:00.000Z" },
      updatedAt: { stringValue: options.updateTime ?? "2026-10-02T12:00:00.000Z" }
    }
  };
}

function createMockThreeWayMatchDoc(
  matchId: string,
  nurseAUid: string,
  nurseBUid: string,
  nurseCUid: string,
  options: {
    status?: string;
    acceptedByA?: boolean;
    acceptedByB?: boolean;
    acceptedByC?: boolean;
    rejectedByA?: boolean;
    rejectedByB?: boolean;
    rejectedByC?: boolean;
    expiresAt?: string;
    updateTime?: string;
    omitAcceptedByC?: boolean;
    omitRejectedByC?: boolean;
    omitNurseCUid?: boolean;
  } = {}
): FirestoreRawDocument {
  const expiresAt = options.expiresAt ?? new Date(Date.now() + 48 * 3600 * 1000).toISOString();
  const fields: Record<string, { stringValue?: string; booleanValue?: boolean }> = {
    nurseAUid: { stringValue: nurseAUid },
    nurseBUid: { stringValue: nurseBUid },
    nurseACurrentHospitalId: { stringValue: "HOSP-001" },
    nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
    nurseCCurrentHospitalId: { stringValue: "HOSP-003" },
    status: { stringValue: options.status ?? "PENDING_CONFIRMATION" },
    acceptedByA: { booleanValue: options.acceptedByA ?? false },
    acceptedByB: { booleanValue: options.acceptedByB ?? false },
    rejectedByA: { booleanValue: options.rejectedByA ?? false },
    rejectedByB: { booleanValue: options.rejectedByB ?? false },
    expiresAt: { stringValue: expiresAt },
    createdAt: { stringValue: "2026-10-02T10:00:00.000Z" },
    updatedAt: { stringValue: options.updateTime ?? "2026-10-02T12:00:00.000Z" }
  };

  if (!options.omitNurseCUid) {
    fields.nurseCUid = { stringValue: nurseCUid };
  }
  if (!options.omitAcceptedByC) {
    fields.acceptedByC = { booleanValue: options.acceptedByC ?? false };
  }
  if (!options.omitRejectedByC) {
    fields.rejectedByC = { booleanValue: options.rejectedByC ?? false };
  }

  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/${matchId}`,
    updateTime: options.updateTime ?? "2026-10-02T12:00:00.000Z",
    fields
  };
}

describe("Server-Side Match Decision Accept/Reject Suite", () => {
  // 1. Valid ACCEPT by Nurse A
  test("1 - Valid ACCEPT by first participant returns PENDING_CONFIRMATION", async () => {
    let capturedWrites: unknown = null;
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json();
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-1");
    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
    assert.equal(result.decisionApplied, true);
    assert(capturedWrites !== null);
  });

  // 2. Valid REJECT by Nurse A
  test("2 - Valid REJECT by first participant returns CANCELLED", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-1", "REJECT", client);

    assert.equal(result.matchId, "match-1");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);
  });

  // 3. Second ACCEPT advances match to CHAT_OPEN with chatDeadline
  test("3 - Second ACCEPT when opponent has already accepted returns CHAT_OPEN", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      acceptedByA: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-1");
    assert.equal(result.newStatus, "CHAT_OPEN");
    assert.ok(result.chatDeadline);
    assert.equal(result.decisionApplied, true);
  });

  // 3b. Final confirmation during CHAT_OPEN confirms the match
  test("3b - Final confirmation by all participants in CHAT_OPEN returns CONFIRMED", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });
    // add confirmedByA: true
    matchDoc.fields.confirmedByA = { booleanValue: true };
    matchDoc.fields.confirmedByB = { booleanValue: false };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-1", "CONFIRM", client);

    assert.equal(result.matchId, "match-1");
    assert.equal(result.newStatus, "CONFIRMED");
    assert.equal(result.decisionApplied, true);
  });

  // 3c. ACCEPT must never act as final confirmation once the chat is open.
  test("3c - ACCEPT during CHAT_OPEN is rejected as INVALID_DECISION", async () => {
    const matchDoc = createMockMatchDoc("match-stage-guard", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });

    let commitCalled = false;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-stage-guard`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCalled = true;
        return new Response(JSON.stringify({ commitTime: new Date().toISOString() }), { status: 200 });
      }
    });
    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => respondToMatch("nurse-b", "match-stage-guard", "ACCEPT", client),
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "INVALID_DECISION");
        assert.equal(err.statusCode, 400);
        return true;
      }
    );
    assert.equal(commitCalled, false, "Invalid stage transition must not write to Firestore");
  });

  // 3d. CONFIRM must never be accepted before all participants open the chat stage.
  test("3d - CONFIRM during PENDING_CONFIRMATION is rejected as INVALID_DECISION", async () => {
    const matchDoc = createMockMatchDoc("match-stage-guard", "nurse-a", "nurse-b");

    let commitCalled = false;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-stage-guard`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        commitCalled = true;
        return new Response(JSON.stringify({ commitTime: new Date().toISOString() }), { status: 200 });
      }
    });
    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => respondToMatch("nurse-a", "match-stage-guard", "CONFIRM", client),
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "INVALID_DECISION");
        assert.equal(err.statusCode, 400);
        return true;
      }
    );
    assert.equal(commitCalled, false, "Invalid stage transition must not write to Firestore");
  });

  // 4. Either REJECT cancels the match even if opponent accepted
  test("4 - Either REJECT cancels the match even if opponent accepted", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      acceptedByA: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-1", "REJECT", client);

    assert.equal(result.matchId, "match-1");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);
  });

  // 5. Non-participant rejected with 403
  test("5 - Non-participant caller is rejected with 403 NON_PARTICIPANT", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c-intruder", "match-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "NON_PARTICIPANT");
        assert.equal(err.statusCode, 403);
        return true;
      }
    );
  });

  // 6. Missing match returns 404
  test("6 - Missing match document returns 404 MATCH_NOT_FOUND", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-nonexistent`]: () =>
        new Response(JSON.stringify({ error: "Not found" }), { status: 404 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-nonexistent", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MATCH_NOT_FOUND");
        assert.equal(err.statusCode, 404);
        return true;
      }
    );
  });

  // 7. Malformed match doc rejected
  test("7 - Malformed match document missing required fields throws 500 MALFORMED_MATCH_DOC", async () => {
    const malformedDoc: FirestoreRawDocument = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-bad`,
      fields: {
        nurseAUid: { stringValue: "nurse-a" }
        // missing nurseBUid, status, expiresAt
      }
    };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-bad`]: () =>
        new Response(JSON.stringify(malformedDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-bad", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MALFORMED_MATCH_DOC");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 8. Duplicate decision rejected safely
  test("8 - Duplicate response by already responded participant throws 409 ALREADY_RESPONDED", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      acceptedByA: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "ALREADY_RESPONDED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 9. Expired match rejected
  test("9 - Decision on expired match throws 410 EXPIRED", async () => {
    const expiredTime = new Date(Date.now() - 3600 * 1000).toISOString();
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      expiresAt: expiredTime
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "EXPIRED");
        assert.equal(err.statusCode, 410);
        return true;
      }
    );
  });

  // 10. Concurrent update safety (commit precondition failure)
  test("10 - Concurrent modification on commit throws FIRESTORE_COMMIT_FAILED", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      updateTime: "2026-10-02T12:00:00.000Z"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ error: "Precondition failed" }), { status: 409 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "FIRESTORE_COMMIT_FAILED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 11. Match status is not PENDING_CONFIRMATION
  test("11 - Match already confirmed or cancelled returns 409 INVALID_STATUS", async () => {
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      status: "CONFIRMED"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "INVALID_STATUS");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // =========================================================================
  // 3-Way Match Decision Tests (Phase 1.5.4)
  // =========================================================================

  // 16. 3-way A accepts: acceptedByA = true, status remains PENDING_CONFIRMATION
  test("16 - 3-way A accepts: sets acceptedByA=true, remains PENDING_CONFIRMATION", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-3w-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.acceptedByA.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByA.booleanValue, false);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "PENDING_CONFIRMATION");
  });

  // 17. 3-way B accepts: acceptedByB = true, status remains PENDING_CONFIRMATION
  test("17 - 3-way B accepts: sets acceptedByB=true, remains PENDING_CONFIRMATION", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-3w-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.acceptedByB.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByB.booleanValue, false);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "PENDING_CONFIRMATION");
  });

  // 18. 3-way C accepts: acceptedByC = true, status remains PENDING_CONFIRMATION
  test("18 - 3-way C accepts: sets acceptedByC=true, remains PENDING_CONFIRMATION", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.acceptedByC.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByC.booleanValue, false);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "PENDING_CONFIRMATION");
  });

  // 19. All three accept: A, B already accepted, C accepts -> transitions to CHAT_OPEN
  test("19 - 3-way all three accept: C accepts when A and B accepted -> transitions to CHAT_OPEN", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      acceptedByA: true,
      acceptedByB: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "CHAT_OPEN");
    assert.ok(result.chatDeadline);
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.acceptedByC.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "CHAT_OPEN");
  });

  // 19b. 3-way unanimous final confirmation in CHAT_OPEN -> transitions to CONFIRMED
  test("19b - 3-way all three confirm in CHAT_OPEN: C confirms when A and B confirmed -> transitions to CONFIRMED", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      acceptedByC: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: true };
    matchDoc.fields.confirmedByB = { booleanValue: true };
    matchDoc.fields.confirmedByC = { booleanValue: false };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3w-1", "CONFIRM", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "CONFIRMED");
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.confirmedByC.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "CONFIRMED");
  });

  // 20. Partial acceptance (only A and B accepted) remains PENDING_CONFIRMATION
  test("20 - Partial 3-way acceptance (A and B only) remains PENDING_CONFIRMATION", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      acceptedByA: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-3w-1", "ACCEPT", client);

    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
  });

  // 21. A rejects 3-way match -> transitions to CANCELLED
  test("21 - 3-way A rejects: sets rejectedByA=true, transitions to CANCELLED", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-3w-1", "REJECT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByA.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.acceptedByA.booleanValue, false);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "CANCELLED");
  });

  // 22. B rejects 3-way match -> transitions to CANCELLED
  test("22 - 3-way B rejects: sets rejectedByB=true, transitions to CANCELLED", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-3w-1", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByB.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "CANCELLED");
  });

  // 23. C rejects 3-way match -> transitions to CANCELLED
  test("23 - 3-way C rejects: sets rejectedByC=true, transitions to CANCELLED", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3w-1", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByC.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status.stringValue, "CANCELLED");
  });

  // 24. Non-participant cannot accept 3-way match (403 NON_PARTICIPANT)
  test("24 - Non-participant caller cannot accept 3-way match, throws 403 NON_PARTICIPANT", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("intruder-x", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "NON_PARTICIPANT");
        assert.equal(err.statusCode, 403);
        return true;
      }
    );
  });

  // 25. Non-participant cannot reject 3-way match (403 NON_PARTICIPANT)
  test("25 - Non-participant caller cannot reject 3-way match, throws 403 NON_PARTICIPANT", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("intruder-x", "match-3w-1", "REJECT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "NON_PARTICIPANT");
        assert.equal(err.statusCode, 403);
        return true;
      }
    );
  });

  // 26. Missing acceptedByC does NOT count as accepted; throws 500 MALFORMED_MATCH_DOC
  test("26 - Missing acceptedByC in 3-way match throws 500 MALFORMED_MATCH_DOC", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      omitAcceptedByC: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MALFORMED_MATCH_DOC");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 27. Missing rejectedByC in 3-way match throws 500 MALFORMED_MATCH_DOC
  test("27 - Missing rejectedByC in 3-way match throws 500 MALFORMED_MATCH_DOC", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      omitRejectedByC: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c", "match-3w-1", "REJECT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MALFORMED_MATCH_DOC");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 28. Missing nurseCUid when C flags present throws 500 MALFORMED_MATCH_DOC
  test("28 - Missing nurseCUid when C flags are present throws 500 MALFORMED_MATCH_DOC", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      omitNurseCUid: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "MALFORMED_MATCH_DOC");
        assert.equal(err.statusCode, 500);
        return true;
      }
    );
  });

  // 29. Repeated acceptance by same participant in 3-way throws 409 ALREADY_RESPONDED
  test("29 - Repeated acceptance by already accepted participant in 3-way throws 409 ALREADY_RESPONDED", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      acceptedByC: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "ALREADY_RESPONDED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 30. Concurrent modification on commit for 3-way decision throws FIRESTORE_COMMIT_FAILED
  test("30 - Concurrent modification on 3-way decision commit throws FIRESTORE_COMMIT_FAILED", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ error: "Precondition failed" }), { status: 409 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "FIRESTORE_COMMIT_FAILED");
        assert.equal(err.statusCode, 409);
        return true;
      }
    );
  });

  // 31. Expired 3-way match cannot be accepted, throws 410 EXPIRED
  test("31 - Decision on expired 3-way match throws 410 EXPIRED", async () => {
    const expiredTime = new Date(Date.now() - 3600 * 1000).toISOString();
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      expiresAt: expiredTime
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-c", "match-3w-1", "ACCEPT", client);
      },
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.code, "EXPIRED");
        assert.equal(err.statusCode, 410);
        return true;
      }
    );
  });

  // 32. Rejection by C when another has already accepted cancels the match
  test("32 - Rejection by C cancels 3-way match even when A and B previously accepted", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-3w-1", "nurse-a", "nurse-b", "nurse-c", {
      acceptedByA: true,
      acceptedByB: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3w-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3w-1", "REJECT", client);

    assert.equal(result.matchId, "match-3w-1");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);
  });

  // 33. 2-way rejection releases both participants
  test("33 - 2-way rejection releases both participants when currentMatchId matches", async () => {
    const matchDoc = createMockMatchDoc("match-release-2w", "nurse-a", "nurse-b");
    const nurseADoc = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        currentHospitalId: { stringValue: "HOSP-001" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-002" }] } },
        grade: { stringValue: "Grade I" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-release-2w" }
      }
    };
    const nurseBDoc = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        currentHospitalId: { stringValue: "HOSP-002" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-001" }] } },
        grade: { stringValue: "Grade II" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-release-2w" }
      }
    };

    let committedWrites: any[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-release-2w`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(nurseADoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(nurseBDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        committedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-release-2w", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(committedWrites.length, 3, "Must include 1 match write and 2 participant release writes");
    // Verify match doc write
    assert.equal(committedWrites[0].update.fields.status.stringValue, "CANCELLED");
    // Verify nurse A release write
    assert.equal(committedWrites[1].update.fields.status.stringValue, "SEARCHING");
    assert.equal(committedWrites[1].update.fields.locked.booleanValue, false);
    assert.equal(committedWrites[1].update.fields.currentMatchId.nullValue, null);
    // Verify nurse B release write
    assert.equal(committedWrites[2].update.fields.status.stringValue, "SEARCHING");
    assert.equal(committedWrites[2].update.fields.locked.booleanValue, false);
    assert.equal(committedWrites[2].update.fields.currentMatchId.nullValue, null);
  });

  // 34. 3-way rejection releases all three participants
  test("34 - 3-way rejection releases all three participants", async () => {
    const matchDoc = createMockThreeWayMatchDoc("match-release-3w", "nurse-a", "nurse-b", "nurse-c");
    const createParticipant = (uid: string) => ({
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/${uid}`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: uid },
        currentHospitalId: { stringValue: "HOSP-001" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-002" }] } },
        grade: { stringValue: "Grade I" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-release-3w" }
      }
    });

    let committedWrites: any[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-release-3w`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(createParticipant("nurse-a")), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(createParticipant("nurse-b")), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`]: () =>
        new Response(JSON.stringify(createParticipant("nurse-c")), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        committedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-release-3w", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(committedWrites.length, 4, "Must include 1 match write and 3 participant release writes");
    for (let i = 1; i <= 3; i++) {
      assert.equal(committedWrites[i].update.fields.status.stringValue, "SEARCHING");
      assert.equal(committedWrites[i].update.fields.locked.booleanValue, false);
      assert.equal(committedWrites[i].update.fields.currentMatchId.nullValue, null);
    }
  });

  // 35. Stale release cannot unlock participant belonging to a newer match
  test("35 - Stale release cannot unlock participant belonging to a newer match", async () => {
    const matchDoc = createMockMatchDoc("match-stale-check", "nurse-a", "nurse-b");
    const nurseADoc = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        currentHospitalId: { stringValue: "HOSP-001" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-002" }] } },
        grade: { stringValue: "Grade I" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-stale-check" }
      }
    };
    const nurseBDocNewer = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        currentHospitalId: { stringValue: "HOSP-002" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-001" }] } },
        grade: { stringValue: "Grade I" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "brand-new-match-999" } // Pointing to a newer match!
      }
    };

    let committedWrites: any[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-stale-check`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(nurseADoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(nurseBDocNewer), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        committedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-stale-check", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    // Only match write and nurse A write should be included; nurse B must NOT be unlocked!
    assert.equal(committedWrites.length, 2, "Must NOT unlock nurse B who belongs to a newer match");
    assert.equal(committedWrites[1].update.name.includes("nurse-a"), true);
  });

  // 36. Race recovery when match doc was already CANCELLED concurrently
  test("36 - Race recovery returns CANCELLED when match is already cancelled by competing response", async () => {
    const pendingMatchDoc = createMockMatchDoc("match-race-1", "nurse-a", "nurse-b");
    const cancelledMatchDoc = createMockMatchDoc("match-race-1", "nurse-a", "nurse-b", {
      status: "CANCELLED",
      rejectedByB: true,
      updateTime: "2026-10-02T12:01:00.000Z"
    });

    let getMatchCalls = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-race-1`]: () => {
        getMatchCalls += 1;
        return new Response(JSON.stringify(getMatchCalls === 1 ? pendingMatchDoc : cancelledMatchDoc), { status: 200 });
      },
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify({ fields: { firebaseUid: { stringValue: "nurse-a" }, currentMatchId: { stringValue: "match-race-1" } } }), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify({ fields: { firebaseUid: { stringValue: "nurse-b" }, currentMatchId: { stringValue: "match-race-1" } } }), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () =>
        new Response(JSON.stringify({ error: { code: 409, message: "Conflict", status: "ABORTED" } }), { status: 409 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-race-1", "REJECT", client);

    assert.equal(result.matchId, "match-race-1");
    assert.equal(result.newStatus, "CANCELLED");
  });

  // 37. Rejection preserves cross-grade eligibility
  test("37 - Rejection preserves cross-grade participant eligibility", async () => {
    const matchDoc = createMockMatchDoc("match-crossgrade-rel", "nurse-a", "nurse-b");
    const nurseADoc = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        currentHospitalId: { stringValue: "HOSP-001" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-002" }] } },
        grade: { stringValue: "Grade I" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-crossgrade-rel" }
      }
    };
    const nurseBDoc = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        currentHospitalId: { stringValue: "HOSP-002" },
        preferenceHospitalIds: { arrayValue: { values: [{ stringValue: "HOSP-001" }] } },
        grade: { stringValue: "Grade II" }, // Different grade!
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-crossgrade-rel" }
      }
    };

    let committedWrites: any[] = [];
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-crossgrade-rel`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(nurseADoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(nurseBDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req: Request) => {
        const body = (await req.json()) as { writes: any[] };
        committedWrites = body.writes;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-crossgrade-rel", "REJECT", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(committedWrites.length, 3);
    // Grade II candidate is restored to SEARCHING without grade exclusion
    assert.equal(committedWrites[2].update.fields.status.stringValue, "SEARCHING");
    assert.equal(committedWrites[2].update.fields.locked.booleanValue, false);
  });
});

describe("Cloudflare Worker Endpoint: POST /api/matching/respond", () => {
  let keyPair: CryptoKeyPair;
  let validToken: string;

  test("Setup test RSA key pair and JWT", async () => {
    keyPair = await crypto.subtle.generateKey(
      {
        name: "RSASSA-PKCS1-v1_5",
        modulusLength: 2048,
        publicExponent: new Uint8Array([1, 0, 1]),
        hash: "SHA-256"
      },
      true,
      ["sign", "verify"]
    );

    const nowSec = Math.floor(Date.now() / 1000);
    validToken = await createSignedTestJwt(
      { alg: "RS256", kid: "test-key-id", typ: "JWT" },
      {
        iss: `https://securetoken.google.com/${TEST_PROJECT_ID}`,
        aud: TEST_PROJECT_ID,
        sub: "nurse-a",
        auth_time: nowSec,
        iat: nowSec,
        exp: nowSec + 3600
      },
      keyPair.privateKey
    );
  });

  test("12 - Worker returns 401 Unauthorized when Authorization header is missing", async () => {
    const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
    const req = new Request("http://localhost/api/matching/respond", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ matchId: "match-1", decision: "ACCEPT" })
    });

    const res = await worker.fetch(req, env);
    assert.equal(res.status, 401);
  });

  test("13 - Worker returns 405 MethodNotAllowed when using GET", async () => {
    const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
    const req = new Request("http://localhost/api/matching/respond", {
      method: "GET"
    });

    const res = await worker.fetch(req, env);
    assert.equal(res.status, 405);
  });

  test("14 - Worker returns 400 InvalidRequest when matchId or decision is missing", async () => {
    const exportedJwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
    const googleJwk: GoogleJwk = {
      kty: exportedJwk.kty || "RSA",
      alg: "RS256",
      use: "sig",
      kid: "test-key-id",
      n: exportedJwk.n || "",
      e: exportedJwk.e || ""
    };

    const originalFetch = globalThis.fetch;
    globalThis.fetch = async (url: string | URL | Request) => {
      const urlStr = url.toString();
      if (urlStr.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [googleJwk] }), { status: 200 });
      }
      return originalFetch(url);
    };

    try {
      const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
      const req = new Request("http://localhost/api/matching/respond", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ matchId: "match-1" }) // missing decision
      });

      const res = await worker.fetch(req, env);
      assert.equal(res.status, 400);
      const data = await res.json() as { error: string };
      assert.equal(data.error, "InvalidRequest");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  test("15 - Worker successfully applies decision via POST /api/matching/respond", async () => {
    const exportedJwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
    const googleJwk: GoogleJwk = {
      kty: exportedJwk.kty || "RSA",
      alg: "RS256",
      use: "sig",
      kid: "test-key-id",
      n: exportedJwk.n || "",
      e: exportedJwk.e || ""
    };

    const matchDoc = createMockMatchDoc("match-endpoint", "nurse-a", "nurse-b");

    const originalFetch = globalThis.fetch;
    globalThis.fetch = async (url: string | URL | Request, init?: RequestInit) => {
      const urlStr = url.toString();
      if (urlStr.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [googleJwk] }), { status: 200 });
      }
      if (urlStr.includes("/matches/match-endpoint")) {
        return new Response(JSON.stringify(matchDoc), { status: 200 });
      }
      if (urlStr.includes("/documents:commit")) {
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
      return originalFetch(url, init);
    };

    try {
      const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
      const req = new Request("http://localhost/api/matching/respond", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ matchId: "match-endpoint", decision: "ACCEPT" })
      });

      const res = await worker.fetch(req, env);
      assert.equal(res.status, 200);
      const data = await res.json() as { matchId: string; newStatus: string; decisionApplied: boolean };
      assert.equal(data.matchId, "match-endpoint");
      assert.equal(data.newStatus, "PENDING_CONFIRMATION");
      assert.equal(data.decisionApplied, true);
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  test("16 - Rejection throws PARTICIPANT_READ_FAILED when participant document read fails", async () => {
    const matchDoc = createMockMatchDoc("match-rej-fail", "nurse-a", "nurse-b");

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-rej-fail`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response("Internal Server Error", { status: 500 })
    });

    const client = new FirestoreClient({
      projectId: TEST_PROJECT_ID,
      tokenProvider: mockTokenProvider,
      transport
    });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-rej-fail", "REJECT", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 500);
        assert.equal(matchErr.code, "PARTICIPANT_READ_FAILED");
        return true;
      }
    );
  });
});

describe("Mutual Transfer Phase 2 Workflow State Machine Suite", () => {
  // 1. Sliding window triggers on first ACCEPT: sets firstResponseAt and tightens expiresAt
  test("PW-1: First ACCEPT triggers sliding window, records firstResponseAt, and tightens deadline to 24h", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const initialExpiresAt = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
    const matchDoc = createMockMatchDoc("match-sw-1", "nurse-a", "nurse-b", {
      expiresAt: initialExpiresAt
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-sw-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-sw-1", "ACCEPT", client);

    assert.equal(result.matchId, "match-sw-1");
    assert.equal(result.newStatus, "PENDING_CONFIRMATION");
    assert.ok(result.firstResponseAt, "firstResponseAt must be set on first ACCEPT");
    assert.ok(result.expiresAt, "expiresAt must be updated");
    // expiresAt must be earlier than the original 48h deadline (approx 24h from now)
    assert(new Date(result.expiresAt).getTime() < new Date(initialExpiresAt).getTime());

    assert.ok(capturedWrites);
    assert.ok(capturedWrites.writes[0].update.fields.firstResponseAt?.stringValue);
    assert.ok(capturedWrites.writes[0].update.fields.expiresAt?.stringValue);
  });

  // 2. Second ACCEPT does not overwrite firstResponseAt if already set
  test("PW-2: Subsequent ACCEPT does not overwrite existing firstResponseAt", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const existingFirstResponseAt = "2026-10-02T11:00:00.000Z";
    const matchDoc = createMockMatchDoc("match-sw-2", "nurse-a", "nurse-b", {
      acceptedByA: true
    });
    matchDoc.fields.firstResponseAt = { stringValue: existingFirstResponseAt };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-sw-2`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-sw-2", "ACCEPT", client);

    assert.equal(result.newStatus, "CHAT_OPEN");
    assert.equal(result.firstResponseAt, existingFirstResponseAt);
    assert.ok(result.chatDeadline);
  });

  // 3. Rejection during CHAT_OPEN transitions to CANCELLED and unlocks participants
  test("PW-3: Rejection during CHAT_OPEN transitions to CANCELLED and unlocks participants", async () => {
    let capturedWrites: { writes: Array<{ update: { name: string; fields: Record<string, { booleanValue?: boolean; stringValue?: string; nullValue?: null }> } }> } | null = null;
    const matchDoc = createMockMatchDoc("match-chat-rej", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });

    const docA = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-rej" }
      }
    };
    const docB = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-rej" }
      }
    };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-rej`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(docA), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(docB), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-chat-rej", "REJECT", client);

    assert.equal(result.matchId, "match-chat-rej");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes.length, 3, "Must write match doc + 2 participant unlock docs");
    // Match update
    assert.equal(capturedWrites.writes[0].update.fields.status?.stringValue, "CANCELLED");
    // Nurse A unlocked
    assert.equal(capturedWrites.writes[1].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[1].update.fields.status?.stringValue, "SEARCHING");
    // Nurse B unlocked
    assert.equal(capturedWrites.writes[2].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[2].update.fields.status?.stringValue, "SEARCHING");
  });

  // 4. Participant already confirmed in CHAT_OPEN throws ALREADY_CONFIRMED
  test("PW-4: Repeat confirmation in CHAT_OPEN throws 409 ALREADY_CONFIRMED", async () => {
    const matchDoc = createMockMatchDoc("match-chat-repeat", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: true };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-repeat`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-chat-repeat", "CONFIRM", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 409);
        assert.equal(matchErr.code, "ALREADY_CONFIRMED");
        return true;
      }
    );
  });

  // =========================================================================
  // Phase 1.5.4: Final Transfer Chat Decision State Machine (CONFIRM & LEAVE)
  // =========================================================================

  // 5. 2-way: A confirms (pending B confirmation, status remains CHAT_OPEN)
  test("PW-5: 2-way: Participant A confirms, match remains CHAT_OPEN awaiting Participant B", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockMatchDoc("match-chat-2way-1", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: false };
    matchDoc.fields.confirmedByB = { booleanValue: false };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-2way-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-chat-2way-1", "CONFIRM", client);

    assert.equal(result.matchId, "match-chat-2way-1");
    assert.equal(result.newStatus, "CHAT_OPEN");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.confirmedByA?.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status, undefined, "Status field must not change to CONFIRMED yet");
  });

  // 6. 2-way: B confirms after A confirmed -> transitions to CONFIRMED
  test("PW-6: 2-way: Participant B confirms after A confirmed, match transitions to CONFIRMED", async () => {
    let capturedWrites: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const matchDoc = createMockMatchDoc("match-chat-2way-2", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: true };
    matchDoc.fields.confirmedByB = { booleanValue: false };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-2way-2`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-chat-2way-2", "CONFIRM", client);

    assert.equal(result.matchId, "match-chat-2way-2");
    assert.equal(result.newStatus, "CONFIRMED");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.confirmedByB?.booleanValue, true);
    assert.equal(capturedWrites.writes[0].update.fields.status?.stringValue, "CONFIRMED");
  });

  // 7. 2-way: A leaves team in CHAT_OPEN -> transitions to CANCELLED and unlocks both
  test("PW-7: 2-way: Participant A LEAVE in CHAT_OPEN transitions to CANCELLED and unlocks both participants", async () => {
    let capturedWrites: { writes: Array<{ update: { name: string; fields: Record<string, { booleanValue?: boolean; stringValue?: string; nullValue?: null }> } }> } | null = null;
    const matchDoc = createMockMatchDoc("match-chat-2way-leave", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });

    const docA = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-2way-leave" }
      }
    };
    const docB = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-2way-leave" }
      }
    };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-2way-leave`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(docA), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(docB), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-a", "match-chat-2way-leave", "LEAVE", client);

    assert.equal(result.matchId, "match-chat-2way-leave");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes.length, 3);
    assert.equal(capturedWrites.writes[0].update.fields.status?.stringValue, "CANCELLED");
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByA?.booleanValue, true);
    assert.equal(capturedWrites.writes[1].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[1].update.fields.status?.stringValue, "SEARCHING");
    assert.equal(capturedWrites.writes[2].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[2].update.fields.status?.stringValue, "SEARCHING");
  });

  // 8. 2-way: B leaves team after A already confirmed -> cancels and unlocks both
  test("PW-8: 2-way: Participant B LEAVE after Participant A already confirmed cancels and unlocks both", async () => {
    let capturedWrites: { writes: Array<{ update: { name: string; fields: Record<string, { booleanValue?: boolean; stringValue?: string; nullValue?: null }> } }> } | null = null;
    const matchDoc = createMockMatchDoc("match-chat-2way-b-leave", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: true };
    matchDoc.fields.confirmedByB = { booleanValue: false };

    const docA = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-2way-b-leave" }
      }
    };
    const docB = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-chat-2way-b-leave" }
      }
    };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-2way-b-leave`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(docA), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(docB), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-chat-2way-b-leave", "LEAVE", client);

    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByB?.booleanValue, true);
    assert.equal(capturedWrites.writes[1].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[2].update.fields.locked?.booleanValue, false);
  });

  // 9. 3-way: A confirms -> remains CHAT_OPEN; B confirms -> remains CHAT_OPEN; C confirms -> CONFIRMED
  test("PW-9: 3-way: Sequential confirmation across A, B, and C transitions to CONFIRMED only when all 3 confirm", async () => {
    // Step 1: A confirms
    const matchDoc1 = createMockThreeWayMatchDoc("match-3way-seq", "nurse-a", "nurse-b", "nurse-c", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      acceptedByC: true
    });
    matchDoc1.fields.confirmedByA = { booleanValue: false };
    matchDoc1.fields.confirmedByB = { booleanValue: false };
    matchDoc1.fields.confirmedByC = { booleanValue: false };

    let capturedWrites1: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const transport1 = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3way-seq`]: () =>
        new Response(JSON.stringify(matchDoc1), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites1 = await req.json() as typeof capturedWrites1;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });
    const client1 = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport: transport1 });
    const resA = await respondToMatch("nurse-a", "match-3way-seq", "CONFIRM", client1);
    assert.equal(resA.newStatus, "CHAT_OPEN");
    assert.equal(capturedWrites1?.writes[0].update.fields.confirmedByA?.booleanValue, true);

    // Step 2: B confirms (A already confirmed)
    const matchDoc2 = createMockThreeWayMatchDoc("match-3way-seq", "nurse-a", "nurse-b", "nurse-c", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      acceptedByC: true
    });
    matchDoc2.fields.confirmedByA = { booleanValue: true };
    matchDoc2.fields.confirmedByB = { booleanValue: false };
    matchDoc2.fields.confirmedByC = { booleanValue: false };

    let capturedWrites2: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const transport2 = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3way-seq`]: () =>
        new Response(JSON.stringify(matchDoc2), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites2 = await req.json() as typeof capturedWrites2;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });
    const client2 = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport: transport2 });
    const resB = await respondToMatch("nurse-b", "match-3way-seq", "CONFIRM", client2);
    assert.equal(resB.newStatus, "CHAT_OPEN");
    assert.equal(capturedWrites2?.writes[0].update.fields.confirmedByB?.booleanValue, true);

    // Step 3: C confirms (A and B already confirmed)
    const matchDoc3 = createMockThreeWayMatchDoc("match-3way-seq", "nurse-a", "nurse-b", "nurse-c", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      acceptedByC: true
    });
    matchDoc3.fields.confirmedByA = { booleanValue: true };
    matchDoc3.fields.confirmedByB = { booleanValue: true };
    matchDoc3.fields.confirmedByC = { booleanValue: false };

    let capturedWrites3: { writes: Array<{ update: { fields: Record<string, { booleanValue?: boolean; stringValue?: string }> } }> } | null = null;
    const transport3 = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3way-seq`]: () =>
        new Response(JSON.stringify(matchDoc3), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites3 = await req.json() as typeof capturedWrites3;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });
    const client3 = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport: transport3 });
    const resC = await respondToMatch("nurse-c", "match-3way-seq", "CONFIRM", client3);
    assert.equal(resC.newStatus, "CONFIRMED");
    assert.equal(capturedWrites3?.writes[0].update.fields.confirmedByC?.booleanValue, true);
    assert.equal(capturedWrites3?.writes[0].update.fields.status?.stringValue, "CONFIRMED");
  });

  // 10. 3-way: A and B confirmed, C leaves team -> cancels match and unlocks A, B, and C
  test("PW-10: 3-way: Participant C LEAVE after A and B confirmed cancels match and atomically unlocks all 3", async () => {
    let capturedWrites: { writes: Array<{ update: { name: string; fields: Record<string, { booleanValue?: boolean; stringValue?: string; nullValue?: null }> } }> } | null = null;
    const matchDoc = createMockThreeWayMatchDoc("match-3way-leave", "nurse-a", "nurse-b", "nurse-c", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      acceptedByC: true
    });
    matchDoc.fields.confirmedByA = { booleanValue: true };
    matchDoc.fields.confirmedByB = { booleanValue: true };
    matchDoc.fields.confirmedByC = { booleanValue: false };

    const docA = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-a" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-3way-leave" }
      }
    };
    const docB = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-b" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-3way-leave" }
      }
    };
    const docC = {
      name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`,
      updateTime: "2026-10-02T12:00:00.000Z",
      fields: {
        firebaseUid: { stringValue: "nurse-c" },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-3way-leave" }
      }
    };

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3way-leave`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(docA), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(docB), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`]: () =>
        new Response(JSON.stringify(docC), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedWrites = await req.json() as typeof capturedWrites;
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:30:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-c", "match-3way-leave", "LEAVE", client);

    assert.equal(result.matchId, "match-3way-leave");
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);

    assert.ok(capturedWrites);
    assert.equal(capturedWrites.writes.length, 4, "Must write match doc + 3 participant unlock docs");
    assert.equal(capturedWrites.writes[0].update.fields.status?.stringValue, "CANCELLED");
    assert.equal(capturedWrites.writes[0].update.fields.rejectedByC?.booleanValue, true);
    // All three participants unlocked
    assert.equal(capturedWrites.writes[1].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[1].update.fields.status?.stringValue, "SEARCHING");
    assert.equal(capturedWrites.writes[2].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[2].update.fields.status?.stringValue, "SEARCHING");
    assert.equal(capturedWrites.writes[3].update.fields.locked?.booleanValue, false);
    assert.equal(capturedWrites.writes[3].update.fields.status?.stringValue, "SEARCHING");
  });

  // 11. LEAVE in PENDING_CONFIRMATION returns 400 INVALID_DECISION
  test("PW-11: Attempting LEAVE in PENDING_CONFIRMATION throws 400 INVALID_DECISION", async () => {
    const matchDoc = createMockMatchDoc("match-pending-leave", "nurse-a", "nurse-b", {
      status: "PENDING_CONFIRMATION"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-pending-leave`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-pending-leave", "LEAVE", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 400);
        assert.equal(matchErr.code, "INVALID_DECISION");
        return true;
      }
    );
  });

  // 12. Decision on already CONFIRMED match returns 409 INVALID_STATUS
  test("PW-12: Decision on already CONFIRMED match throws 409 INVALID_STATUS", async () => {
    const matchDoc = createMockMatchDoc("match-already-confirmed", "nurse-a", "nurse-b", {
      status: "CONFIRMED"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-already-confirmed`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-already-confirmed", "CONFIRM", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 409);
        assert.equal(matchErr.code, "INVALID_STATUS");
        return true;
      }
    );
  });

  // 13. Decision on already CANCELLED match returns idempotent CANCELLED for LEAVE/REJECT, 409 for CONFIRM
  test("PW-13: LEAVE on already CANCELLED match is idempotent and returns CANCELLED", async () => {
    const matchDoc = createMockMatchDoc("match-already-cancelled", "nurse-a", "nurse-b", {
      status: "CANCELLED"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-already-cancelled`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    const result = await respondToMatch("nurse-a", "match-already-cancelled", "LEAVE", client);
    assert.equal(result.newStatus, "CANCELLED");
    assert.equal(result.decisionApplied, true);

    // CONFIRM on CANCELLED match still throws 409 INVALID_STATUS
    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-already-cancelled", "CONFIRM", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 409);
        assert.equal(matchErr.code, "INVALID_STATUS");
        return true;
      }
    );
  });

  // 14. Decision on expired match in CHAT_OPEN throws 410 EXPIRED
  test("PW-14: Decision on expired match in CHAT_OPEN throws 410 EXPIRED", async () => {
    const pastDate = new Date(Date.now() - 3600 * 1000).toISOString();
    const matchDoc = createMockMatchDoc("match-chat-expired", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      expiresAt: pastDate
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-expired`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => {
        await respondToMatch("nurse-a", "match-chat-expired", "CONFIRM", client);
      },
      (err: Error) => {
        const matchErr = err as MatchServiceError;
        assert.equal(matchErr.statusCode, 410);
        assert.equal(matchErr.code, "EXPIRED");
        return true;
      }
    );
  });

  // 15. Concurrent LEAVE / CONFIRM conflict recovers cleanly
  test("PW-15: Concurrency retry recovers cleanly when competing caller already confirmed", async () => {
    const matchDocBefore = createMockMatchDoc("match-chat-race", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true,
      updateTime: "2026-10-02T12:00:00.000Z"
    });
    matchDocBefore.fields.confirmedByA = { booleanValue: true };
    matchDocBefore.fields.confirmedByB = { booleanValue: false };

    const matchDocAfter = createMockMatchDoc("match-chat-race", "nurse-a", "nurse-b", {
      status: "CONFIRMED",
      acceptedByA: true,
      acceptedByB: true,
      updateTime: "2026-10-02T12:00:01.000Z"
    });
    matchDocAfter.fields.confirmedByA = { booleanValue: true };
    matchDocAfter.fields.confirmedByB = { booleanValue: true };

    let getCount = 0;
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-race`]: () => {
        getCount++;
        return new Response(JSON.stringify(getCount === 1 ? matchDocBefore : matchDocAfter), { status: 200 });
      },
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: () => {
        return new Response(
          JSON.stringify({ error: { code: 409, message: "Precondition failed: updateTime mismatch", status: "ABORTED" } }),
          { status: 409 }
        );
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await respondToMatch("nurse-b", "match-chat-race", "CONFIRM", client);

    assert.equal(result.matchId, "match-chat-race");
    assert.equal(result.newStatus, "CONFIRMED");
    assert.equal(result.decisionApplied, true);
  });
});
