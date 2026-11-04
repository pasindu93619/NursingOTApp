/**
 * Administrative one-off backfill script for Mutual Transfer match deadline fields.
 *
 * Usage:
 *   Dry-run (default, non-destructive):
 *     npx tsx scripts/backfillDeadlines.ts
 *     npx tsx scripts/backfillDeadlines.ts --dry-run
 *
 *   Live execution (requires explicit flag AND service account credentials in environment):
 *     npx tsx scripts/backfillDeadlines.ts --execute
 *
 * Invariants:
 * - Completely idempotent: skips any match whose *Ms fields already exist.
 * - Non-destructive: only adds missing *Ms fields, never overwrites or deletes.
 * - Defaults to dry-run mode unless explicitly instructed with --execute.
 * - Zero credentials leaked to stdout/stderr.
 */

import { FirestoreClient } from "../src/firestore/firestoreClient.ts";
import { createServiceAccountTokenProvider } from "../src/auth/serviceAccountAuth.ts";
import { backfillMatchDeadlines } from "../src/matching/matchingService.ts";

async function main() {
  const isExecute = process.argv.includes("--execute");
  const isDryRun = !isExecute || process.argv.includes("--dry-run");

  console.log(`[BACKFILL_MATCH_DEADLINES] Mode: ${isDryRun ? "DRY-RUN (no changes will be written)" : "LIVE EXECUTION"}`);

  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY;

  if (!projectId) {
    console.error("[BACKFILL_ERROR] FIREBASE_PROJECT_ID environment variable is required.");
    process.exit(1);
  }

  if (!isDryRun && (!clientEmail || !privateKey)) {
    console.error("[BACKFILL_ERROR] Live execution requires FIREBASE_SERVICE_ACCOUNT_CLIENT_EMAIL and FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY.");
    process.exit(1);
  }

  const tokenProvider = (clientEmail && privateKey)
    ? createServiceAccountTokenProvider({ clientEmail, privateKey })
    : async () => "mock-token-for-dry-run";

  const client = new FirestoreClient({
    projectId,
    tokenProvider
  });

  // Query pending confirmation matches
  const pendingDocs = await client.queryPendingConfirmationMatches(500);
  console.log(`[BACKFILL_MATCH_DEADLINES] Fetched ${pendingDocs.length} pending match candidate documents.`);

  const result = await backfillMatchDeadlines(pendingDocs, client, { dryRun: isDryRun });

  console.log("[BACKFILL_MATCH_DEADLINES] Completed:", {
    scanned: result.scanned,
    updated: result.updated,
    skipped: result.skipped,
    dryRun: result.dryRun,
    errorCount: result.errors.length
  });

  if (result.errors.length > 0) {
    console.error("[BACKFILL_MATCH_DEADLINES] Errors encountered:", result.errors);
    process.exit(1);
  }
}

main().catch((err) => {
  console.error("[BACKFILL_FATAL]", err instanceof Error ? err.message : String(err));
  process.exit(1);
});
