import test, { describe } from "node:test";
import assert from "node:assert/strict";
import { FirestoreClient, type FirestoreRawDocument, type HttpTransport } from "../src/firestore/firestoreClient.ts";
import { reportTransferCompletion } from "../src/matching/matchingService.ts";

const projectId = "nursing-super-app-test";

function createHarness(initial: Record<string, unknown>) {
  let fields: Record<string, any> = {};
  for (const [key, value] of Object.entries(initial)) {
    if (typeof value === "string") fields[key] = { stringValue: value };
    else if (typeof value === "boolean") fields[key] = { booleanValue: value };
  }
  let version = 1;
  const documentName = `projects/${projectId}/databases/(default)/documents/matches/match-1`;
  const transport: HttpTransport = async (input, init) => {
    const request = input instanceof Request ? input : new Request(input, init);
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname.endsWith("/documents/matches/match-1")) {
      const doc: FirestoreRawDocument = {
        name: documentName,
        updateTime: `2026-10-11T10:00:0${version}.000Z`,
        fields: structuredClone(fields)
      };
      return new Response(JSON.stringify(doc), { status: 200 });
    }
    if (request.method === "POST" && url.pathname.endsWith("/documents:commit")) {
      const body = await request.json() as { writes: Array<{ update?: { fields: Record<string, unknown> }; updateMask?: { fieldPaths: string[] }; currentDocument?: { updateTime?: string } }> };
      const write = body.writes[0];
      if (write.currentDocument?.updateTime && write.currentDocument.updateTime !== `2026-10-11T10:00:0${version}.000Z`) {
        return new Response(JSON.stringify({ error: { status: "ABORTED", message: "version conflict" } }), { status: 409 });
      }
      for (const path of write.updateMask?.fieldPaths ?? []) {
        if (write.update?.fields[path] !== undefined) fields[path] = write.update.fields[path];
      }
      version += 1;
      return new Response(JSON.stringify({ commitTime: "2026-10-11T10:00:10.000Z" }), { status: 200 });
    }
    return new Response(JSON.stringify({ error: { message: `Unexpected request ${request.method} ${url.pathname}` } }), { status: 500 });
  };
  return {
    client: new FirestoreClient({ projectId, tokenProvider: async () => "service-token", transport }),
    read: () => fields
  };
}

describe("self-reported transfer completion", () => {
  test("two-way match awaits the second participant and completes only after both report", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b",
      status: "CONFIRMED", completionReportedByA: false, completionReportedByB: false
    });
    const first = await reportTransferCompletion("nurse-a", "match-1", h.client);
    assert.equal(first.newStatus, "AWAITING_COMPLETION");
    assert.equal(first.completedParticipants, 1);
    assert.equal(h.read().status.stringValue, "AWAITING_COMPLETION");

    const second = await reportTransferCompletion("nurse-b", "match-1", h.client);
    assert.equal(second.newStatus, "COMPLETED");
    assert.equal(second.completedParticipants, 2);
    assert.equal(typeof h.read().completedAt.stringValue, "string");
    assert.equal(h.read().completionReportedByA.booleanValue, true);
    assert.equal(h.read().completionReportedByB.booleanValue, true);
  });

  test("three-way match completes only after all three participants report", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b", nurseCUid: "nurse-c",
      status: "CONFIRMED", completionReportedByA: false, completionReportedByB: false, completionReportedByC: false
    });
    assert.equal((await reportTransferCompletion("nurse-a", "match-1", h.client)).newStatus, "AWAITING_COMPLETION");
    assert.equal((await reportTransferCompletion("nurse-b", "match-1", h.client)).completedParticipants, 2);
    const final = await reportTransferCompletion("nurse-c", "match-1", h.client);
    assert.equal(final.newStatus, "COMPLETED");
    assert.equal(final.completedParticipants, 3);
  });

  test("rejects non-participants and matches not yet confirmed", async () => {
    const h = createHarness({ nurseAUid: "nurse-a", nurseBUid: "nurse-b", status: "CHAT_OPEN" });
    await assert.rejects(() => reportTransferCompletion("intruder", "match-1", h.client), (error: any) => error.code === "NON_PARTICIPANT");
    await assert.rejects(() => reportTransferCompletion("nurse-a", "match-1", h.client), (error: any) => error.code === "INVALID_COMPLETION_STATE");
  });

  test("duplicate report is idempotent and does not overwrite the other participant's state", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b", status: "AWAITING_COMPLETION",
      completionReportedByA: true, completionReportedByB: false
    });
    const result = await reportTransferCompletion("nurse-a", "match-1", h.client);
    assert.equal(result.newStatus, "AWAITING_COMPLETION");
    assert.equal(result.completedParticipants, 1);
    assert.equal(h.read().completionReportedByB.booleanValue, false);
  });
});
