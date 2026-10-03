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

  // 3. Second ACCEPT confirms the match
  test("3 - Second ACCEPT when opponent has already accepted returns CONFIRMED", async () => {
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
    assert.equal(result.newStatus, "CONFIRMED");
    assert.equal(result.decisionApplied, true);
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
});
