/**
 * Environment bindings injected by Cloudflare Workers runtime.
 */
export interface Env {
  FIREBASE_PROJECT_ID?: string;
  ENVIRONMENT?: string;
  // Service-account secrets for Firestore REST:
  FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL?: string;
  FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY?: string;
  // Match configuration:
  MATCH_EXPIRATION_HOURS?: string;
  CANDIDATE_LIMIT?: string;
}

export interface ScheduledEvent {
  cron: string;
  scheduledTime: number;
}

export interface ExecutionContext {
  waitUntil(promise: Promise<unknown>): void;
  passThroughOnException(): void;
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

// Request payload for match decision (accept/reject/confirm)
export interface MatchDecisionRequest {
  matchId: string;
  decision: "ACCEPT" | "REJECT" | "CONFIRM" | "LEAVE";
}

// Response payload after processing a decision
export interface MatchDecisionResponse {
  matchId: string;
  newStatus: string; // PENDING_CONFIRMATION | CHAT_OPEN | CONFIRMED | CANCELLED
  expiresAt: string;
  chatDeadline?: string;
  firstResponseAt?: string;
  decisionApplied: boolean;
}
