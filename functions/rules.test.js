const { test, describe, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');

const PROJECT_ID = 'demo-rules-unit-testing';
const RULES_PATH = path.resolve(__dirname, '../firestore.rules');

describe('Firestore Security Rules Emulator Suite', () => {
  let testEnv;

  before(async () => {
    const rules = fs.readFileSync(RULES_PATH, 'utf8');
    const [host, portStr] = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080').split(':');
    const port = parseInt(portStr, 10);

    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host,
        port,
        rules,
      },
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    if (testEnv) {
      await testEnv.clearFirestore();
    }
  });

  // Admin seeding helper bypassing security rules
  async function seedDoc(docPath, data) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.doc(docPath).set(data);
    });
  }

  function getAuthDb(uid) {
    return testEnv.authenticatedContext(uid).firestore();
  }

  function getUnauthDb() {
    return testEnv.unauthenticatedContext().firestore();
  }

  // -------------------------------------------------------------
  // 1. Transfer Requests (/transferRequests/{userId})
  // -------------------------------------------------------------
  describe('Transfer Requests (/transferRequests/{userId})', () => {
    const validCreateData = {
      firebaseUid: 'nurse-A',
      status: 'SEARCHING',
      locked: false,
      currentHospitalId: 'HOSP-001',
      preferenceHospitalIds: ['HOSP-002', 'HOSP-003'],
      grade: 'Grade I',
    };

    test('unauthenticated caller cannot read or list transfer requests', async () => {
      const db = getUnauthDb();
      await assertFails(db.collection('transferRequests').doc('nurse-A').get());
      await assertFails(db.collection('transferRequests').get());
    });

    test('authenticated user cannot list transferRequests collection (enumeration blocked)', async () => {
      const db = getAuthDb('nurse-A');
      await assertFails(db.collection('transferRequests').get());
    });

    test('user A cannot read user B request, but can read own request', async () => {
      await seedDoc('transferRequests/nurse-B', {
        ...validCreateData,
        firebaseUid: 'nurse-B',
      });
      await seedDoc('transferRequests/nurse-A', validCreateData);

      const dbA = getAuthDb('nurse-A');
      await assertFails(dbA.collection('transferRequests').doc('nurse-B').get());
      await assertSucceeds(dbA.collection('transferRequests').doc('nurse-A').get());
    });

    test('create allows currentMatchId explicitly null or omitted', async () => {
      const dbA = getAuthDb('nurse-A');
      // Case 1: currentMatchId is explicitly null
      await assertSucceeds(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          currentMatchId: null,
        })
      );

      await testEnv.clearFirestore();

      // Case 2: currentMatchId is omitted entirely
      const dbB = getAuthDb('nurse-B');
      await assertSucceeds(
        dbB.collection('transferRequests').doc('nurse-B').set({
          ...validCreateData,
          firebaseUid: 'nurse-B',
        })
      );
    });

    test('create rejects forged currentMatchId', async () => {
      const dbA = getAuthDb('nurse-A');
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          currentMatchId: 'forged-match-999',
        })
      );
    });

    test('create rejects forged locked=true', async () => {
      const dbA = getAuthDb('nurse-A');
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          locked: true,
        })
      );
    });

    test('create rejects server-controlled status MATCHED or MATCH_PENDING', async () => {
      const dbA = getAuthDb('nurse-A');
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          status: 'MATCHED',
        })
      );
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          status: 'MATCH_PENDING',
        })
      );
    });

    test('create rejects spoofed firebaseUid or mismatching doc id', async () => {
      const dbA = getAuthDb('nurse-A');
      // Mismatching doc id
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-B').set(validCreateData)
      );
      // Mismatching payload firebaseUid
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          firebaseUid: 'nurse-B',
        })
      );
    });

    test('create validates required hospital and preference constraints', async () => {
      const dbA = getAuthDb('nurse-A');
      // Empty hospitalId
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          currentHospitalId: '',
        })
      );
      // 0 preferences
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          preferenceHospitalIds: [],
        })
      );
      // > 3 preferences
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          preferenceHospitalIds: ['H1', 'H2', 'H3', 'H4'],
        })
      );
      // Empty grade
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').set({
          ...validCreateData,
          grade: '',
        })
      );
    });

    test('update tolerates missing and null currentMatchId interchangeably', async () => {
      // Seed with currentMatchId: null
      await seedDoc('transferRequests/nurse-A', {
        ...validCreateData,
        currentMatchId: null,
      });

      const dbA = getAuthDb('nurse-A');
      // Update with currentMatchId omitted
      await assertSucceeds(
        dbA.collection('transferRequests').doc('nurse-A').update({
          currentHospitalId: 'HOSP-NEW',
        })
      );

      // Update with currentMatchId explicitly null
      await assertSucceeds(
        dbA.collection('transferRequests').doc('nurse-A').update({
          currentMatchId: null,
          grade: 'Grade II',
        })
      );
    });

    test('update rejects setting or forging currentMatchId', async () => {
      await seedDoc('transferRequests/nurse-A', {
        ...validCreateData,
        currentMatchId: null,
      });

      const dbA = getAuthDb('nurse-A');
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').update({
          currentMatchId: 'forged-match-001',
        })
      );
    });

    test('update rejects client setting locked=true or modifying locked request', async () => {
      await seedDoc('transferRequests/nurse-A', validCreateData);
      const dbA = getAuthDb('nurse-A');

      // Cannot set locked to true
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').update({
          locked: true,
        })
      );

      // If document is already locked on server, user cannot update it
      await seedDoc('transferRequests/nurse-A', {
        ...validCreateData,
        locked: true,
        currentMatchId: 'match-xyz',
      });
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').update({
          currentHospitalId: 'HOSP-MUTATED',
        })
      );
    });

    test('update allows status transition to WITHDRAWN, but rejects MATCHED / MATCH_PENDING', async () => {
      await seedDoc('transferRequests/nurse-A', validCreateData);
      const dbA = getAuthDb('nurse-A');

      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').update({
          status: 'MATCHED',
        })
      );
      await assertFails(
        dbA.collection('transferRequests').doc('nurse-A').update({
          status: 'MATCH_PENDING',
        })
      );
      await assertSucceeds(
        dbA.collection('transferRequests').doc('nurse-A').update({
          status: 'WITHDRAWN',
        })
      );
    });

    test('delete allowed only for own unlocked request', async () => {
      await seedDoc('transferRequests/nurse-A', validCreateData);
      await seedDoc('transferRequests/nurse-B', {
        ...validCreateData,
        firebaseUid: 'nurse-B',
      });
      await seedDoc('transferRequests/nurse-locked', {
        ...validCreateData,
        firebaseUid: 'nurse-locked',
        locked: true,
      });

      const dbA = getAuthDb('nurse-A');
      const dbLocked = getAuthDb('nurse-locked');

      // Cannot delete another user's request
      await assertFails(dbA.collection('transferRequests').doc('nurse-B').delete());
      // Cannot delete locked request
      await assertFails(dbLocked.collection('transferRequests').doc('nurse-locked').delete());
      // Can delete own unlocked request
      await assertSucceeds(dbA.collection('transferRequests').doc('nurse-A').delete());
    });
  });

  // -------------------------------------------------------------
  // 2. Matches Collection (/matches/{matchId})
  // -------------------------------------------------------------
  describe('Matches (/matches/{matchId})', () => {
    const twoWayMatch = {
      matchId: 'match-2way',
      status: 'PENDING_CONFIRMATION',
      nurseAUid: 'nurse-A',
      nurseBUid: 'nurse-B',
    };

    const threeWayMatch = {
      matchId: 'match-3way',
      status: 'CHAT_OPEN',
      nurseAUid: 'nurse-A',
      nurseBUid: 'nurse-B',
      nurseCUid: 'nurse-C',
    };

    test('client cannot create, update, or delete match documents directly', async () => {
      const dbA = getAuthDb('nurse-A');
      await assertFails(dbA.collection('matches').doc('match-1').set(twoWayMatch));
      await seedDoc('matches/match-2way', twoWayMatch);
      await assertFails(
        dbA.collection('matches').doc('match-2way').update({ status: 'CONFIRMED' })
      );
      await assertFails(dbA.collection('matches').doc('match-2way').delete());
    });

    test('collection list on matches is denied', async () => {
      await seedDoc('matches/match-2way', twoWayMatch);
      const dbA = getAuthDb('nurse-A');
      const unauth = getUnauthDb();
      await assertFails(dbA.collection('matches').get());
      await assertFails(unauth.collection('matches').get());
    });

    test('2-way match reads: participants can get, non-participants cannot', async () => {
      await seedDoc('matches/match-2way', twoWayMatch);

      const dbA = getAuthDb('nurse-A');
      const dbB = getAuthDb('nurse-B');
      const dbX = getAuthDb('nurse-X');
      const unauth = getUnauthDb();

      await assertSucceeds(dbA.collection('matches').doc('match-2way').get());
      await assertSucceeds(dbB.collection('matches').doc('match-2way').get());
      await assertFails(dbX.collection('matches').doc('match-2way').get());
      await assertFails(unauth.collection('matches').doc('match-2way').get());
    });

    test('3-way match reads: nurseCUid is recognized as authorized participant', async () => {
      await seedDoc('matches/match-3way', threeWayMatch);

      const dbA = getAuthDb('nurse-A');
      const dbB = getAuthDb('nurse-B');
      const dbC = getAuthDb('nurse-C');
      const dbX = getAuthDb('nurse-X');

      await assertSucceeds(dbA.collection('matches').doc('match-3way').get());
      await assertSucceeds(dbB.collection('matches').doc('match-3way').get());
      await assertSucceeds(dbC.collection('matches').doc('match-3way').get());
      await assertFails(dbX.collection('matches').doc('match-3way').get());
    });
  });

  // -------------------------------------------------------------
  // 3. Match Messages Subcollection (/matches/{matchId}/messages/{messageId})
  // -------------------------------------------------------------
  describe('Match Messages (/matches/{matchId}/messages/{messageId})', () => {
    const futureMs = Date.now() + 86400000; // +24 hours
    const pastMs = Date.now() - 3600000; // -1 hour

    const openMatch = {
      matchId: 'match-open',
      status: 'CHAT_OPEN',
      nurseAUid: 'nurse-A',
      nurseBUid: 'nurse-B',
      chatDeadlineMs: futureMs,
    };

    function validMessage(msgId = 'msg-1', senderUid = 'nurse-A') {
      return {
        messageId: msgId,
        matchId: 'match-open',
        senderUid,
        senderHospitalId: 'HOSP-001',
        senderGrade: 'Grade I',
        text: 'Hello, looking forward to the transfer!',
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      };
    }

    test('participants can read messages across match states; non-participants cannot', async () => {
      await seedDoc('matches/match-open', openMatch);
      await seedDoc('matches/match-open/messages/msg-1', {
        messageId: 'msg-1',
        matchId: 'match-open',
        senderUid: 'nurse-A',
        senderHospitalId: 'HOSP-001',
        senderGrade: 'Grade I',
        text: 'Hello',
        createdAt: new Date(),
      });

      const dbA = getAuthDb('nurse-A');
      const dbB = getAuthDb('nurse-B');
      const dbX = getAuthDb('nurse-X');
      const unauth = getUnauthDb();

      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').get()
      );
      await assertSucceeds(
        dbB.collection('matches').doc('match-open').collection('messages').doc('msg-1').get()
      );
      await assertFails(
        dbX.collection('matches').doc('match-open').collection('messages').doc('msg-1').get()
      );
      await assertFails(
        unauth.collection('matches').doc('match-open').collection('messages').doc('msg-1').get()
      );
    });

    test('message create requires CHAT_OPEN state', async () => {
      const states = ['PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'EXPIRED'];
      for (const st of states) {
        await seedDoc('matches/match-open', {
          ...openMatch,
          status: st,
        });
        const dbA = getAuthDb('nurse-A');
        await assertFails(
          dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage())
        );
      }

      // Now set to CHAT_OPEN -> succeeds
      await seedDoc('matches/match-open', openMatch);
      const dbA = getAuthDb('nurse-A');
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage())
      );
    });

    test('deadline enforcement: chatDeadlineMs', async () => {
      const dbA = getAuthDb('nurse-A');

      // Future deadline: succeeds
      await seedDoc('matches/match-open', { ...openMatch, chatDeadlineMs: futureMs });
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage('msg-1'))
      );

      // Past deadline: fails
      await seedDoc('matches/match-open', { ...openMatch, chatDeadlineMs: pastMs });
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-2').set(validMessage('msg-2'))
      );
    });

    test('deadline compatibility: falls back to expiresAtMs when chatDeadlineMs absent', async () => {
      const dbA = getAuthDb('nurse-A');

      // expiresAtMs in future
      await seedDoc('matches/match-open', {
        matchId: 'match-open',
        status: 'CHAT_OPEN',
        nurseAUid: 'nurse-A',
        nurseBUid: 'nurse-B',
        expiresAtMs: futureMs,
      });
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage('msg-1'))
      );

      // expiresAtMs in past
      await seedDoc('matches/match-open', {
        matchId: 'match-open',
        status: 'CHAT_OPEN',
        nurseAUid: 'nurse-A',
        nurseBUid: 'nurse-B',
        expiresAtMs: pastMs,
      });
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-2').set(validMessage('msg-2'))
      );
    });

    test('deadline compatibility: falls back to legacy ISO timestamp (chatDeadline / expiresAt)', async () => {
      const dbA = getAuthDb('nurse-A');
      const futureDate = new Date(Date.now() + 86400000);
      const pastDate = new Date(Date.now() - 3600000);

      // Legacy chatDeadline timestamp in future
      await seedDoc('matches/match-open', {
        matchId: 'match-open',
        status: 'CHAT_OPEN',
        nurseAUid: 'nurse-A',
        nurseBUid: 'nurse-B',
        chatDeadline: firebase.firestore.Timestamp.fromDate(futureDate),
      });
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage('msg-1'))
      );

      // Legacy chatDeadline timestamp in past
      await seedDoc('matches/match-open', {
        matchId: 'match-open',
        status: 'CHAT_OPEN',
        nurseAUid: 'nurse-A',
        nurseBUid: 'nurse-B',
        chatDeadline: firebase.firestore.Timestamp.fromDate(pastDate),
      });
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-2').set(validMessage('msg-2'))
      );

      // Legacy expiresAt timestamp in future
      await seedDoc('matches/match-open', {
        matchId: 'match-open',
        status: 'CHAT_OPEN',
        nurseAUid: 'nurse-A',
        nurseBUid: 'nurse-B',
        expiresAt: firebase.firestore.Timestamp.fromDate(futureDate),
      });
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-3').set(validMessage('msg-3'))
      );
    });

    test('message content validation: length, whitespace, allowed keys, and server timestamp', async () => {
      await seedDoc('matches/match-open', openMatch);
      const dbA = getAuthDb('nurse-A');

      // Empty text denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set({
          ...validMessage('msg-1'),
          text: '',
        })
      );

      // Whitespace-only text denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set({
          ...validMessage('msg-1'),
          text: '   \n  \t  ',
        })
      );

      // Multiline text with characters allowed
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set({
          ...validMessage('msg-1'),
          text: 'Line 1\nLine 2',
        })
      );

      // 500 characters allowed
      const text500 = 'A'.repeat(500);
      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-500').set({
          ...validMessage('msg-500'),
          text: text500,
        })
      );

      // 501 characters denied
      const text501 = 'A'.repeat(501);
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-501').set({
          ...validMessage('msg-501'),
          text: text501,
        })
      );

      // Spoofed senderUid
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-spoof').set({
          ...validMessage('msg-spoof'),
          senderUid: 'nurse-B',
        })
      );

      // Non-server arbitrary createdAt timestamp denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-time').set({
          ...validMessage('msg-time'),
          createdAt: new Date('2026-01-01T00:00:00Z'),
        })
      );

      // Extra unapproved key denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-extra').set({
          ...validMessage('msg-extra'),
          attachmentUrl: 'https://evil.com/img.png',
        })
      );
    });

    test('messages are strictly immutable (updates and deletes denied)', async () => {
      await seedDoc('matches/match-open', openMatch);
      const dbA = getAuthDb('nurse-A');

      await assertSucceeds(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').set(validMessage('msg-1'))
      );

      // Update denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').update({
          text: 'Edited text',
        })
      );

      // Delete denied
      await assertFails(
        dbA.collection('matches').doc('match-open').collection('messages').doc('msg-1').delete()
      );
    });
  });

  // -------------------------------------------------------------
  // 4. Transfer Identities & Profiles
  // -------------------------------------------------------------
  describe('Transfer Identities and Profiles', () => {
    test('transferIdentities: user can read and create own SELF_DECLARED identity without verified fields', async () => {
      const dbA = getAuthDb('nurse-A');
      const dbB = getAuthDb('nurse-B');

      await assertSucceeds(
        dbA.collection('transferIdentities').doc('nurse-A').set({
          firebaseUid: 'nurse-A',
          verificationMethod: 'SELF_DECLARED',
          phone: '+94771234567',
        })
      );

      // User A can read own
      await assertSucceeds(dbA.collection('transferIdentities').doc('nurse-A').get());
      // User B cannot read user A's identity
      await assertFails(dbB.collection('transferIdentities').doc('nurse-A').get());

      // Cannot forge verifiedServiceNo
      await assertFails(
        dbA.collection('transferIdentities').doc('nurse-A-2').set({
          firebaseUid: 'nurse-A',
          verificationMethod: 'SELF_DECLARED',
          verifiedServiceNo: 'SN12345',
        })
      );

      // Cannot self-promote verificationMethod
      await assertFails(
        dbA.collection('transferIdentities').doc('nurse-A-3').set({
          firebaseUid: 'nurse-A',
          verificationMethod: 'VERIFIED',
        })
      );
    });

    test('transferProfiles: direct client read and write are completely denied', async () => {
      const dbA = getAuthDb('nurse-A');
      const unauth = getUnauthDb();

      await assertFails(dbA.collection('transferProfiles').doc('SN12345').get());
      await assertFails(unauth.collection('transferProfiles').doc('SN12345').get());
      await assertFails(
        dbA.collection('transferProfiles').doc('SN12345').set({ serviceNo: 'SN12345' })
      );
    });

    test('unhandled collections denied by default wildcard', async () => {
      const dbA = getAuthDb('nurse-A');
      await assertFails(dbA.collection('nonExistentCollection').doc('item1').get());
      await assertFails(
        dbA.collection('nonExistentCollection').doc('item1').set({ secret: true })
      );
    });
  });
});
