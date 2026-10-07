import test, { describe, before } from "node:test";
import assert from "node:assert/strict";
import {
  createServiceAccountJwtAssertion,
  exchangeJwtForAccessToken,
  createServiceAccountTokenProvider,
  importPemPrivateKey,
  pemToPkcs8Der,
  ServiceAccountAuthError,
  OAUTH_JWT_GRANT_TYPE,
  DEFAULT_DATASTORE_SCOPE
} from "../src/auth/serviceAccountAuth.ts";
import type { HttpTransport } from "../src/firestore/firestoreClient.ts";

const FAKE_CLIENT_EMAIL = "nursing-transfer-sa@nursing-super-app-test.iam.gserviceaccount.com";
const FAKE_TOKEN_ENDPOINT = "https://oauth.example.invalid/token";

function createMockTransport(
  routes: Record<string, (req: Request) => Promise<Response> | Response>
): HttpTransport {
  return async (input, init) => {
    const req = input instanceof Request ? input : new Request(input, init);
    const url = new URL(req.url);
    const key = `${req.method} ${url.origin}${url.pathname}`;
    const handler = routes[key] || routes[`${req.method} ${url.pathname}`] || routes[url.href];

    if (!handler) {
      throw new Error(`Unexpected outgoing HTTP request in test: ${req.method} ${req.url}`);
    }

    return handler(req);
  };
}

describe("Service Account OAuth / Web Crypto RS256 Test Suite", () => {
  let testKeyPair: CryptoKeyPair;
  let testPemPrivateKey: string;

  before(async () => {
    // Generate ephemeral RSA key pair for testing
    testKeyPair = await crypto.subtle.generateKey(
      {
        name: "RSASSA-PKCS1-v1_5",
        modulusLength: 2048,
        publicExponent: new Uint8Array([1, 0, 1]),
        hash: "SHA-256"
      },
      true,
      ["sign", "verify"]
    );

    const pkcs8Der = await crypto.subtle.exportKey("pkcs8", testKeyPair.privateKey);
    const base64 = Buffer.from(pkcs8Der).toString("base64");
    testPemPrivateKey = `-----BEGIN PRIVATE KEY-----\n${base64}\n-----END PRIVATE KEY-----`;
  });

  // Test 1-8: JWT structure, claims, and header checks
  test("1-8 - JWT assertion contains correct header, claims, and 3 base64url parts", async () => {
    const now = 1700000000;
    const lifetime = 3600;

    const assertion = await createServiceAccountJwtAssertion({
      clientEmail: FAKE_CLIENT_EMAIL,
      privateKey: testKeyPair.privateKey,
      tokenEndpoint: FAKE_TOKEN_ENDPOINT,
      nowSeconds: now,
      lifetimeSeconds: lifetime
    });

    // 8. Exactly three base64url sections
    const parts = assertion.split(".");
    assert.equal(parts.length, 3, "JWT must contain exactly three parts");

    const header = JSON.parse(Buffer.from(parts[0], "base64url").toString("utf8"));
    const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8"));

    // 1. JWT header contains RS256
    assert.equal(header.alg, "RS256");

    // 2. JWT type is JWT
    assert.equal(header.typ, "JWT");

    // 3. Correct issuer
    assert.equal(payload.iss, FAKE_CLIENT_EMAIL);

    // 4. Correct datastore scope
    assert.equal(payload.scope, DEFAULT_DATASTORE_SCOPE);

    // 5. Audience matches configured OAuth endpoint
    assert.equal(payload.aud, FAKE_TOKEN_ENDPOINT);

    // 6. iat is current Unix timestamp
    assert.equal(payload.iat, now);

    // 7. exp is after iat
    assert.equal(payload.exp, now + lifetime);
    assert.ok(payload.exp > payload.iat);
  });

  // Test 9: Independent cryptographic signature verification
  test("9 - Signature is cryptographically valid using generated public key", async () => {
    const assertion = await createServiceAccountJwtAssertion({
      clientEmail: FAKE_CLIENT_EMAIL,
      privateKey: testKeyPair.privateKey,
      tokenEndpoint: FAKE_TOKEN_ENDPOINT
    });

    const [headerB64, payloadB64, signatureB64] = assertion.split(".");
    const signingInputBytes = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
    const signatureBytes = Buffer.from(signatureB64, "base64url");

    const isValid = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      testKeyPair.publicKey,
      signatureBytes,
      signingInputBytes
    );

    assert.equal(isValid, true, "Web Crypto RS256 signature must independently verify");
  });

  // Test 10: PEM private key import
  test("10 - PEM private key import produces a valid CryptoKey for signing", async () => {
    const importedKey = await importPemPrivateKey(testPemPrivateKey);
    assert.ok(importedKey);
    assert.equal(importedKey.type, "private");
    assert.equal(importedKey.algorithm.name, "RSASSA-PKCS1-v1_5");

    // Test signing with the imported key
    const assertion = await createServiceAccountJwtAssertion({
      clientEmail: FAKE_CLIENT_EMAIL,
      privateKey: importedKey,
      tokenEndpoint: FAKE_TOKEN_ENDPOINT
    });
    assert.equal(assertion.split(".").length, 3);
  });

  // Test 11: Google JSON escaped-newline PEM representation
  test("11 - PEM private key import accepts literal \\n sequences", async () => {
    const escapedPemPrivateKey = testPemPrivateKey.replace(/\n/g, "\\\\n");

    const importedKey = await importPemPrivateKey(escapedPemPrivateKey);
    assert.ok(importedKey);
    assert.equal(importedKey.type, "private");
    assert.equal(importedKey.algorithm.name, "RSASSA-PKCS1-v1_5");
  });

  // Test 12-15: OAuth request format and contents
  test("11-14 - OAuth request uses POST, form-urlencoded, grant_type, and assertion", async () => {
    let capturedMethod = "";
    let capturedContentType = "";
    let capturedBody = "";

    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: async (req) => {
        capturedMethod = req.method;
        capturedContentType = req.headers.get("Content-Type") || "";
        capturedBody = await req.text();

        return new Response(
          JSON.stringify({
            access_token: "mock-oauth-token-12345",
            token_type: "Bearer",
            expires_in: 3600
          }),
          { status: 200 }
        );
      }
    });

    const dummyAssertion = "header.payload.signature";
    const token = await exchangeJwtForAccessToken(
      dummyAssertion,
      FAKE_TOKEN_ENDPOINT,
      transport
    );

    // 11. POST method
    assert.equal(capturedMethod, "POST");

    // 12. application/x-www-form-urlencoded
    assert.equal(capturedContentType, "application/x-www-form-urlencoded");

    // 13. grant_type check
    const params = new URLSearchParams(capturedBody);
    assert.equal(params.get("grant_type"), OAUTH_JWT_GRANT_TYPE);

    // 14. assertion check
    assert.equal(params.get("assertion"), dummyAssertion);

    // Return value
    assert.equal(token, "mock-oauth-token-12345");
  });

  // Test 16: Mock OAuth success returns access_token
  test("15 - Mock OAuth success returns valid access_token", async () => {
    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response(
          JSON.stringify({
            access_token: "ya29.mock-token-abc",
            token_type: "Bearer",
            expires_in: 3600
          }),
          { status: 200 }
        );
      }
    });

    const token = await exchangeJwtForAccessToken("test.jwt.sig", FAKE_TOKEN_ENDPOINT, transport);
    assert.equal(token, "ya29.mock-token-abc");
  });

  // Test 17: Missing access_token throws controlled error
  test("16 - Missing access_token in response throws controlled ServiceAccountAuthError", async () => {
    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response(
          JSON.stringify({
            token_type: "Bearer",
            expires_in: 3600
          }),
          { status: 200 }
        );
      }
    });

    await assert.rejects(
      async () => {
        await exchangeJwtForAccessToken("test.jwt.sig", FAKE_TOKEN_ENDPOINT, transport);
      },
      (err: ServiceAccountAuthError) => {
        assert.match(err.message, /missing access_token/i);
        return true;
      }
    );
  });

  // Test 18: OAuth HTTP error throws controlled error
  test("17 - OAuth HTTP error throws controlled ServiceAccountAuthError", async () => {
    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response(
          JSON.stringify({ error: "invalid_grant", error_description: "Invalid assertion" }),
          { status: 400 }
        );
      }
    });

    await assert.rejects(
      async () => {
        await exchangeJwtForAccessToken("test.jwt.sig", FAKE_TOKEN_ENDPOINT, transport);
      },
      (err: ServiceAccountAuthError) => {
        assert.equal(err.statusCode, 400);
        assert.match(err.message, /HTTP 400/);
        return true;
      }
    );
  });

  // Test 19: Malformed OAuth response throws controlled error
  test("18 - Malformed OAuth response throws controlled ServiceAccountAuthError", async () => {
    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response("Not valid JSON <html/>", { status: 200 });
      }
    });

    await assert.rejects(
      async () => {
        await exchangeJwtForAccessToken("test.jwt.sig", FAKE_TOKEN_ENDPOINT, transport);
      },
      (err: ServiceAccountAuthError) => {
        assert.match(err.message, /malformed JSON/i);
        return true;
      }
    );
  });

  // Test 20: Invalid private key throws controlled error
  test("19 - Invalid private key format throws controlled ServiceAccountAuthError", async () => {
    // Case 1: Malformed base64
    await assert.rejects(
      async () => {
        await importPemPrivateKey("not-a-real-pem-key-12345");
      },
      (err: ServiceAccountAuthError) => {
        assert.match(err.message, /Failed to decode base64 in private key/i);
        return true;
      }
    );

    // Case 2: Valid base64 but not valid PKCS#8 DER
    await assert.rejects(
      async () => {
        await importPemPrivateKey("-----BEGIN PRIVATE KEY-----\nYWJjZGVmZw==\n-----END PRIVATE KEY-----");
      },
      (err: ServiceAccountAuthError) => {
        assert.match(err.message, /Failed to import PKCS#8 private key into Web Crypto/i);
        return true;
      }
    );

    // Case 3: Empty string
    assert.throws(
      () => {
        pemToPkcs8Der("");
      },
      (err: ServiceAccountAuthError) => {
        assert.match(err.message, /must be a non-empty string/i);
        return true;
      }
    );
  });

  // Test 21: Unexpected network route blocked by mock transport
  test("20 - Unexpected network route blocked immediately by mock transport", async () => {
    const strictTransport = createMockTransport({});

    await assert.rejects(
      async () => {
        await exchangeJwtForAccessToken("jwt", "https://unregistered.domain.com/token", strictTransport);
      },
      (err: Error) => {
        assert.match(err.message, /Unexpected outgoing HTTP request in test/);
        return true;
      }
    );
  });

  // Test 22: Sensitive credential material never appears in errors
  test("21 - Sensitive credential material never appears in thrown errors", async () => {
    const fakeSecretKey = "SUPER_SECRET_PRIVATE_KEY_BYTES_DO_NOT_LEAK";
    const failingTransport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response("OAuth error", { status: 401 });
      }
    });

    const provider = createServiceAccountTokenProvider({
      clientEmail: FAKE_CLIENT_EMAIL,
      privateKey: testPemPrivateKey,
      tokenEndpoint: FAKE_TOKEN_ENDPOINT,
      transport: failingTransport
    });

    try {
      await provider();
      assert.fail("Should have thrown");
    } catch (err: unknown) {
      assert.ok(err instanceof ServiceAccountAuthError);
      assert.equal(err.message.includes(fakeSecretKey), false);
      assert.equal(err.message.includes(testPemPrivateKey), false);
      assert.equal(err.message.includes("Bearer"), false);
    }
  });

  // Test 23: TokenProvider interface compatibility
  test("22 - TokenProvider interface compatibility produces string token for FirestoreClient", async () => {
    const transport = createMockTransport({
      [`POST ${FAKE_TOKEN_ENDPOINT}`]: () => {
        return new Response(
          JSON.stringify({
            access_token: "token-compatible-123",
            token_type: "Bearer",
            expires_in: 3600
          }),
          { status: 200 }
        );
      }
    });

    const provider = createServiceAccountTokenProvider({
      clientEmail: FAKE_CLIENT_EMAIL,
      privateKey: testPemPrivateKey,
      tokenEndpoint: FAKE_TOKEN_ENDPOINT,
      transport
    });

    const token = await provider();
    assert.equal(typeof token, "string");
    assert.equal(token, "token-compatible-123");
  });
});
