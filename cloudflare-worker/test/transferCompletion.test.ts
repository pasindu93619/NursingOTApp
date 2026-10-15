import test, { describe } from "node:test";
import assert from "node:assert/strict";
import { FirestoreClient, type FirestoreRawDocument, type HttpTransport } from "../src/firestore/firestoreClient.ts";
import { getTransferCompletionStatus, reportTransferCompletion } from "../src/matching/matchingService.ts";

const projectId = "nursing-super-app-test";

function createHarness(initial: Record<string, unknown>) {
  let fields: Record<string, any> = {};
  for (const [key, value] of Object.entries(initial)) {
    if (typeof value === "string") fields[key] = { stringValue: value };
    else if (typeof value === "boolean") fields[key] = { booleanValue: value };
  }
  let version = 1;
  const documentName = `projects/${projectId}/databases/(default)/documents/matches/match-1`;
  const requestDocs = new Map<string, { fields: Record<string, any>; version: number }>();
  for (const uid of [initial.nurseAUid, initial.nurseBUid, initial.nurseCUid].filter((x): x is string => typeof x === "string")) {
    requestDocs.set(uid, {
      version: 1,
      fields: {
        firebaseUid: { stringValue: uid },
        status: { stringValue: "MATCHED" },
        locked: { booleanValue: true },
        currentMatchId: { stringValue: "match-1" },
        currentHospitalId: { stringValue: "hospital-old" },
        grade: { stringValue: "Grade III" }
      }
    });
  }
  const transport: HttpTransport = async (input, init) => {
    const request = input instanceof Request ? input : new Request(input, init);
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname.endsWith("/documents/matches/match-1")) {
      const doc: FirestoreRawDocument = {
        name: documentName,
        updateTime: `2026-10-11T10:00:${String(version).padStart(2, "0")}.000Z`,
        fields: structuredClone(fields)
      };
      return new Response(JSON.stringify(doc), { status: 200 });
    }
    if (request.method === "GET" && url.pathname.includes("/documents/transferRequests/")) {
      const uid = decodeURIComponent(url.pathname.split("/").pop() ?? "");
      const stored = requestDocs.get(uid);
      if (!stored) return new Response(JSON.stringify({ error: { message: "not found" } }), { status: 404 });
      return new Response(JSON.stringify({
        name: `projects/${projectId}/databases/(default)/documents/transferRequests/${uid}`,
        updateTime: `2026-10-11T10:01:${String(stored.version).padStart(2, "0")}.000Z`,
        fields: structuredClone(stored.fields)
      }), { status: 200 });
    }
    if (request.method === "POST" && url.pathname.endsWith("/documents:commit")) {
      const body = await request.json() as { writes: Array<{ update?: { name: string; fields: Record<string, any> }; updateMask?: { fieldPaths: string[] }; currentDocument?: { updateTime?: string } }> };
      for (const write of body.writes) {
        const name = write.update?.name ?? "";
        if (name.includes("/matches/")) {
          if (write.currentDocument?.updateTime && write.currentDocument.updateTime !== `2026-10-11T10:00:${String(version).padStart(2, "0")}.000Z`) {
            return new Response(JSON.stringify({ error: { status: "ABORTED", message: "version conflict" } }), { status: 409 });
          }
          for (const fieldPath of write.updateMask?.fieldPaths ?? []) {
            if (write.update?.fields[fieldPath] !== undefined) fields[fieldPath] = write.update.fields[fieldPath];
          }
          version += 1;
        } else if (name.includes("/transferRequests/")) {
          const uid = decodeURIComponent(name.split("/").pop() ?? "");
          const stored = requestDocs.get(uid);
          if (!stored) return new Response(JSON.stringify({ error: { status: "NOT_FOUND", message: "request missing" } }), { status: 409 });
          for (const fieldPath of write.updateMask?.fieldPaths ?? []) {
            if (write.update?.fields[fieldPath] !== undefined) stored.fields[fieldPath] = write.update.fields[fieldPath];
          }
          stored.version += 1;
        }
      }
      return new Response(JSON.stringify({ commitTime: "2026-10-11T10:00:10.000Z" }), { status: 200 });
    }
    return new Response(JSON.stringify({ error: { message: `Unexpected request ${request.method} ${url.pathname}` } }), { status: 500 });
  };
  return {
    client: new FirestoreClient({ projectId, tokenProvider: async () => "service-token", transport }),
    read: () => fields,
    readRequest: (uid: string) => requestDocs.get(uid)?.fields
  };
}

describe("self-reported transfer completion", () => {
  test("two-way match awaits the second participant and completes only after both report", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b", nurseADestinationHospitalId: "hospital-b", nurseBDestinationHospitalId: "hospital-a",
      status: "CONFIRMED", completionReportedByA: false, completionReportedByB: false
    });
    const first = await reportTransferCompletion("nurse-a", "match-1", h.client);
    assert.equal(first.status, "AWAITING_COMPLETION");
    assert.equal(first.completedParticipants, 1);
    assert.equal(h.read().status.stringValue, "AWAITING_COMPLETION");

    const second = await reportTransferCompletion("nurse-b", "match-1", h.client);
    assert.equal(second.status, "COMPLETED");
    assert.equal(second.completedParticipants, 2);
    assert.equal(typeof h.read().completedAt.stringValue, "string");
    assert.equal(h.read().completionReportedByA.booleanValue, true);
    assert.equal(h.read().completionReportedByB.booleanValue, true);
    assert.equal(h.readRequest("nurse-a")?.currentHospitalId.stringValue, "hospital-b");
    assert.equal(h.readRequest("nurse-a")?.status.stringValue, "COMPLETED");
  });

  test("three-way match completes only after all three participants report", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b", nurseCUid: "nurse-c", nurseADestinationHospitalId: "hospital-b", nurseBDestinationHospitalId: "hospital-c", nurseCDestinationHospitalId: "hospital-a",
      status: "CONFIRMED", completionReportedByA: false, completionReportedByB: false, completionReportedByC: false
    });
    assert.equal((await reportTransferCompletion("nurse-a", "match-1", h.client)).status, "AWAITING_COMPLETION");
    assert.equal((await reportTransferCompletion("nurse-b", "match-1", h.client)).completedParticipants, 2);
    const final = await reportTransferCompletion("nurse-c", "match-1", h.client);
    assert.equal(final.status, "COMPLETED");
    assert.equal(final.completedParticipants, 3);
    assert.equal(h.readRequest("nurse-c")?.currentHospitalId.stringValue, "hospital-a");
  });

  test("completion progress is visible only to match participants", async () => {
    const h = createHarness({
      nurseAUid: "nurse-a", nurseBUid: "nurse-b", status: "AWAITING_COMPLETION",
      completionReportedByA: true, completionReportedByB: false
    });
    const status = await getTransferCompletionStatus("nurse-a", "match-1", h.client);
    assert.equal(status.callerReportedComplete, true);
    assert.equal(status.completedParticipants, 1);
    assert.equal(status.totalParticipants, 2);
    await assert.rejects(() => getTransferCompletionStatus("intruder", "match-1", h.client), (error: any) => error.code === "NON_PARTICIPANT");
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
    assert.equal(result.status, "AWAITING_COMPLETION");
    assert.equal(result.completedParticipants, 1);
    assert.equal(h.read().completionReportedByB.booleanValue, false);
  });
});
