const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

initializeApp();

const db = getFirestore();

function requireNonEmptyString(value, field) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${field} is required`);
  }
  return value.trim();
}

function requireOptionalString(value, field) {
  if (value == null) return null;
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${field} must be a string or null`);
  }
  return value.trim();
}

function requireNonNegativeInteger(value, field) {
  if (!Number.isInteger(value) || value < 0) {
    throw new HttpsError("invalid-argument", `${field} must be a non-negative integer`);
  }
  return value;
}

/**
 * Trusted publication boundary.
 *
 * Security invariant:
 * - Firebase Authentication identifies the caller.
 * - The caller can never create VERIFIED identity state.
 * - verifiedServiceNo/fullName/grade are taken from the authoritative
 *   verification record, never trusted from the callable payload.
 * - transferProfiles is written only by this privileged server path.
 */
exports.publishVerifiedTransferProfile = onCall(
  { region: "asia-south1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Firebase Authentication is required");
    }

    const uid = request.auth.uid;
    const input = request.data?.profile;

    if (!input || typeof input !== "object") {
      throw new HttpsError("invalid-argument", "profile is required");
    }

    const identityRef = db.collection("transferIdentities").doc(uid);
    const identitySnapshot = await identityRef.get();

    if (!identitySnapshot.exists) {
      throw new HttpsError("permission-denied", "Transfer identity is missing");
    }

    const identity = identitySnapshot.data() || {};

    if (identity.verificationMethod !== "VERIFIED") {
      throw new HttpsError(
        "permission-denied",
        "Transfer profile publication requires authoritative verification"
      );
    }

    const serviceNo = requireNonEmptyString(
      identity.verifiedServiceNo,
      "verifiedServiceNo"
    );
    const fullName = requireNonEmptyString(
      identity.verifiedFullName,
      "verifiedFullName"
    );
    const grade = requireNonEmptyString(
      identity.verifiedGrade,
      "verifiedGrade"
    );

    const requestedServiceNo = requireNonEmptyString(input.serviceNo, "serviceNo");
    if (requestedServiceNo !== serviceNo) {
      throw new HttpsError(
        "permission-denied",
        "Profile service number does not match the verified identity"
      );
    }

    const currentHospitalId = requireNonEmptyString(
      input.currentHospitalId,
      "currentHospitalId"
    );
    const cadre = requireNonEmptyString(input.cadre, "cadre");
    const postingDate = requireOptionalString(input.postingDate, "postingDate");
    const yearsOfService = requireNonNegativeInteger(
      input.yearsOfService,
      "yearsOfService"
    );
    const batchYear =
      input.batchYear == null
        ? null
        : requireNonNegativeInteger(input.batchYear, "batchYear");

    const transferScore =
      input.transferScore == null
        ? 100
        : requireNonNegativeInteger(input.transferScore, "transferScore");

    const contactVerified = input.contactVerified === true;

    const profileRef = db.collection("transferProfiles").doc(serviceNo);

    await db.runTransaction(async (transaction) => {
      const existing = await transaction.get(profileRef);

      const payload = {
        serviceNo,
        fullName,
        grade,
        cadre,
        currentHospitalId,
        postingDate,
        yearsOfService,
        batchYear,
        transferScore,
        contactVerified,
        firebaseUid: uid,
        updatedAt: FieldValue.serverTimestamp()
      };

      if (!existing.exists) {
        transaction.create(profileRef, payload);
      } else {
        transaction.update(profileRef, payload);
      }
    });

    return {
      published: true,
      serviceNo
    };
  }
);
