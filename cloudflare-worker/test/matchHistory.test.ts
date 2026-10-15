import test, { describe } from "node:test";
import assert from "node:assert/strict";
import { FirestoreClient, type HttpTransport, type FirestoreRawDocument } from "../src/firestore/firestoreClient.ts";
import { getMatchHistory } from "../src/matching/matchingService.ts";

const projectId = "nursing-super-app-test";
const participantUid = "nurse-123";

function matchDoc(
  id: string,
  fields: Record<string, unknown>,
  updateTime: string
): FirestoreRawDocument {
  const wireFields: Record<string, any> = {};
  for (const [key, value] of Object.entries(fields)) {
    if (typeof value === "string") wireFields[key] = { stringValue: value };
    else if (typeof value === "boolean") wireFields[key] = { booleanValue: value };
  }
  return {
    name: `projects/${projectId}/databases/(default)/documents/matches/${id}`,
    createTime: String(fields.createdAt ?? updateTime),
    updateTime,
    fields: wireFields
  };
}

describe("participant match history", () => {
  test("returns only terminal matches for the authenticated UID, deduplicated and newest first", async () => {
    const cancelled = matchDoc("cancelled-1", {
      nurseAUid: participantUid,
      nurseBUid: "nurse-456",
      status: "CANCELLED",
      matchType: "DIRECT_2_WAY",
      createdAt: "2026-10-01T10:00:00.000Z",
      terminalReason: "A participant rejected the proposed match."
    }, "2026-10-01T11:00:00.000Z");
    const expired = matchDoc("expired-1", {
      nurseBUid: participantUid,
      nurseCUid: "nurse-789",
      status: "EXPIRED",
      createdAt: "2026-10-02T10:00:00.000Z"
    }, "2026-10-02T12:00:00.000Z");
    const active = matchDoc("active-1", {
      nurseCUid: participantUid,
      nurseAUid: "nurse-456",
      nurseBUid: "nurse-789",
      status: "CHAT_OPEN",
      createdAt: "2026-10-03T10:00:00.000Z"
    }, "2026-10-03T11:00:00.000Z");

    const transport: HttpTransport = async (input, init) => {
      const request = input instanceof Request ? input : new Request(input, init);
      const body = await request.json() as { structuredQuery?: { where?: { fieldFilter?: { field?: { fieldPath?: string } } } } };
      const field = body.structuredQuery?.where?.fieldFilter?.field?.fieldPath;
      const docs = field === "nurseAUid"
        ? [cancelled, active]
        : field === "nurseBUid"
          ? [expired]
          : [active];
      return new Response(JSON.stringify(docs.map(document => ({ document }))), { status: 200 });
    };

    const client = new FirestoreClient({
      projectId,
      tokenProvider: async () => "service-token",
      transport
    });

    const history = await getMatchHistory(participantUid, client);

    assert.deepEqual(history.map(item => item.matchId), ["expired-1", "cancelled-1"]);
    assert.equal(history[0].status, "EXPIRED");
    assert.equal(history[0].matchType, "THREE_WAY");
    assert.match(history[0].reason, /deadline passed/i);
    assert.equal(history[1].reason, "A participant rejected the proposed match.");
  });

  test("rejects a blank participant identity", async () => {
    const client = new FirestoreClient({
      projectId,
      tokenProvider: async () => "service-token",
      transport: async () => new Response("[]", { status: 200 })
    });
    await assert.rejects(() => getMatchHistory("  ", client), /Authenticated user ID is required/);
  });
});
