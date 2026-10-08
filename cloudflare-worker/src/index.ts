import type { Env, AuthCheckResponse, ScheduledEvent, ExecutionContext } from "./types.ts";
import {
  AuthError,
  extractBearerToken,
  verifyFirebaseIdToken
} from "./auth/firebaseAuth.ts";
import { FirestoreClient } from "./firestore/firestoreClient.ts";
import { createServiceAccountTokenProvider } from "./auth/serviceAccountAuth.ts";
import type { MatchDecisionRequest } from "./types.ts";
import {
  findAndLockMatch,
  respondToMatch,
  sweepExpiredMatches,
  MatchServiceError
} from "./matching/matchingService.ts";

const DEFAULT_PROJECT_ID = "nursing-super-app";

function corsHeaders(): HeadersInit {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Content-Type": "application/json"
  };
}

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: corsHeaders()
  });
}

function errorResponse(error: string, message: string, status = 400): Response {
  return jsonResponse(
    {
      error,
      message
    },
    status
  );
}

function resolveTokenProvider(env: Env): (() => Promise<string>) | Response {
  if (env.FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL && env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY) {
    return createServiceAccountTokenProvider({
      clientEmail: env.FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL,
      privateKey: env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY
    });
  }
  if (env.ENVIRONMENT === "production") {
    return errorResponse(
      "ConfigError",
      "Firebase service account credentials missing in production environment",
      500
    );
  }
  return async () => "mock-access-token";
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // 1. Handle CORS pre-flight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders()
      });
    }

    const url = new URL(request.url);
    const pathname = url.pathname;

    // 2. Health check endpoint
    if (pathname === "/" || pathname === "/health") {
      return jsonResponse({
        status: "ok",
        service: "nursing-transfer-worker",
        environment: env.ENVIRONMENT || "development"
      });
    }

    // 3. Authenticated check endpoint
    if (pathname === "/api/auth/check") {
      try {
        const authHeader = request.headers.get("Authorization");
        const token = extractBearerToken(authHeader);
        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;
        const verifiedResult = await verifyFirebaseIdToken(token, { projectId });
        const response: AuthCheckResponse = {
          authenticated: true,
          uid: verifiedResult.uid
        };
        return jsonResponse(response, 200);
      } catch (err: unknown) {
        if (err instanceof AuthError) {
          return errorResponse("Unauthorized", err.message, err.statusCode);
        }
        const message = err instanceof Error ? err.message : "Internal Server Error";
        return errorResponse("InternalError", message, 500);
      }
    }

    // 5. Server-Side Match Decision Accept/Reject
    if (pathname === "/api/matching/respond") {
      if (request.method !== "POST") {
        return errorResponse("MethodNotAllowed", "Method not allowed. Use POST.", 405);
      }
      try {
        const authHeader = request.headers.get("Authorization");
        const token = extractBearerToken(authHeader);
        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;
        const verifiedResult = await verifyFirebaseIdToken(token, { projectId });
        const callerUid = verifiedResult.uid;

        // Token provider for Firestore Client
        const tokenProviderResult = resolveTokenProvider(env);
        if (tokenProviderResult instanceof Response) {
          return tokenProviderResult;
        }
        const firestoreClient = new FirestoreClient({ projectId, tokenProvider: tokenProviderResult });

        const body = await request.json();
        const decisionReq = body as MatchDecisionRequest;
        if (!decisionReq.matchId || !decisionReq.decision) {
          return errorResponse("InvalidRequest", "matchId and decision are required", 400);
        }
        if (!["ACCEPT", "REJECT", "CONFIRM", "LEAVE"].includes(decisionReq.decision)) {
          return errorResponse("InvalidRequest", `Invalid decision: ${decisionReq.decision}`, 400);
        }
        const result = await respondToMatch(callerUid, decisionReq.matchId, decisionReq.decision, firestoreClient);
        return jsonResponse(result, 200);
      } catch (err: unknown) {
        if (err instanceof AuthError) {
          return errorResponse("Unauthorized", err.message, err.statusCode);
        }
        if (err instanceof MatchServiceError) {
          return errorResponse(err.code, err.message, err.statusCode);
        }
        const message = err instanceof Error ? err.message : "Internal Server Error";
        return errorResponse("InternalError", message, 500);
      }
    }

    // 4. Server-Side Mutual Matching + Atomic Locking
    if (pathname === "/api/matching/find-and-lock") {
      if (request.method !== "POST") {
        return errorResponse("MethodNotAllowed", "Method not allowed. Use POST.", 405);
      }
      try {
        const authHeader = request.headers.get("Authorization");
        const token = extractBearerToken(authHeader);
        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;
        // Security Invariant:
        // Caller UID is derived exclusively from cryptographically verified token.
        const verifiedResult = await verifyFirebaseIdToken(token, { projectId });
        const callerUid = verifiedResult.uid;

        // Token provider for Firestore Client
        const tokenProviderResult = resolveTokenProvider(env);
        if (tokenProviderResult instanceof Response) {
          return tokenProviderResult;
        }
        const firestoreClient = new FirestoreClient({ projectId, tokenProvider: tokenProviderResult });

        if (!env.MATCH_EXPIRATION_HOURS || env.MATCH_EXPIRATION_HOURS.trim().length === 0) {
          return errorResponse(
            "ConfigError",
            "MATCH_EXPIRATION_HOURS must be explicitly configured in environment (no implicit default permitted)",
            500
          );
        }

        const expirationHours = parseInt(env.MATCH_EXPIRATION_HOURS.trim(), 10);
        if (isNaN(expirationHours) || expirationHours <= 0) {
          return errorResponse(
            "ConfigError",
            "MATCH_EXPIRATION_HOURS must be a valid positive integer",
            500
          );
        }

        let candidateLimit: number | undefined;
        if (env.CANDIDATE_LIMIT && env.CANDIDATE_LIMIT.trim().length > 0) {
          const parsed = parseInt(env.CANDIDATE_LIMIT.trim(), 10);
          if (!isNaN(parsed) && parsed > 0) {
            candidateLimit = parsed;
          }
        }

        const result = await findAndLockMatch(callerUid, firestoreClient, {
          expirationHours,
          candidateLimit
        });

        return jsonResponse(result, 200);
      } catch (err: unknown) {
        console.error("[MATCHING_FIND_AND_LOCK_ERROR]", {
          name: err instanceof Error ? err.name : "UnknownError",
          message: err instanceof Error ? err.message : String(err),
          statusCode:
            typeof err === "object" &&
            err !== null &&
            "statusCode" in err
              ? (err as { statusCode?: unknown }).statusCode
              : undefined,
          errorCode:
            typeof err === "object" &&
            err !== null &&
            "code" in err
              ? (err as { code?: unknown }).code
              : undefined
        });
        if (err instanceof AuthError) {
          return errorResponse("Unauthorized", err.message, err.statusCode);
        }
        if (err instanceof MatchServiceError) {
          return errorResponse(err.code, err.message, err.statusCode);
        }
        const message = err instanceof Error ? err.message : "Internal Server Error";
        return errorResponse("InternalError", message, 500);
      }
    }

    // 6. Manual / Triggered Expiry Sweep Endpoint (Protected)
    if (pathname === "/api/matching/sweep-expired") {
      if (request.method !== "POST") {
        return errorResponse("MethodNotAllowed", "Method not allowed. Use POST.", 405);
      }
      try {
        const authHeader = request.headers.get("Authorization");
        const token = extractBearerToken(authHeader);
        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;
        await verifyFirebaseIdToken(token, { projectId });

        const tokenProviderResult = resolveTokenProvider(env);
        if (tokenProviderResult instanceof Response) {
          return tokenProviderResult;
        }
        const firestoreClient = new FirestoreClient({ projectId, tokenProvider: tokenProviderResult });

        const result = await sweepExpiredMatches(firestoreClient);
        return jsonResponse(result, 200);
      } catch (err: unknown) {
        if (err instanceof AuthError) {
          return errorResponse("Unauthorized", err.message, err.statusCode);
        }
        if (err instanceof MatchServiceError) {
          return errorResponse(err.code, err.message, err.statusCode);
        }
        const message = err instanceof Error ? err.message : "Internal Server Error";
        return errorResponse("InternalError", message, 500);
      }
    }

    // Default 404
    return errorResponse("NotFound", `Endpoint '${pathname}' not found`, 404);
  },

  async scheduled(event: ScheduledEvent, env: Env, ctx?: ExecutionContext): Promise<void> {
    const runSweep = async () => {
      try {
        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;
        const tokenProviderResult = resolveTokenProvider(env);
        if (tokenProviderResult instanceof Response) {
          console.error("[SCHEDULED_SWEEP_CONFIG_ERROR] Failed to resolve token provider");
          return;
        }
        const firestoreClient = new FirestoreClient({ projectId, tokenProvider: tokenProviderResult });
        const result = await sweepExpiredMatches(firestoreClient);
        console.log("[SCHEDULED_SWEEP_COMPLETED]", result);
      } catch (err: unknown) {
        console.error("[SCHEDULED_SWEEP_ERROR]", {
          message: err instanceof Error ? err.message : String(err)
        });
      }
    };

    if (ctx && typeof ctx.waitUntil === "function") {
      ctx.waitUntil(runSweep());
    } else {
      await runSweep();
    }
  }
};
