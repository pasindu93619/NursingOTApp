import type { CandidateRequest } from "../matching/matchingEngine.ts";

export type HttpTransport = (
  input: string | URL | Request,
  init?: RequestInit
) => Promise<Response>;

export type TokenProvider = () => Promise<string>;

export interface FirestoreClientConfig {
  projectId: string;
  tokenProvider: TokenProvider;
  transport?: HttpTransport;
}

export class FirestoreError extends Error {
  statusCode?: number;
  code?: string;

  constructor(message: string, statusCode?: number, code?: string) {
    super(message);
    this.name = "FirestoreError";
    this.statusCode = statusCode;
    this.code = code;
  }
}

// -----------------------------------------------------------------------------
// Firestore REST Wire Types
// -----------------------------------------------------------------------------

export type FirestoreValue =
  | { stringValue: string }
  | { booleanValue: boolean }
  | { integerValue: string }
  | { doubleValue: number }
  | { nullValue: null }
  | { arrayValue: { values?: FirestoreValue[] } }
  | { mapValue: { fields?: Record<string, FirestoreValue> } };

export interface FirestoreRawDocument {
  name: string;
  fields?: Record<string, FirestoreValue>;
  createTime?: string;
  updateTime?: string;
}

export interface FirestoreRunQueryItem {
  document?: FirestoreRawDocument;
  readTime?: string;
}

export interface FirestoreWrite {
  update?: {
    name: string;
    fields: Record<string, FirestoreValue>;
  };
  delete?: string;
  currentDocument?: {
    exists?: boolean;
    updateTime?: string;
  };
  updateMask?: {
    fieldPaths: string[];
  };
}

export interface FirestoreCommitResponse {
  commitTime: string;
  writeResults?: Array<{
    updateTime?: string;
  }>;
}

// -----------------------------------------------------------------------------
// Path Helpers
// -----------------------------------------------------------------------------

export function getDatabaseRootPath(projectId: string): string {
  return `projects/${projectId}/databases/(default)/documents`;
}

export function getTransferRequestDocPath(projectId: string, userId: string): string {
  return `${getDatabaseRootPath(projectId)}/transferRequests/${encodeURIComponent(userId)}`;
}

export function getMatchDocPath(projectId: string, matchId: string): string {
  return `${getDatabaseRootPath(projectId)}/matches/${encodeURIComponent(matchId)}`;
}

// -----------------------------------------------------------------------------
// Value Conversion Helpers
// -----------------------------------------------------------------------------

export function toFirestoreValue(val: unknown): FirestoreValue {
  if (val === null || val === undefined) {
    return { nullValue: null };
  }
  if (typeof val === "string") {
    return { stringValue: val };
  }
  if (typeof val === "boolean") {
    return { booleanValue: val };
  }
  if (typeof val === "number") {
    if (Number.isInteger(val)) {
      return { integerValue: val.toString() };
    }
    return { doubleValue: val };
  }
  if (Array.isArray(val)) {
    return {
      arrayValue: {
        values: val.map(toFirestoreValue)
      }
    };
  }
  if (typeof val === "object") {
    const fields: Record<string, FirestoreValue> = {};
    for (const [k, v] of Object.entries(val)) {
      fields[k] = toFirestoreValue(v);
    }
    return { mapValue: { fields } };
  }
  return { stringValue: String(val) };
}

export function fromFirestoreValue(val: FirestoreValue | undefined): unknown {
  if (!val) return null;
  if ("stringValue" in val) return val.stringValue;
  if ("booleanValue" in val) return val.booleanValue;
  if ("integerValue" in val) return parseInt(val.integerValue, 10);
  if ("doubleValue" in val) return val.doubleValue;
  if ("nullValue" in val) return null;
  if ("arrayValue" in val) {
    const values = val.arrayValue.values || [];
    return values.map(fromFirestoreValue);
  }
  if ("mapValue" in val) {
    const fields = val.mapValue.fields || {};
    const res: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(fields)) {
      res[k] = fromFirestoreValue(v);
    }
    return res;
  }
  return null;
}

export function toFirestoreFields(obj: Record<string, unknown>): Record<string, FirestoreValue> {
  const fields: Record<string, FirestoreValue> = {};
  for (const [k, v] of Object.entries(obj)) {
    fields[k] = toFirestoreValue(v);
  }
  return fields;
}

export function candidateRequestFromDoc(doc: FirestoreRawDocument): CandidateRequest {
  const fields = doc.fields || {};

  const getString = (f?: FirestoreValue): string => {
    if (f && "stringValue" in f) return f.stringValue;
    return "";
  };

  const getBool = (f?: FirestoreValue): boolean => {
    if (f && "booleanValue" in f) return f.booleanValue;
    return false;
  };

  const getNullableString = (f?: FirestoreValue): string | null => {
    if (f && "stringValue" in f) return f.stringValue;
    return null;
  };

  const getArrayStrings = (f?: FirestoreValue): string[] => {
    if (f && "arrayValue" in f && Array.isArray(f.arrayValue.values)) {
      return f.arrayValue.values
        .map(v => ("stringValue" in v ? v.stringValue : ""))
        .filter(s => s.length > 0);
    }
    return [];
  };

  // Derive firebaseUid from fields or document path
  let firebaseUid = getString(fields.firebaseUid);
  if (!firebaseUid && doc.name) {
    const segments = doc.name.split("/");
    firebaseUid = segments[segments.length - 1] || "";
  }

  return {
    firebaseUid,
    status: getString(fields.status) || "PENDING",
    locked: getBool(fields.locked),
    currentMatchId: getNullableString(fields.currentMatchId),
    currentHospitalId: getString(fields.currentHospitalId),
    preferenceHospitalIds: getArrayStrings(fields.preferenceHospitalIds),
    grade: getString(fields.grade),
    updateTime: doc.updateTime
  };
}

// -----------------------------------------------------------------------------
// Firestore Client
// -----------------------------------------------------------------------------

export class FirestoreClient {
  private projectId: string;
  private tokenProvider: TokenProvider;
  private transport: HttpTransport;
  private baseUrl: string;

  constructor(config: FirestoreClientConfig) {
    if (!config.projectId || config.projectId.trim().length === 0) {
      throw new FirestoreError("projectId must not be empty");
    }
    this.projectId = config.projectId.trim();
    this.tokenProvider = config.tokenProvider;
    this.transport = config.transport || globalThis.fetch.bind(globalThis);
    this.baseUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents`;
  }

  public getProjectId(): string {
    return this.projectId;
  }

  private async getAuthHeader(): Promise<string> {
    try {
      const token = await this.tokenProvider();
      if (!token || typeof token !== "string" || token.trim().length === 0) {
        throw new FirestoreError("TokenProvider returned an empty or invalid token");
      }
      return `Bearer ${token.trim()}`;
    } catch (err: unknown) {
      if (err instanceof FirestoreError) throw err;
      const msg = err instanceof Error ? err.message : "Failed to obtain access token";
      throw new FirestoreError(`Authentication error: ${msg}`);
    }
  }

  /**
   * Reads a single match document.
   * Returns null if HTTP 404 (document not found).
   */
  async getMatchDoc(matchId: string): Promise<FirestoreRawDocument | null> {
    if (!matchId || matchId.trim().length === 0) {
      throw new FirestoreError("matchId must not be empty");
    }
    const authHeader = await this.getAuthHeader();
    const url = `${this.baseUrl}/matches/${encodeURIComponent(matchId.trim())}`;
    let response: Response;
    try {
      response = await this.transport(url, {
        method: "GET",
        headers: {
          Authorization: authHeader,
          Accept: "application/json"
        }
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Network failure";
      throw new FirestoreError(`Transport error while reading match: ${msg}`);
    }
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new FirestoreError(`Failed to read match document: HTTP ${response.status}`, response.status);
    }
    let rawDoc: FirestoreRawDocument;
    try {
      rawDoc = (await response.json()) as FirestoreRawDocument;
    } catch {
      throw new FirestoreError("Invalid JSON returned by Firestore");
    }
    return rawDoc;
  }

  /**
   * Reads a single nurse's transfer request document.
   * Returns null if HTTP 404 (document not found).
   */
  async getRequestDoc(userId: string): Promise<CandidateRequest | null> {
    if (!userId || userId.trim().length === 0) {
      throw new FirestoreError("userId must not be empty");
    }

    const authHeader = await this.getAuthHeader();
    const url = `${this.baseUrl}/transferRequests/${encodeURIComponent(userId.trim())}`;

    let response: Response;
    try {
      response = await this.transport(url, {
        method: "GET",
        headers: {
          Authorization: authHeader,
          Accept: "application/json"
        }
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Network failure";
      throw new FirestoreError(`Transport error while reading request: ${msg}`);
    }

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      throw new FirestoreError(
        `Failed to read transfer request: HTTP ${response.status}`,
        response.status
      );
    }

    let rawDoc: FirestoreRawDocument;
    try {
      rawDoc = (await response.json()) as FirestoreRawDocument;
    } catch {
      throw new FirestoreError("Invalid JSON returned by Firestore");
    }

    return candidateRequestFromDoc(rawDoc);
  }

  /**
   * Queries searching candidates.
   *
   * Query filter:
   * status == 'SEARCHING' AND locked == false
   *
   * Invariant: Does NOT enforce grade equality in the query.
   * Cross-grade candidates remain eligible in accordance with project rules.
   */
  async querySearchingCandidates(limit = 100, offset = 0): Promise<CandidateRequest[]> {
    const authHeader = await this.getAuthHeader();
    const url = `${this.baseUrl}:runQuery`;

    const structuredQuery: Record<string, unknown> = {
      from: [{ collectionId: "transferRequests" }],
      where: {
        compositeFilter: {
          op: "AND",
          filters: [
            {
              fieldFilter: {
                field: { fieldPath: "status" },
                op: "EQUAL",
                value: { stringValue: "SEARCHING" }
              }
            },
            {
              fieldFilter: {
                field: { fieldPath: "locked" },
                op: "EQUAL",
                value: { booleanValue: false }
              }
            }
          ]
        }
      },
      limit,
      orderBy: [
        {
          field: { fieldPath: "__name__" },
          direction: "ASCENDING"
        }
      ]
    };

    if (offset > 0) {
      structuredQuery.offset = offset;
    }

    const body = { structuredQuery };

    let response: Response;
    try {
      response = await this.transport(url, {
        method: "POST",
        headers: {
          Authorization: authHeader,
          "Content-Type": "application/json",
          Accept: "application/json"
        },
        body: JSON.stringify(body)
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Network failure";
      throw new FirestoreError(`Transport error while querying candidates: ${msg}`);
    }

    if (!response.ok) {
      throw new FirestoreError(
        `Failed to query searching candidates: HTTP ${response.status}`,
        response.status
      );
    }

    let items: FirestoreRunQueryItem[];
    try {
      items = (await response.json()) as FirestoreRunQueryItem[];
    } catch {
      throw new FirestoreError("Invalid JSON returned by Firestore query");
    }

    const candidates: CandidateRequest[] = [];
    if (Array.isArray(items)) {
      for (const item of items) {
        if (item.document) {
          candidates.push(candidateRequestFromDoc(item.document));
        }
      }
    }

    return candidates;
  }

  /**
   * Queries matches with status IN ['PENDING_CONFIRMATION', 'CHAT_OPEN'].
   * Used by server-side expiry sweep.
   */
  async queryPendingConfirmationMatches(limit = 100): Promise<FirestoreRawDocument[]> {
    const authHeader = await this.getAuthHeader();
    const url = `${this.baseUrl}:runQuery`;

    const structuredQuery: Record<string, unknown> = {
      from: [{ collectionId: "matches" }],
      where: {
        fieldFilter: {
          field: { fieldPath: "status" },
          op: "IN",
          value: {
            arrayValue: {
              values: [
                { stringValue: "PENDING_CONFIRMATION" },
                { stringValue: "CHAT_OPEN" }
              ]
            }
          }
        }
      },
      limit,
      orderBy: [
        {
          field: { fieldPath: "__name__" },
          direction: "ASCENDING"
        }
      ]
    };

    const body = { structuredQuery };

    let response: Response;
    try {
      response = await this.transport(url, {
        method: "POST",
        headers: {
          Authorization: authHeader,
          "Content-Type": "application/json",
          Accept: "application/json"
        },
        body: JSON.stringify(body)
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Network failure";
      throw new FirestoreError(`Transport error while querying pending matches: ${msg}`);
    }

    if (!response.ok) {
      throw new FirestoreError(
        `Failed to query pending matches: HTTP ${response.status}`,
        response.status
      );
    }

    let items: FirestoreRunQueryItem[];
    try {
      items = (await response.json()) as FirestoreRunQueryItem[];
    } catch {
      throw new FirestoreError("Invalid JSON returned by Firestore query");
    }

    const matchDocs: FirestoreRawDocument[] = [];
    if (Array.isArray(items)) {
      for (const item of items) {
        if (item.document) {
          matchDocs.push(item.document);
        }
      }
    }

    return matchDocs;
  }

  /**
   * Low-level atomic commit capability.
   *
   * Sends caller-provided writes atomically to Firestore REST :commit endpoint.
   * Does NOT perform matching decisions.
   */
  async commitAtomicMatch(writes: FirestoreWrite[]): Promise<FirestoreCommitResponse> {
    if (!Array.isArray(writes) || writes.length === 0) {
      throw new FirestoreError("Commit requires at least one write operation");
    }

    const authHeader = await this.getAuthHeader();
    const url = `${this.baseUrl}:commit`;

    // Firestore REST Write.currentDocument is a protobuf oneof: exactly one of
    // exists or updateTime may be present. Normalize every write at the transport
    // boundary so callers cannot accidentally send an invalid oneof or stray fields.
    const normalizedWrites = writes.map((write): FirestoreWrite => {
      const normalized: FirestoreWrite = {};

      if (write.update) {
        normalized.update = {
          name: write.update.name,
          fields: write.update.fields
        };
      }

      if (write.delete) {
        normalized.delete = write.delete;
      }

      if (write.updateMask) {
        normalized.updateMask = {
          fieldPaths: [...write.updateMask.fieldPaths]
        };
      }

      if (write.currentDocument?.updateTime) {
        normalized.currentDocument = {
          updateTime: write.currentDocument.updateTime
        };
      } else if (write.currentDocument?.exists !== undefined) {
        normalized.currentDocument = {
          exists: write.currentDocument.exists
        };
      }

      return normalized;
    });

    const body = { writes: normalizedWrites };

    let response: Response;
    try {
      response = await this.transport(url, {
        method: "POST",
        headers: {
          Authorization: authHeader,
          "Content-Type": "application/json",
          Accept: "application/json"
        },
        body: JSON.stringify(body)
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Network failure";
      throw new FirestoreError(`Transport error while committing writes: ${msg}`);
    }

    if (!response.ok) {
      let message = `Failed to commit atomic writes: HTTP ${response.status}`;
      let code: string | undefined;

      try {
        const errorBody = (await response.json()) as {
          error?: { message?: string; status?: string };
        };
        if (errorBody.error?.message) {
          message = errorBody.error.message;
        }
        code = errorBody.error?.status;
      } catch {
        // Keep the HTTP status when Firestore does not return JSON.
      }

      throw new FirestoreError(message, response.status, code);
    }

    try {
      return (await response.json()) as FirestoreCommitResponse;
    } catch {
      throw new FirestoreError("Invalid JSON returned by Firestore commit");
    }
  }
}
