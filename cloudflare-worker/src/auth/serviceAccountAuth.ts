import type { TokenProvider, HttpTransport } from "../firestore/firestoreClient.ts";

export const DEFAULT_GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
export const DEFAULT_DATASTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
export const OAUTH_JWT_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

export interface ServiceAccountAuthConfig {
  clientEmail: string;
  privateKey: string; // PKCS#8 PEM string
  tokenEndpoint?: string;
  scope?: string;
  jwtLifetimeSeconds?: number;
  transport?: HttpTransport;
}

export class ServiceAccountAuthError extends Error {
  statusCode?: number;

  constructor(message: string, statusCode?: number) {
    super(message);
    this.name = "ServiceAccountAuthError";
    this.statusCode = statusCode;
  }
}

// -----------------------------------------------------------------------------
// Base64URL Encoding Utilities (Pure Web APIs)
// -----------------------------------------------------------------------------

export function uint8ArrayToBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

export function stringToBase64Url(str: string): string {
  const bytes = new TextEncoder().encode(str);
  return uint8ArrayToBase64Url(bytes);
}

// -----------------------------------------------------------------------------
// Private Key PEM Handling
// -----------------------------------------------------------------------------

/**
 * Strips PEM header/footer and decodes base64 bytes to PKCS#8 DER Uint8Array.
 * Never leaks the key in error messages.
 */
export function pemToPkcs8Der(pem: string): Uint8Array {
  if (!pem || typeof pem !== "string" || pem.trim().length === 0) {
    throw new ServiceAccountAuthError("Private key must be a non-empty string");
  }

  const cleanBase64 = pem
    .replace(/-----BEGIN[ A-Z_-]+-----/g, "")
    .replace(/-----END[ A-Z_-]+-----/g, "")
    .replace(/\s+/g, "");

  if (cleanBase64.length === 0) {
    throw new ServiceAccountAuthError("Private key contains no valid base64 content");
  }

  try {
    const binary = atob(cleanBase64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  } catch {
    throw new ServiceAccountAuthError("Failed to decode base64 in private key");
  }
}

/**
 * Imports a PKCS#8 PEM formatted private RSA key into a Web Crypto CryptoKey.
 */
export async function importPemPrivateKey(pem: string): Promise<CryptoKey> {
  const pkcs8Der = pemToPkcs8Der(pem);

  try {
    return await crypto.subtle.importKey(
      "pkcs8",
      pkcs8Der,
      {
        name: "RSASSA-PKCS1-v1_5",
        hash: "SHA-256"
      },
      false, // non-extractable
      ["sign"]
    );
  } catch {
    throw new ServiceAccountAuthError("Failed to import PKCS#8 private key into Web Crypto");
  }
}

// -----------------------------------------------------------------------------
// Service Account JWT Assertion Generation
// -----------------------------------------------------------------------------

export interface JwtAssertionOptions {
  clientEmail: string;
  privateKey: CryptoKey;
  tokenEndpoint: string;
  scope?: string;
  nowSeconds?: number;
  lifetimeSeconds?: number;
}

/**
 * Constructs and signs an RS256 JWT bearer assertion for Google OAuth token exchange.
 *
 * Invariants:
 * - Header: { alg: "RS256", typ: "JWT" }
 * - Claims:
 *   - iss: clientEmail
 *   - scope: target OAuth scope (e.g. Datastore)
 *   - aud: tokenEndpoint
 *   - iat: now
 *   - exp: now + lifetimeSeconds (e.g. 3600)
 */
export async function createServiceAccountJwtAssertion(
  options: JwtAssertionOptions
): Promise<string> {
  if (!options.clientEmail || options.clientEmail.trim().length === 0) {
    throw new ServiceAccountAuthError("clientEmail must not be empty");
  }
  if (!options.tokenEndpoint || options.tokenEndpoint.trim().length === 0) {
    throw new ServiceAccountAuthError("tokenEndpoint must not be empty");
  }

  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  const lifetime = options.lifetimeSeconds ?? 3600;

  if (lifetime <= 0 || lifetime > 3600) {
    throw new ServiceAccountAuthError(
      "JWT assertion lifetime must be between 1 and 3600 seconds"
    );
  }

  const header = {
    alg: "RS256",
    typ: "JWT"
  };

  const payload = {
    iss: options.clientEmail.trim(),
    scope: (options.scope || DEFAULT_DATASTORE_SCOPE).trim(),
    aud: options.tokenEndpoint.trim(),
    iat: now,
    exp: now + lifetime
  };

  const encodedHeader = stringToBase64Url(JSON.stringify(header));
  const encodedPayload = stringToBase64Url(JSON.stringify(payload));
  const signingInput = `${encodedHeader}.${encodedPayload}`;

  let signatureBuffer: ArrayBuffer;
  try {
    signatureBuffer = await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      options.privateKey,
      new TextEncoder().encode(signingInput)
    );
  } catch {
    throw new ServiceAccountAuthError("Cryptographic signature generation failed");
  }

  const encodedSignature = uint8ArrayToBase64Url(new Uint8Array(signatureBuffer));
  return `${signingInput}.${encodedSignature}`;
}

// -----------------------------------------------------------------------------
// OAuth Token Exchange
// -----------------------------------------------------------------------------

/**
 * Exchanges a signed JWT assertion for a Google OAuth access token.
 *
 * Enforces:
 * - Method: POST
 * - Content-Type: application/x-www-form-urlencoded
 * - grant_type: urn:ietf:params:oauth:grant-type:jwt-bearer
 * - assertion: <signed JWT>
 * - Sanitized error messages (credentials never exposed in errors)
 */
export async function exchangeJwtForAccessToken(
  assertion: string,
  tokenEndpoint: string,
  transport: HttpTransport = globalThis.fetch.bind(globalThis)
): Promise<string> {
  if (!assertion || typeof assertion !== "string" || assertion.trim().length === 0) {
    throw new ServiceAccountAuthError("JWT assertion must not be empty");
  }
  if (!tokenEndpoint || typeof tokenEndpoint !== "string" || tokenEndpoint.trim().length === 0) {
    throw new ServiceAccountAuthError("tokenEndpoint must not be empty");
  }

  const params = new URLSearchParams({
    grant_type: OAUTH_JWT_GRANT_TYPE,
    assertion: assertion.trim()
  });

  let response: Response;
  try {
    response = await transport(tokenEndpoint.trim(), {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json"
      },
      body: params.toString()
    });
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Transport error";
    throw new ServiceAccountAuthError(`OAuth transport failure: ${msg}`);
  }

  if (!response.ok) {
    throw new ServiceAccountAuthError(
      `OAuth token endpoint request failed with HTTP ${response.status}`,
      response.status
    );
  }

  let data: unknown;
  try {
    data = await response.json();
  } catch {
    throw new ServiceAccountAuthError("OAuth token endpoint returned malformed JSON");
  }

  if (!data || typeof data !== "object") {
    throw new ServiceAccountAuthError("Unexpected OAuth response structure");
  }

  const resObj = data as Record<string, unknown>;
  const accessToken = resObj.access_token;

  if (!accessToken || typeof accessToken !== "string" || accessToken.trim().length === 0) {
    throw new ServiceAccountAuthError("OAuth response missing access_token");
  }

  return accessToken.trim();
}

// -----------------------------------------------------------------------------
// TokenProvider Factory
// -----------------------------------------------------------------------------

/**
 * Creates a TokenProvider function satisfying: () => Promise<string>.
 * Caches the CryptoKey in memory so it does not need to re-parse the PEM on each call.
 */
export function createServiceAccountTokenProvider(
  config: ServiceAccountAuthConfig
): TokenProvider {
  if (!config.clientEmail || config.clientEmail.trim().length === 0) {
    throw new ServiceAccountAuthError("clientEmail must not be empty");
  }
  if (!config.privateKey || config.privateKey.trim().length === 0) {
    throw new ServiceAccountAuthError("privateKey must not be empty");
  }

  const clientEmail = config.clientEmail.trim();
  const tokenEndpoint = (config.tokenEndpoint || DEFAULT_GOOGLE_TOKEN_ENDPOINT).trim();
  const scope = (config.scope || DEFAULT_DATASTORE_SCOPE).trim();
  const lifetimeSeconds = config.jwtLifetimeSeconds ?? 3600;
  const transport = config.transport || globalThis.fetch.bind(globalThis);

  let cachedCryptoKey: CryptoKey | null = null;

  return async (): Promise<string> => {
    if (!cachedCryptoKey) {
      cachedCryptoKey = await importPemPrivateKey(config.privateKey);
    }

    const assertion = await createServiceAccountJwtAssertion({
      clientEmail,
      privateKey: cachedCryptoKey,
      tokenEndpoint,
      scope,
      lifetimeSeconds
    });

    return await exchangeJwtForAccessToken(assertion, tokenEndpoint, transport);
  };
}
