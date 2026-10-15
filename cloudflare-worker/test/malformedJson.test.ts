import test, { describe, before } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";
import type { Env, GoogleJwk } from "../src/types.ts";

const TEST_PROJECT_ID = "nursing-super-app-test";

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

describe("Worker Malformed JSON Hardening Suite", () => {
  let validToken: string;
  let googleJwk: GoogleJwk;

  before(async () => {
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

    const publicJwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
    googleJwk = {
      kty: "RSA",
      use: "sig",
      alg: "RS256",
      n: publicJwk.n!,
      e: publicJwk.e!,
      kid: "test-key-id"
    };

    const now = Math.floor(Date.now() / 1000);
    validToken = await createSignedTestJwt(
      { alg: "RS256", kid: "test-key-id", typ: "JWT" },
      {
        iss: `https://securetoken.google.com/${TEST_PROJECT_ID}`,
        aud: TEST_PROJECT_ID,
        sub: "nurse-a",
        auth_time: now - 60,
        iat: now - 60,
        exp: now + 3600
      },
      keyPair.privateKey
    );
  });

  test("POST /api/matching/respond with malformed JSON returns 400 InvalidRequest", async () => {
    const originalFetch = globalThis.fetch;
    try {
      globalThis.fetch = async (url: string | URL | Request) => {
        const urlStr = url.toString();
        if (urlStr.includes("service_accounts/v1/jwk")) {
          return new Response(JSON.stringify({ keys: [googleJwk] }), { status: 200 });
        }
        return new Response("Not found", { status: 404 });
      };

      const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
      const request = new Request("http://localhost/api/matching/respond", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`,
          "Content-Type": "application/json"
        },
        body: "{malformed_json_here"
      });

      const response = await worker.fetch(request, env);
      assert.equal(response.status, 400);
      const body = (await response.json()) as { error: string; message: string };
      assert.equal(body.error, "InvalidRequest");
      assert.equal(body.message, "Malformed JSON body");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  test("POST /api/matching/completion with malformed JSON returns 400 InvalidRequest", async () => {
    const originalFetch = globalThis.fetch;
    try {
      globalThis.fetch = async (url: string | URL | Request) => {
        const urlStr = url.toString();
        if (urlStr.includes("service_accounts/v1/jwk")) {
          return new Response(JSON.stringify({ keys: [googleJwk] }), { status: 200 });
        }
        return new Response("Not found", { status: 404 });
      };

      const env: Env = { FIREBASE_PROJECT_ID: TEST_PROJECT_ID };
      const request = new Request("http://localhost/api/matching/completion", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${validToken}`,
          "Content-Type": "application/json"
        },
        body: "{\"matchId\": not_a_valid_string"
      });

      const response = await worker.fetch(request, env);
      assert.equal(response.status, 400);
      const body = (await response.json()) as { error: string; message: string };
      assert.equal(body.error, "InvalidRequest");
      assert.equal(body.message, "Malformed JSON body");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });
});
