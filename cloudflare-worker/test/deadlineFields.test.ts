import test, { describe } from "node:test";
import assert from "node:assert/strict";
import {
  readDeadlineMs,
  respondToMatch,
  sweepExpiredMatches,
  backfillMatchDeadlines,
  expireMatchNow,
  MatchServiceError
} from "../src/matching/matchingService.ts";
import { FirestoreClient, type HttpTransport } from "../src/firestore/firestoreClient.ts";
import type { FirestoreRawDocument, FirestoreWrite } from "../src/firestore/firestoreClient.ts";
import type { FirestoreValue } from "../src/types.ts";

const TEST_PROJECT_ID = "nursing-super-app-test";

function createMockTransport(
  routes: Record<string, (req: Request) => Promise<Response> | Response>
): HttpTransport {
  return async (input, init) => {
    const req = input instanceof Request ? input : new Request(input, init);
    const url = new URL(req.url);
    const method = req.method;
    const pathname = url.pathname;

    let handler = routes[`${method} ${pathname}`] || routes[pathname];
    if (!handler) {
      for (const [routePattern, fn] of Object.entries(routes)) {
        const parts = routePattern.split(" ");
        if (parts.length === 2) {
          const [m, p] = parts;
          if (m === method && pathname.includes(p)) {
            handler = fn;
            break;
          }
        } else if (pathname.includes(routePattern)) {
          handler = fn;
          break;
        }
      }
    }

    if (!handler) {
      if (req.method === "GET" && url.pathname.includes("/transferRequests/")) {
        return new Response("Not found", { status: 404 });
      }
      throw new Error(`Unexpected outgoing HTTP request in test: ${req.method} ${req.url}`);
    }

    return handler(req);
  };
}

describe("Phase 1, Step 1.4: Deadline Fields Suite", () => {
  // =========================================================================
  // 1. readDeadlineMs Helper Unit Tests
  // =========================================================================
  describe("readDeadlineMs Helper", () => {
    test("1 - ISO-only deadline document is parsed to epoch milliseconds", () => {
      const iso = "2026-10-15T12:00:00.000Z";
      const expectedMs = Date.parse(iso);
      const fields: Record<string, FirestoreValue> = {
        expiresAt: { stringValue: iso }
      };

      const result = readDeadlineMs(fields, "expiresAtMs", "expiresAt");
      assert.equal(result, expectedMs);
    });

    test("2 - When both ISO and millisecond fields exist, millisecond field takes precedence", () => {
      const iso = "2026-10-15T12:00:00.000Z"; // Date.parse => 1792065600000
      const msValue = 1799999999000; // different value
      const fields: Record<string, FirestoreValue> = {
        expiresAt: { stringValue: iso },
        expiresAtMs: { integerValue: msValue.toString() }
      };

      const result = readDeadlineMs(fields, "expiresAtMs", "expiresAt");
      assert.equal(result, msValue);
    });

    test("3 - Valid millisecond field is returned directly when ISO field is absent", () => {
      const msValue = 1795000000000;
      const fields: Record<string, FirestoreValue> = {
        chatDeadlineMs: { integerValue: msValue.toString() }
      };

      const result = readDeadlineMs(fields, "chatDeadlineMs", "chatDeadline");
      assert.equal(result, msValue);
    });

    test("4 - Missing both ms and ISO fields returns undefined", () => {
      const fields: Record<string, FirestoreValue> = {
        otherField: { stringValue: "hello" }
      };

      const result = readDeadlineMs(fields, "expiresAtMs", "expiresAt");
      assert.equal(result, undefined);
    });

    test("5 - Malformed ISO timestamp string returns undefined without throwing", () => {
      const fields: Record<string, FirestoreValue> = {
        expiresAt: { stringValue: "not-a-valid-date-string" }
      };

      const result = readDeadlineMs(fields, "expiresAtMs", "expiresAt");
      assert.equal(result, undefined);
    });

    test("6 - Non-integer millisecond field falls back to valid ISO field", () => {
      const iso = "2026-10-20T08:00:00.000Z";
      const expectedMs = Date.parse(iso);
      const fields: Record<string, FirestoreValue> = {
        expiresAtMs: { integerValue: "not-an-int" },
        expiresAt: { stringValue: iso }
      };

      const result = readDeadlineMs(fields, "expiresAtMs", "expiresAt");
      assert.equal(result, expectedMs);
    });
  });

  // =========================================================================
  // 2. respondToMatch Deadline Reads & Writes
  // =========================================================================
  describe("respondToMatch Deadline Consistency", () => {
    test("7 - Match with ISO-only deadline is successfully accepted", async () => {
      const futureIso = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
      const mockDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-iso-only`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAt: { stringValue: futureIso }
          // expiresAtMs intentionally omitted
        }
      };

      let committedWrites: FirestoreWrite[] = [];
      const transport = createMockTransport({
        "GET /databases/(default)/documents/matches/match-iso-only": async () => {
          return new Response(JSON.stringify(mockDoc), { status: 200 });
        },
        "POST /databases/(default)/documents:commit": async (req) => {
          const body = await req.json() as { writes: FirestoreWrite[] };
          committedWrites = body.writes;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      const res = await respondToMatch("nurse-a", "match-iso-only", "ACCEPT", client);
      assert.equal(res.decisionApplied, true);
      assert.equal(res.newStatus, "PENDING_CONFIRMATION");

      // Verify sliding window wrote both string and integer ms
      const matchWrite = committedWrites.find(w => w.update?.name?.includes("/matches/match-iso-only"));
      assert.ok(matchWrite);
      assert.ok(matchWrite.update?.fields?.firstResponseAt);
      assert.ok(matchWrite.update?.fields?.firstResponseAtMs);
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("firstResponseAt"));
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("firstResponseAtMs"));
    });

    test("8 - Match with millisecond field precedence evaluates deadline correctly", async () => {
      // ISO says future, but expiresAtMs says past: must expire
      const futureIso = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
      const pastMs = Date.now() - 3600 * 1000; // 1 hour ago
      const mockDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-both`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAt: { stringValue: futureIso },
          expiresAtMs: { integerValue: pastMs.toString() }
        }
      };

      let expiredDocWritten = false;
      const transport = createMockTransport({
        "GET /databases/(default)/documents/matches/match-both": async () => {
          return new Response(JSON.stringify(mockDoc), { status: 200 });
        },
        "GET /databases/(default)/documents/transferRequests/nurse-a": async () => {
          return new Response(JSON.stringify({
            fields: { currentMatchId: { stringValue: "match-both" }, locked: { booleanValue: true } }
          }), { status: 200 });
        },
        "GET /databases/(default)/documents/transferRequests/nurse-b": async () => {
          return new Response(JSON.stringify({
            fields: { currentMatchId: { stringValue: "match-both" }, locked: { booleanValue: true } }
          }), { status: 200 });
        },
        "POST /databases/(default)/documents:commit": async (req) => {
          expiredDocWritten = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      // Since pastMs takes precedence over futureIso, inline expiry must trigger (Audit A1) and return 410 for ACCEPT
      await assert.rejects(
        async () => {
          await respondToMatch("nurse-a", "match-both", "ACCEPT", client);
        },
        (err: unknown) => {
          return err instanceof MatchServiceError && err.code === "EXPIRED" && err.statusCode === 410;
        }
      );
      assert.equal(expiredDocWritten, true);
    });

    test("9 - Transition to CHAT_OPEN writes both chatDeadline and chatDeadlineMs", async () => {
      const futureIso = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
      const mockDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-all-accept`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          acceptedByA: { booleanValue: true }, // nurse-a already accepted
          expiresAt: { stringValue: futureIso },
          expiresAtMs: { integerValue: Date.parse(futureIso).toString() }
        }
      };

      let committedWrites: FirestoreWrite[] = [];
      const transport = createMockTransport({
        "GET /databases/(default)/documents/matches/match-all-accept": async () => {
          return new Response(JSON.stringify(mockDoc), { status: 200 });
        },
        "POST /databases/(default)/documents:commit": async (req) => {
          const body = await req.json() as { writes: FirestoreWrite[] };
          committedWrites = body.writes;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      // nurse-b accepts, completing 2-way match -> transitions to CHAT_OPEN
      const res = await respondToMatch("nurse-b", "match-all-accept", "ACCEPT", client);
      assert.equal(res.newStatus, "CHAT_OPEN");

      const matchWrite = committedWrites.find(w => w.update?.name?.includes("/matches/match-all-accept"));
      assert.ok(matchWrite);
      const fields = matchWrite.update?.fields ?? {};

      // Verify chatDeadline and chatDeadlineMs
      assert.ok(fields.chatDeadline?.stringValue);
      assert.ok(fields.chatDeadlineMs?.integerValue);
      const cdMs = Number(fields.chatDeadlineMs?.integerValue);
      const cdIsoParsed = Date.parse(fields.chatDeadline?.stringValue ?? "");
      assert.equal(cdMs, cdIsoParsed);

      // Verify expiresAt and expiresAtMs were updated to match chatDeadline
      assert.ok(fields.expiresAt?.stringValue);
      assert.ok(fields.expiresAtMs?.integerValue);
      assert.equal(fields.expiresAt?.stringValue, fields.chatDeadline?.stringValue);
      assert.equal(fields.expiresAtMs?.integerValue, fields.chatDeadlineMs?.integerValue);

      // Verify updateMask
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("chatDeadline"));
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("chatDeadlineMs"));
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("expiresAt"));
      assert.ok(matchWrite.updateMask?.fieldPaths?.includes("expiresAtMs"));
    });

    test("10 - Missing or unparseable deadline fields throw MALFORMED_MATCH_DOC", async () => {
      const mockDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-corrupt`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAt: { stringValue: "unparseable-gibberish" }
          // expiresAtMs absent
        }
      };

      const transport = createMockTransport({
        "GET /databases/(default)/documents/matches/match-corrupt": async () => {
          return new Response(JSON.stringify(mockDoc), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      await assert.rejects(
        async () => {
          await respondToMatch("nurse-a", "match-corrupt", "ACCEPT", client);
        },
        (err: unknown) => {
          return err instanceof MatchServiceError && err.code === "MALFORMED_MATCH_DOC" && err.statusCode === 500;
        }
      );
    });
  });

  // =========================================================================
  // 3. sweepExpiredMatches Deadline Evaluation
  // =========================================================================
  describe("sweepExpiredMatches Deadline Evaluation", () => {
    test("11 - Sweep identifies expired match by expiresAtMs even if ISO string is missing", async () => {
      const pastMs = Date.now() - 3600 * 1000;
      const expiredDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-sweep-ms`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAtMs: { integerValue: pastMs.toString() }
          // expiresAt string absent
        }
      };

      let expiredCommitted = false;
      const transport = createMockTransport({
        "POST /databases/(default)/documents:runQuery": async () => {
          return new Response(JSON.stringify([{ document: expiredDoc }]), { status: 200 });
        },
        "GET /databases/(default)/documents/transferRequests/nurse-a": async () => {
          return new Response(JSON.stringify({
            fields: { currentMatchId: { stringValue: "match-sweep-ms" }, locked: { booleanValue: true } }
          }), { status: 200 });
        },
        "GET /databases/(default)/documents/transferRequests/nurse-b": async () => {
          return new Response(JSON.stringify({
            fields: { currentMatchId: { stringValue: "match-sweep-ms" }, locked: { booleanValue: true } }
          }), { status: 200 });
        },
        "POST /databases/(default)/documents:commit": async () => {
          expiredCommitted = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      const sweepResult = await sweepExpiredMatches(client);
      assert.equal(sweepResult.scanned, 1);
      assert.equal(sweepResult.expired, 1);
      assert.equal(expiredCommitted, true);
    });

    test("12 - Sweep does not expire match when now <= expiresAtMs", async () => {
      const futureMs = Date.now() + 3600 * 1000;
      const futureDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-sweep-future`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          nurseAUid: { stringValue: "nurse-a" },
          nurseBUid: { stringValue: "nurse-b" },
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAtMs: { integerValue: futureMs.toString() }
        }
      };

      const transport = createMockTransport({
        "POST /databases/(default)/documents:runQuery": async () => {
          return new Response(JSON.stringify([{ document: futureDoc }]), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      const sweepResult = await sweepExpiredMatches(client);
      assert.equal(sweepResult.scanned, 1);
      assert.equal(sweepResult.expired, 0);
    });
  });

  // =========================================================================
  // 4. backfillMatchDeadlines Idempotency & Safety
  // =========================================================================
  describe("backfillMatchDeadlines Idempotency & Safety", () => {
    test("13 - Match with only ISO fields has corresponding *Ms fields derived", async () => {
      const expiresIso = "2026-10-15T12:00:00.000Z";
      const chatDeadlineIso = "2026-10-18T12:00:00.000Z";
      const firstResponseIso = "2026-10-13T10:00:00.000Z";

      const candidateDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-backfill-1`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          status: { stringValue: "CHAT_OPEN" },
          expiresAt: { stringValue: expiresIso },
          chatDeadline: { stringValue: chatDeadlineIso },
          firstResponseAt: { stringValue: firstResponseIso }
          // all *Ms fields absent
        }
      };

      let committedWrites: FirestoreWrite[] = [];
      const transport = createMockTransport({
        "POST /databases/(default)/documents:commit": async (req) => {
          const body = await req.json() as { writes: FirestoreWrite[] };
          committedWrites = body.writes;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      const result = await backfillMatchDeadlines([candidateDoc], client, { dryRun: false });
      assert.equal(result.scanned, 1);
      assert.equal(result.updated, 1);
      assert.equal(result.skipped, 0);
      assert.equal(result.errors.length, 0);

      assert.equal(committedWrites.length, 1);
      const write = committedWrites[0];
      const fields = write.update?.fields ?? {};
      assert.equal(fields.expiresAtMs?.integerValue, Date.parse(expiresIso).toString());
      assert.equal(fields.chatDeadlineMs?.integerValue, Date.parse(chatDeadlineIso).toString());
      assert.equal(fields.firstResponseAtMs?.integerValue, Date.parse(firstResponseIso).toString());
      assert.deepEqual(write.updateMask?.fieldPaths, ["expiresAtMs", "chatDeadlineMs", "firstResponseAtMs"]);
    });

    test("14 - Match already possessing *Ms fields is skipped (idempotent)", async () => {
      const candidateDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-already-done`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAt: { stringValue: "2026-10-15T12:00:00.000Z" },
          expiresAtMs: { integerValue: "1792065600000" }
        }
      };

      let commitCalled = false;
      const transport = createMockTransport({
        "POST /databases/(default)/documents:commit": async () => {
          commitCalled = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      const result = await backfillMatchDeadlines([candidateDoc], client, { dryRun: false });
      assert.equal(result.scanned, 1);
      assert.equal(result.updated, 0);
      assert.equal(result.skipped, 1);
      assert.equal(commitCalled, false);
    });

    test("15 - Dry-run mode reports planned changes without committing to Firestore", async () => {
      const candidateDoc: FirestoreRawDocument = {
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-dry-run`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          status: { stringValue: "PENDING_CONFIRMATION" },
          expiresAt: { stringValue: "2026-10-15T12:00:00.000Z" }
        }
      };

      let commitCalled = false;
      const transport = createMockTransport({
        "POST /databases/(default)/documents:commit": async () => {
          commitCalled = true;
          return new Response(JSON.stringify({ commitTime: "2026-10-02T10:01:00.000Z" }), { status: 200 });
        }
      });

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport
      });

      // Default dryRun: true
      const result = await backfillMatchDeadlines([candidateDoc], client);
      assert.equal(result.dryRun, true);
      assert.equal(result.scanned, 1);
      assert.equal(result.updated, 1); // would-be updated
      assert.equal(commitCalled, false); // no write committed
    });

    test("16 - Terminal matches (CANCELLED, EXPIRED, CONFIRMED) are skipped", async () => {
      const terminalStatuses = ["CANCELLED", "EXPIRED", "CONFIRMED"];
      const docs: FirestoreRawDocument[] = terminalStatuses.map((st, i) => ({
        name: `projects/${TEST_PROJECT_ID}/databases/(default)/documents/matches/match-term-${i}`,
        updateTime: "2026-10-02T10:00:00.000Z",
        fields: {
          status: { stringValue: st },
          expiresAt: { stringValue: "2026-10-15T12:00:00.000Z" }
        }
      }));

      const client = new FirestoreClient({
        projectId: TEST_PROJECT_ID,
        tokenProvider: async () => "token",
        transport: createMockTransport({})
      });

      const result = await backfillMatchDeadlines(docs, client, { dryRun: false });
      assert.equal(result.scanned, 3);
      assert.equal(result.updated, 0);
      assert.equal(result.skipped, 3);
    });
  });
});
