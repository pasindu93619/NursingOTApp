import type { Env, AuthCheckResponse } from "./types.ts";
import {
  AuthError,
  extractBearerToken,
  verifyFirebaseIdToken
} from "./auth/firebaseAuth.ts";

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

    // 3. Authenticated check endpoint: POST or GET /api/auth/check
    if (pathname === "/api/auth/check") {
      try {
        const authHeader = request.headers.get("Authorization");
        const token = extractBearerToken(authHeader);

        const projectId = env.FIREBASE_PROJECT_ID || DEFAULT_PROJECT_ID;

        // Cryptographically verify token
        const verifiedResult = await verifyFirebaseIdToken(token, {
          projectId
        });

        // Security Invariant:
        // Even if the caller passed a body with `{ uid: "attacker" }`, the Worker
        // strictly uses verifiedResult.uid from the cryptographically verified JWT.
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

    // 4. Default 404
    return errorResponse("NotFound", `Endpoint '${pathname}' not found`, 404);
  }
};
