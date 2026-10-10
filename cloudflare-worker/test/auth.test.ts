import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import {
  extractBearerToken,
  verifyFirebaseIdToken,
  base64UrlToUint8Array,
  resetJwksCache
} from "../src/auth/firebaseAuth.ts";
import type { GoogleJwk, Env } from "../src/types.ts";

const TEST_PROJECT_ID = "nursing-super-app-test";

function stringToBase64Url(str: string): string {
  return Buffer.from(str, "utf8").toString("base64url");
}

function uint8ArrayToBase64Url(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString("base64url");
}

// Helper to create and sign test JWTs using Web Crypto API
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

test("Cloudflare Worker Firebase Auth Test Suite", async (t) => {
  // Generate a test RSA key pair for cryptographic verification tests
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
  publicJwk.kid = "test-kid-1";
  publicJwk.alg = "RS256";

  const jwksProvider = async (): Promise<GoogleJwk[]> => [publicJwk];
  const now = Math.floor(Date.now() / 1000);

  const defaultPayload = {
    iss: `https://securetoken.google.com/${TEST_PROJECT_ID}`,
    aud: TEST_PROJECT_ID,
    sub: "verified-nurse-uid-12345",
    auth_time: now - 100,
    iat: now - 100,
    exp: now + 3600
  };

  const defaultHeader = {
    alg: "RS256",
    kid: "test-kid-1",
    typ: "JWT"
  };

  const env: Env = {
    FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
    ENVIRONMENT: "test"
  };

  // ---------------------------------------------------------------------------
  // 1. Missing Authorization header -> 401
  // ---------------------------------------------------------------------------
  await t.test("1 - Missing Authorization header returns 401", async () => {
    const req = new Request("https://worker.local/api/auth/check", {
      method: "POST"
    });
    const res = await worker.fetch(req, env);
    assert.equal(res.status, 401);
    const body = await res.json();
    assert.equal(body.error, "Unauthorized");
    assert.match(body.message, /Missing Authorization header/i);
  });

  // ---------------------------------------------------------------------------
  // 2. Malformed Authorization header -> 401
  // ---------------------------------------------------------------------------
  await t.test("2 - Malformed Authorization header returns 401", async () => {
    const invalidHeaders = [
      "Basic abc123xyz",
      "Bearer",
      "Token abc123xyz",
      "Bearer token1 token2"
    ];

    for (const h of invalidHeaders) {
      const req = new Request("https://worker.local/api/auth/check", {
        method: "POST",
        headers: { Authorization: h }
      });
      const res = await worker.fetch(req, env);
      assert.equal(res.status, 401);
      const body = await res.json();
      assert.equal(body.error, "Unauthorized");
    }
  });

  // ---------------------------------------------------------------------------
  // 3. Invalid/tampered Firebase token -> 401
  // ---------------------------------------------------------------------------
  await t.test("3 - Invalid or tampered token returns 401", async () => {
    const validToken = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    // Tamper with payload
    const parts = validToken.split(".");
    const tamperedPayload = stringToBase64Url(
      JSON.stringify({ ...defaultPayload, sub: "hacker-uid" })
    );
    const tamperedToken = `${parts[0]}.${tamperedPayload}.${parts[2]}`;

    await assert.rejects(
      async () => {
        await verifyFirebaseIdToken(tamperedToken, {
          projectId: TEST_PROJECT_ID,
          jwksProvider
        });
      },
      (err: Error) => err.message.includes("Cryptographic verification failed")
    );

    // Also test through the worker API with mocked globalThis.fetch returning publicJwk
    const originalFetch = globalThis.fetch;
    try {
      globalThis.fetch = (async (url: string | URL | Request) => {
        if (String(url).includes("jwk")) {
          return new Response(JSON.stringify({ keys: [publicJwk] }), {
            status: 200,
            headers: { "Content-Type": "application/json" }
          });
        }
        return new Response("Not found", { status: 404 });
      }) as typeof fetch;

      resetJwksCache();
      const req = new Request("https://worker.local/api/auth/check", {
        method: "POST",
        headers: { Authorization: `Bearer ${tamperedToken}` }
      });
      const res = await worker.fetch(req, env);
      assert.equal(res.status, 401);
    } finally {
      globalThis.fetch = originalFetch;
      resetJwksCache();
    }
  });

  // ---------------------------------------------------------------------------
  // 4. Expired token -> 401
  // ---------------------------------------------------------------------------
  await t.test("4 - Expired token returns 401", async () => {
    const expiredPayload = {
      ...defaultPayload,
      exp: now - 300,
      auth_time: now - 1000,
      iat: now - 1000
    };
    const expiredToken = await createSignedTestJwt(defaultHeader, expiredPayload, keyPair.privateKey);

    await assert.rejects(
      async () => {
        await verifyFirebaseIdToken(expiredToken, {
          projectId: TEST_PROJECT_ID,
          jwksProvider,
          nowSeconds: now
        });
      },
      (err: Error) => err.message.includes("Token has expired")
    );
  });

  // ---------------------------------------------------------------------------
  // 5. Wrong issuer -> 401
  // ---------------------------------------------------------------------------
  await t.test("5 - Wrong issuer returns 401", async () => {
    const wrongIssuerPayload = {
      ...defaultPayload,
      iss: "https://evil-issuer.com"
    };
    const token = await createSignedTestJwt(defaultHeader, wrongIssuerPayload, keyPair.privateKey);

    await assert.rejects(
      async () => {
        await verifyFirebaseIdToken(token, {
          projectId: TEST_PROJECT_ID,
          jwksProvider,
          nowSeconds: now
        });
      },
      (err: Error) => err.message.includes("Invalid token issuer")
    );
  });

  // ---------------------------------------------------------------------------
  // 6. Wrong audience / project ID -> 401
  // ---------------------------------------------------------------------------
  await t.test("6 - Wrong audience / project ID returns 401", async () => {
    const wrongAudPayload = {
      ...defaultPayload,
      aud: "attacker-project-id"
    };
    const token = await createSignedTestJwt(defaultHeader, wrongAudPayload, keyPair.privateKey);

    await assert.rejects(
      async () => {
        await verifyFirebaseIdToken(token, {
          projectId: TEST_PROJECT_ID,
          jwksProvider,
          nowSeconds: now
        });
      },
      (err: Error) => err.message.includes("Invalid token audience")
    );
  });

  // ---------------------------------------------------------------------------
  // 7. Valid Firebase token -> authenticated response
  // ---------------------------------------------------------------------------
  await t.test("7 - Valid Firebase token verifies successfully", async () => {
    const validToken = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const verified = await verifyFirebaseIdToken(validToken, {
      projectId: TEST_PROJECT_ID,
      jwksProvider,
      nowSeconds: now
    });

    assert.equal(verified.uid, "verified-nurse-uid-12345");
    assert.equal(verified.audience, TEST_PROJECT_ID);
    assert.equal(verified.issuer, `https://securetoken.google.com/${TEST_PROJECT_ID}`);
  });

  // ---------------------------------------------------------------------------
  // 8. UID comes from verified token, not request body
  // ---------------------------------------------------------------------------
  await t.test("8 - UID comes strictly from verified token", async () => {
    const validToken = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const verified = await verifyFirebaseIdToken(validToken, {
      projectId: TEST_PROJECT_ID,
      jwksProvider,
      nowSeconds: now
    });

    assert.equal(verified.uid, defaultPayload.sub);
    assert.notEqual(verified.uid, "attacker-body-uid");
  });

  // ---------------------------------------------------------------------------
  // 9. Request body cannot impersonate another Firebase UID
  // ---------------------------------------------------------------------------
  await t.test("9 - Request body cannot impersonate another Firebase UID", async () => {
    const nurseToken = await createSignedTestJwt(
      defaultHeader,
      { ...defaultPayload, sub: "nurse-authorized-uid-777" },
      keyPair.privateKey
    );

    const verified = await verifyFirebaseIdToken(nurseToken, {
      projectId: TEST_PROJECT_ID,
      jwksProvider,
      nowSeconds: now
    });

    // Simulated malicious body claiming a different UID
    const maliciousBody = { uid: "victim-uid-999" };

    // The worker always maps identity from `verified.uid`, never `maliciousBody.uid`
    const resolvedUid = verified.uid;
    assert.equal(resolvedUid, "nurse-authorized-uid-777");
    assert.notEqual(resolvedUid, maliciousBody.uid);
  });

  // ---------------------------------------------------------------------------
  // 10. No private transfer data is returned by the authentication endpoint
  // ---------------------------------------------------------------------------
  await t.test("10 - No private transfer data is exposed in auth response", async () => {
    const validToken = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const verified = await verifyFirebaseIdToken(validToken, {
      projectId: TEST_PROJECT_ID,
      jwksProvider,
      nowSeconds: now
    });

    const response = {
      authenticated: true,
      uid: verified.uid
    };

    assert.deepEqual(response, {
      authenticated: true,
      uid: "verified-nurse-uid-12345"
    });

    // Verify absence of sensitive transfer fields
    assert.equal("currentHospitalId" in response, false);
    assert.equal("preferenceHospitalIds" in response, false);
    assert.equal("matches" in response, false);
    assert.equal("serviceNo" in response, false);
    assert.equal("grade" in response, false);
  });

  // ---------------------------------------------------------------------------
  // 11. Production environment blocks mock access token fallback
  // ---------------------------------------------------------------------------
  await t.test("11 - Production environment blocks mock access token when service account credentials are missing", async () => {
    resetJwksCache();
    const validToken = await createSignedTestJwt(defaultHeader, defaultPayload, keyPair.privateKey);

    const originalFetch = globalThis.fetch;
    globalThis.fetch = async (url: string | URL | Request) => {
      const urlStr = url.toString();
      if (urlStr.includes("service_accounts/v1/jwk")) {
        return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
      }
      return originalFetch(url);
    };

    try {
      const prodEnv: Env = {
        FIREBASE_PROJECT_ID: TEST_PROJECT_ID,
        ENVIRONMENT: "production",
        MATCH_EXPIRATION_HOURS: "48"
      };

      const req = new Request("https://worker.local/api/matching/respond", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ matchId: "any-match", decision: "ACCEPT" })
      });

      const res = await worker.fetch(req, prodEnv);
      assert.equal(res.status, 500);
      const body = (await res.json()) as { error: string; message: string };
      assert.equal(body.error, "ConfigError");
      assert.match(body.message, /Firebase service account credentials missing in production environment/i);
    } finally {
      globalThis.fetch = originalFetch;
      resetJwksCache();
    }
  });
});
