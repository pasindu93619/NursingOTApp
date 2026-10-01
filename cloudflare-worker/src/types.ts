/**
 * Environment bindings injected by Cloudflare Workers runtime.
 */
export interface Env {
  FIREBASE_PROJECT_ID?: string;
  ENVIRONMENT?: string;
  // Future service-account secrets for Firestore REST:
  FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL?: string;
  FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY?: string;
}

/**
 * Standard Firebase Auth ID Token decoded payload claims.
 */
export interface FirebaseIdTokenPayload {
  iss: string;
  aud: string;
  sub: string;
  auth_time: number;
  iat: number;
  exp: number;
  user_id?: string;
  email?: string;
  email_verified?: boolean;
  firebase?: Record<string, unknown>;
  [key: string]: unknown;
}

/**
 * Validated authentication result.
 */
export interface VerifiedAuthResult {
  uid: string;
  issuer: string;
  audience: string;
  authTime: number;
  exp: number;
}

/**
 * Minimal authenticated API response.
 * Never leaks private transfer data.
 */
export interface AuthCheckResponse {
  authenticated: boolean;
  uid: string;
}

/**
 * Standard Google JWK for public RSA key verification.
 */
export interface GoogleJwk {
  kty: string;
  alg: string;
  use?: string;
  kid: string;
  n: string;
  e: string;
}

export interface GoogleJwksResponse {
  keys: GoogleJwk[];
}
