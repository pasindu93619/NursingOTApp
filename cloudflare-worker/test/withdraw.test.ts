import test, { describe, before } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import {
  withdrawTransferRequest,
  MatchServiceError
} from "../src/matching/matchingService.ts";
import {
  FirestoreClient,
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
  } = {}
): FirestoreRawDocument {
  const expiresAt = options.expiresAt ?? new Date(Date.now() + 48 * 3600 * 1000).toISOString();
  return {
    name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/${matchId}`,
    updateTime: options.updateTime ?? "2026-10-02T12:00:00.000Z",
    fields: {
      nurseAUid: { stringValue: nurseAUid },
      nurseBUid: { stringValue: nurseBUid },
      nurseCUid: { stringValue: nurseCUid },
      nurseACurrentHospitalId: { stringValue: "HOSP-001" },
      nurseBCurrentHospitalId: { stringValue: "HOSP-002" },
      nurseCCurrentHospitalId: { stringValue: "HOSP-003" },
      status: { stringValue: options.status ?? "PENDING_CONFIRMATION" },
      acceptedByA: { booleanValue: options.acceptedByA ?? false },
      acceptedByB: { booleanValue: options.acceptedByB ?? false },
      acceptedByC: { booleanValue: options.acceptedByC ?? false },
      rejectedByA: { booleanValue: options.rejectedByA ?? false },
      rejectedByB: { booleanValue: options.rejectedByB ?? false },
      rejectedByC: { booleanValue: options.rejectedByC ?? false },
      expiresAt: { stringValue: expiresAt },
      createdAt: { stringValue: "2026-10-02T10:00:00.000Z" },
      updatedAt: { stringValue: options.updateTime ?? "2026-10-02T12:00:00.000Z" }
    }
  };
}

describe("Transfer Request Authoritative Withdrawal Suite", () => {
  let keyPair: CryptoKeyPair;
  let validToken: string;
  let googleJwk: GoogleJwk;

  before(async () => {
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

    const exportedJwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
    googleJwk = {
      kty: exportedJwk.kty || "RSA",
      alg: "RS256",
      use: "sig",
      kid: "test-key-id",
      n: exportedJwk.n || "",
      e: exportedJwk.e || ""
    };

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

  // 1. Unmatched request withdrawal deletes request document directly
  test("1 - Unmatched request withdrawal directly deletes request document", async () => {
    let capturedCommitBody: { writes: Array<{ delete?: string }> } | null = null;
    const reqDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "SEARCHING",
      locked: false,
      currentMatchId: null
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(reqDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedCommitBody = await req.json();
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-a", client);

    assert.equal(result.withdrawn, true);
    assert.equal(result.matchCancelled, false);
    assert.equal(result.cancelledMatchId, undefined);
    assert(capturedCommitBody !== null, "Commit call must have been issued");
    const writes = capturedCommitBody.writes;
    assert.equal(writes.length, 1);
    assert(writes[0].delete?.endsWith("transferRequests/nurse-a"));
  });

  // 2. Request document already missing returns success idempotently
  test("2 - Request document already absent returns withdrawn=true idempotently", async () => {
    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-absent`]: () =>
        new Response("Not found", { status: 404 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-absent", client);

    assert.equal(result.withdrawn, true);
    assert.equal(result.matchCancelled, false);
  });

  // 3. Matched request withdrawal in PENDING_CONFIRMATION cancels match, unlocks partner, deletes caller
  test("3 - Matched request withdrawal in PENDING_CONFIRMATION cancels match and unlocks partner", async () => {
    let capturedCommitBody: { writes: Array<{ update?: { name: string; fields: Record<string, unknown> }; delete?: string }> } | null = null;

    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "PENDING_CONFIRMATION",
      locked: true,
      currentMatchId: "match-1"
    });
    const partnerDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade II", {
      status: "PENDING_CONFIRMATION",
      locked: true,
      currentMatchId: "match-1"
    });
    const matchDoc = createMockMatchDoc("match-1", "nurse-a", "nurse-b", {
      status: "PENDING_CONFIRMATION"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-1`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(partnerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedCommitBody = await req.json();
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-a", client);

    assert.equal(result.withdrawn, true);
    assert.equal(result.matchCancelled, true);
    assert.equal(result.cancelledMatchId, "match-1");

    assert(capturedCommitBody !== null);
    const writes = capturedCommitBody.writes;
    assert.equal(writes.length, 3, "Must perform 3 writes: update match, unlock partner, delete caller");

    // Verify match update
    const matchWrite = writes.find(w => w.update?.name.endsWith("matches/match-1"));
    assert(matchWrite !== undefined, "Match must be updated");
    assert.equal((matchWrite.update!.fields.status as { stringValue: string }).stringValue, "CANCELLED");
    assert.equal((matchWrite.update!.fields.rejectedByA as { booleanValue: boolean }).booleanValue, true);

    // Verify partner unlock
    const partnerWrite = writes.find(w => w.update?.name.endsWith("transferRequests/nurse-b"));
    assert(partnerWrite !== undefined, "Partner must be updated");
    assert.equal((partnerWrite.update!.fields.status as { stringValue: string }).stringValue, "SEARCHING");
    assert.equal((partnerWrite.update!.fields.locked as { booleanValue: boolean }).booleanValue, false);
    assert.equal((partnerWrite.update!.fields.currentMatchId as { nullValue: null }).nullValue, null);

    // Verify caller delete
    const callerDeleteWrite = writes.find(w => w.delete?.endsWith("transferRequests/nurse-a"));
    assert(callerDeleteWrite !== undefined, "Caller request must be deleted");
  });

  // 4. Matched request withdrawal in CHAT_OPEN cancels active team, unlocks partner, deletes caller
  test("4 - Matched request withdrawal in CHAT_OPEN cancels active team and deletes caller", async () => {
    let capturedCommitBody: { writes: Array<{ update?: { name: string; fields: Record<string, unknown> }; delete?: string }> } | null = null;

    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "MATCH_ACCEPTED",
      locked: true,
      currentMatchId: "match-chat-open"
    });
    const partnerDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-001"], "Grade II", {
      status: "MATCH_ACCEPTED",
      locked: true,
      currentMatchId: "match-chat-open"
    });
    const matchDoc = createMockMatchDoc("match-chat-open", "nurse-a", "nurse-b", {
      status: "CHAT_OPEN",
      acceptedByA: true,
      acceptedByB: true
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-chat-open`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(partnerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedCommitBody = await req.json();
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-a", client);

    assert.equal(result.withdrawn, true);
    assert.equal(result.matchCancelled, true);
    assert.equal(result.cancelledMatchId, "match-chat-open");

    assert(capturedCommitBody !== null);
    const writes = capturedCommitBody.writes;
    assert.equal(writes.length, 3);
    const matchWrite = writes.find(w => w.update?.name.endsWith("matches/match-chat-open"));
    assert.equal((matchWrite!.update!.fields.status as { stringValue: string }).stringValue, "CANCELLED");
    const callerDeleteWrite = writes.find(w => w.delete?.endsWith("transferRequests/nurse-a"));
    assert(callerDeleteWrite !== undefined);
  });

  // 5. 3-Way match withdrawal cancels match, unlocks both B and C, deletes caller
  test("5 - 3-Way matched request withdrawal unlocks both partners and deletes caller", async () => {
    let capturedCommitBody: { writes: Array<{ update?: { name: string; fields: Record<string, unknown> }; delete?: string }> } | null = null;

    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "PENDING_CONFIRMATION",
      locked: true,
      currentMatchId: "match-3way"
    });
    const partnerBDoc = createMockRawDoc("nurse-b", "HOSP-002", ["HOSP-003"], "Grade II", {
      status: "PENDING_CONFIRMATION",
      locked: true,
      currentMatchId: "match-3way"
    });
    const partnerCDoc = createMockRawDoc("nurse-c", "HOSP-003", ["HOSP-001"], "Grade II", {
      status: "PENDING_CONFIRMATION",
      locked: true,
      currentMatchId: "match-3way"
    });
    const matchDoc = createMockThreeWayMatchDoc("match-3way", "nurse-a", "nurse-b", "nurse-c", {
      status: "PENDING_CONFIRMATION"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-3way`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-b`]: () =>
        new Response(JSON.stringify(partnerBDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-c`]: () =>
        new Response(JSON.stringify(partnerCDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async (req) => {
        capturedCommitBody = await req.json();
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-a", client);

    assert.equal(result.withdrawn, true);
    assert.equal(result.matchCancelled, true);
    assert.equal(result.cancelledMatchId, "match-3way");

    assert(capturedCommitBody !== null);
    const writes = capturedCommitBody.writes;
    assert.equal(writes.length, 4, "Must write match update, 2 partner unlocks, and 1 caller delete");

    const partnerBWrite = writes.find(w => w.update?.name.endsWith("transferRequests/nurse-b"));
    assert(partnerBWrite !== undefined);
    assert.equal((partnerBWrite.update!.fields.locked as { booleanValue: boolean }).booleanValue, false);

    const partnerCWrite = writes.find(w => w.update?.name.endsWith("transferRequests/nurse-c"));
    assert(partnerCWrite !== undefined);
    assert.equal((partnerCWrite.update!.fields.locked as { booleanValue: boolean }).booleanValue, false);

    const callerDeleteWrite = writes.find(w => w.delete?.endsWith("transferRequests/nurse-a"));
    assert(callerDeleteWrite !== undefined);
  });

  // 6. Withdrawal when match is CONFIRMED throws 409 MATCH_ALREADY_CONFIRMED
  test("6 - Withdrawal when match is CONFIRMED throws 409 MATCH_ALREADY_CONFIRMED", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "CONFIRMED",
      locked: true,
      currentMatchId: "match-conf"
    });
    const matchDoc = createMockMatchDoc("match-conf", "nurse-a", "nurse-b", {
      status: "CONFIRMED"
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-conf`]: () =>
        new Response(JSON.stringify(matchDoc), { status: 200 })
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });

    await assert.rejects(
      async () => withdrawTransferRequest("nurse-a", client),
      (err: unknown) => {
        assert(err instanceof MatchServiceError);
        assert.equal(err.statusCode, 409);
        assert.equal(err.code, "MATCH_ALREADY_CONFIRMED");
        return true;
      }
    );
  });

  // 7. Concurrency conflict retry: commit failure retries and succeeds
  test("7 - Concurrency commit conflict retries and succeeds", async () => {
    let commitAttempts = 0;
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "SEARCHING",
      locked: false,
      currentMatchId: null
    });

    const transport = createMockTransport({
      [`GET /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents/transferRequests/nurse-a`]: () =>
        new Response(JSON.stringify(callerDoc), { status: 200 }),
      [`POST /v1/projects/${TEST_PROJECT_ID}/databases/(default)/documents:commit`]: async () => {
        commitAttempts++;
        if (commitAttempts === 1) {
          return new Response(JSON.stringify({ error: { code: 409, message: "Aborted" } }), { status: 409 });
        }
        return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
      }
    });

    const client = new FirestoreClient({ projectId: TEST_PROJECT_ID, tokenProvider: mockTokenProvider, transport });
    const result = await withdrawTransferRequest("nurse-a", client, 1);

    assert.equal(result.withdrawn, true);
    assert.equal(commitAttempts, 2, "Must have retried on 409");
  });

  // 8. HTTP Endpoint POST /api/matching/withdraw requires Bearer token
  test("8 - POST /api/matching/withdraw rejects requests missing Bearer token with 401", async () => {
    const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
    const request = new Request("http://localhost/api/matching/withdraw", {
      method: "POST"
    });

    const response = await worker.fetch(request, env);

    assert.equal(response.status, 401);
    const body = (await response.json()) as { error: string };
    assert.equal(body.error, "Unauthorized");
  });

  // 9. HTTP Endpoint POST /api/matching/withdraw processes valid request end-to-end
  test("9 - POST /api/matching/withdraw succeeds with valid JWT", async () => {
    const callerDoc = createMockRawDoc("nurse-a", "HOSP-001", ["HOSP-002"], "Grade II", {
      status: "SEARCHING",
      locked: false,
      currentMatchId: null
    });

    const originalFetch = globalThis.fetch;
    try {
      globalThis.fetch = async (url: string | URL | Request, init?: RequestInit) => {
        const urlStr = url.toString();
        if (urlStr.includes("service_accounts/v1/jwk")) {
          return new Response(JSON.stringify({ keys: [googleJwk] }), { status: 200 });
        }
        if (urlStr.includes("/oauth2/v4/token")) {
          return new Response(JSON.stringify({ access_token: MOCK_TOKEN, expires_in: 3600 }), { status: 200 });
        }
        if (urlStr.includes("/transferRequests/nurse-a")) {
          return new Response(JSON.stringify(callerDoc), { status: 200 });
        }
        if (urlStr.includes("/documents:commit")) {
          return new Response(JSON.stringify({ commitTime: "2026-10-02T12:00:00Z" }), { status: 200 });
        }
        return originalFetch(url, init);
      };

      const env: Env = {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        FIREBASE_SERVICE_ACCOUNT_KEY: JSON.stringify({
          client_email: "test@app.iam.gserviceaccount.com",
          private_key: "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...\n-----END PRIVATE KEY-----\n"
        })
      };

      // Mock getServiceAccountToken directly in token provider or environment
      const request = new Request("http://localhost/api/matching/withdraw", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`
        }
      });

      const response = await worker.fetch(request, env);
      assert.equal(response.status, 200);
      const body = (await response.json()) as { withdrawn: boolean; matchCancelled: boolean };
      assert.equal(body.withdrawn, true);
      assert.equal(body.matchCancelled, false);
    } finally {
      globalThis.fetch = originalFetch;
    }
  });
});
