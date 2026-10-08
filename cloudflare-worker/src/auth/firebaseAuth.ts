import type {
  FirebaseIdTokenPayload,
  GoogleJwk,
  GoogleJwksResponse,
  VerifiedAuthResult
} from "../types.ts";

const GOOGLE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

// In-memory JWKS cache with expiration
interface JwksCache {
  keys: GoogleJwk[];
  expiresAt: number;
}

let jwksCache: JwksCache | null = null;

export class AuthError extends Error {
  statusCode: number;

  constructor(message: string, statusCode: number = 401) {
    super(message);
    this.name = "AuthError";
    this.statusCode = statusCode;
  }
}

/**
 * Extracts Bearer token from the Authorization header.
 * Throws AuthError (401) on missing or malformed header.
 */
export function extractBearerToken(authHeader: string | null | undefined): string {
  if (!authHeader || typeof authHeader !== "string") {
    throw new AuthError("Missing Authorization header", 401);
  }

  const parts = authHeader.trim().split(/\s+/);
  if (parts.length !== 2 || parts[0].toLowerCase() !== "bearer" || !parts[1]) {
    throw new AuthError("Malformed Authorization header: Expected 'Bearer <token>'", 401);
  }

  return parts[1];
}

/**
 * Decodes base64url string to Uint8Array.
 */
export function base64UrlToUint8Array(str: string): Uint8Array {
  const base64 = str.replace(/-/g, "+").replace(/_/g, "/");
  const pad = base64.length % 4;
  const padded = pad ? base64 + "=".repeat(4 - pad) : base64;
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

/**
 * Decodes base64url string to JSON object.
 */
export function base64UrlToJson<T>(str: string): T {
  try {
    const bytes = base64UrlToUint8Array(str);
    const decoded = new TextDecoder().decode(bytes);
    return JSON.parse(decoded) as T;
  } catch (err) {
    throw new AuthError("Invalid base64url JSON encoding in token", 401);
  }
}

/**
 * Fetches Google public JWKS, caching in memory for up to 1 hour (or Cache-Control max-age).
 */
export async function getGoogleJwks(customFetch?: typeof fetch): Promise<GoogleJwk[]> {
  const now = Date.now();
  if (jwksCache && jwksCache.expiresAt > now) {
    return jwksCache.keys;
  }

  const fetcher = customFetch || fetch;
  let res: Response;
  try {
    res = await fetcher(GOOGLE_JWKS_URL);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Network failure";
    throw new AuthError(`Unable to fetch Google JWKS public keys: ${msg}`, 502);
  }

  if (!res.ok) {
    throw new AuthError("Unable to fetch Google JWKS public keys", 502);
  }

  const data = (await res.json()) as GoogleJwksResponse;
  if (!data || !Array.isArray(data.keys)) {
    throw new AuthError("Invalid JWKS response structure from Google", 502);
  }

  // Cache for 1 hour by default, or read max-age header if available
  let maxAgeSeconds = 3600;
  const cacheControl = res.headers.get("cache-control");
  if (cacheControl) {
    const match = cacheControl.match(/max-age=(\d+)/i);
    if (match && match[1]) {
      maxAgeSeconds = parseInt(match[1], 10);
    }
  }

  jwksCache = {
    keys: data.keys,
    expiresAt: now + maxAgeSeconds * 1000
  };

  return data.keys;
}

/**
 * Clears the JWKS cache (useful for testing or cache refresh).
 */
export function resetJwksCache(): void {
  jwksCache = null;
}

export interface VerifyTokenOptions {
  projectId: string;
  jwksProvider?: () => Promise<GoogleJwk[]>;
  clockSkewSeconds?: number;
  nowSeconds?: number;
}

/**
 * Cryptographically verifies a Firebase Auth ID Token.
 *
 * Checks:
 * 1. 3-part JWT structure
 * 2. Header: alg == "RS256", non-empty kid
 * 3. Matching Google JWK public key found
 * 4. RS256 digital signature verified via Web Crypto API
 * 5. Claims:
 *    - aud == projectId
 *    - iss == https://securetoken.google.com/<projectId>
 *    - sub non-empty (this is the verified Firebase UID)
 *    - exp > now
 *    - iat <= now (with clock skew)
 *    - auth_time <= now (with clock skew)
 */
export async function verifyFirebaseIdToken(
  token: string,
  options: VerifyTokenOptions
): Promise<VerifiedAuthResult> {
  if (!token || typeof token !== "string") {
    throw new AuthError("Token must be a non-empty string", 401);
  }

  const parts = token.split(".");
  if (parts.length !== 3) {
    throw new AuthError("Invalid JWT: Token must have exactly 3 parts", 401);
  }

  const [headerB64, payloadB64, signatureB64] = parts;

  // 1. Decode and validate header
  interface JwtHeader {
    alg?: string;
    kid?: string;
    typ?: string;
  }
  const header = base64UrlToJson<JwtHeader>(headerB64);

  if (header.alg !== "RS256") {
    throw new AuthError(`Invalid JWT algorithm: Expected RS256, got ${header.alg}`, 401);
  }

  if (!header.kid || typeof header.kid !== "string" || header.kid.trim().length === 0) {
    throw new AuthError("Invalid JWT header: Missing or empty 'kid'", 401);
  }

  // 2. Decode and validate payload claims
  const payload = base64UrlToJson<FirebaseIdTokenPayload>(payloadB64);
  const clockSkew = options.clockSkewSeconds ?? 60;
  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);

  if (payload.aud !== options.projectId) {
    throw new AuthError(
      `Invalid token audience: Expected project ID '${options.projectId}', got '${payload.aud}'`,
      401
    );
  }

  const expectedIssuer = `https://securetoken.google.com/${options.projectId}`;
  if (payload.iss !== expectedIssuer) {
    throw new AuthError(
      `Invalid token issuer: Expected '${expectedIssuer}', got '${payload.iss}'`,
      401
    );
  }

  if (!payload.sub || typeof payload.sub !== "string" || payload.sub.trim().length === 0) {
    throw new AuthError("Invalid token subject: 'sub' must be a non-empty string UID", 401);
  }

  if (typeof payload.exp !== "number" || payload.exp <= now - clockSkew) {
    throw new AuthError(`Token has expired at epoch ${payload.exp} (current: ${now})`, 401);
  }

  if (typeof payload.iat !== "number" || payload.iat > now + clockSkew) {
    throw new AuthError(`Token issued in future: iat ${payload.iat} (current: ${now})`, 401);
  }

  if (typeof payload.auth_time === "number" && payload.auth_time > now + clockSkew) {
    throw new AuthError(`Token auth_time in future: auth_time ${payload.auth_time} (current: ${now})`, 401);
  }

  // 3. Obtain Google public key for this kid
  const jwks = options.jwksProvider
    ? await options.jwksProvider()
    : await getGoogleJwks();

  const matchingJwk = jwks.find((k) => k.kid === header.kid);
  if (!matchingJwk) {
    throw new AuthError(
      `Public key with kid '${header.kid}' not found in Google JWKS`,
      401
    );
  }

  // 4. Cryptographic signature check via Web Crypto API (crypto.subtle)
  try {
    const cryptoKey = await crypto.subtle.importKey(
      "jwk",
      matchingJwk,
      {
        name: "RSASSA-PKCS1-v1_5",
        hash: "SHA-256"
      },
      false,
      ["verify"]
    );

    const signedData = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
    const signatureBytes = base64UrlToUint8Array(signatureB64);

    const isValid = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      cryptoKey,
      signatureBytes,
      signedData
    );

    if (!isValid) {
      throw new AuthError("Invalid token signature: Cryptographic verification failed", 401);
    }
  } catch (err: unknown) {
    if (err instanceof AuthError) throw err;
    throw new AuthError(
      `Signature verification error: ${err instanceof Error ? err.message : String(err)}`,
      401
    );
  }

  return {
    uid: payload.sub,
    issuer: payload.iss,
    audience: payload.aud,
    authTime: payload.auth_time,
    exp: payload.exp
  };
}
