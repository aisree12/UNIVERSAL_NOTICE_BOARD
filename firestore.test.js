const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const crypto = require("node:crypto");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DEAN_A = "dean_alpha_uid";
const DEAN_B = "dean_beta_uid";
const STAFF_A = "staff_alpha_uid";
const STUDENT_A = "student_alpha_uid";
const LEADER_A = "leader_alpha_uid";
const OUTSIDER = "outsider_uid";

const INST_A = "NB-ALPHA-7K4P2";
const INST_B = "NB-BETA-9M2X4";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256(input) {
  return crypto.createHash("sha256").update(input).digest("hex");
}

function nowPast() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
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

async function seedInstitution(instId, deanUid, passSecret = "CAMPUS2026", confidentialEnabled = true) {
  const passHash = sha256(`${instId}:${passSecret}`);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = nowPast();
    await db.collection("institution_codes").doc(instId).set({
      institutionId: instId,
      name: `Institute ${instId}`,
      tagline: "Official Campus Board",
      accentColorKey: "INDIGO",
      deanUid,
      studentPassEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("institutions").doc(instId).set({
      institutionId: instId,
      name: `Institute ${instId}`,
      tagline: "Official Campus Board",
      contactEmail: "dean@campus.edu",
      contactPhone: "+1555000111",
      accentColorKey: "INDIGO",
      deanUid,
      deanName: "Dr. Dean",
      studentPassEnabled: true,
      studentPassHash: passHash,
      confidentialFeedbackEnabled: confidentialEnabled,
      driveConnected: false,
      driveAccountEmail: "",
      driveFolderName: "",
      driveFolderId: "",
      departments: ["CSE", "ECE", "ME"],
      categories: ["Circular", "Exam", "Emergency"],
      priorities: ["EMERGENCY", "URGENT", "NORMAL"],
      activityTypes: ["Technical", "Cultural"],
      sportTypes: ["Cricket", "Football"],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("institutions").doc(instId).collection("memberships").doc(deanUid).set({
      uid: deanUid,
      institutionId: instId,
      displayName: "Dr. Dean",
      email: `${deanUid}@campus.edu`,
      designation: "Dean",
      department: "Administration",
      role: "DEAN",
      permissions: ["CREATE_NOTICE", "EDIT_NOTICE", "MANAGE_USERS", "VIEW_CONFIDENTIAL_FEEDBACK", "VIEW_AUDIT"],
      scopedDepartments: [],
      status: "ACTIVE",
      joinMethod: "DEAN_FOUNDER",
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated users cannot read or enumerate any institution data", async () => {
  await seedInstitution(INST_A, DEAN_A);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("institution_codes").doc(INST_A).get());
  await assertFails(unauthDb.collection("institution_codes").get());
  await assertFails(unauthDb.collection("institutions").doc(INST_A).get());
  await assertFails(unauthDb.collection("institutions").doc(INST_A).collection("notices").get());
});

test("Institution enumeration (list) is strictly blocked even for authenticated users", async () => {
  await seedInstitution(INST_A, DEAN_A);
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER).firestore();
  await assertFails(outsiderDb.collection("institution_codes").get());
  await assertFails(outsiderDb.collection("institutions").get());
  // Exact lookup by ID returns only safe public code metadata
  await assertSucceeds(outsiderDb.collection("institution_codes").doc(INST_A).get());
  // Private institution doc still denied to non-members
  await assertFails(outsiderDb.collection("institutions").doc(INST_A).get());
});

test("Dean-first atomic registration succeeds; non-Dean privilege escalation fails", async () => {
  const deanDb = testEnv.authenticatedContext(DEAN_A, { email: "dean@alpha.edu", email_verified: true }).firestore();
  const ts = nowPast();
  const batch = deanDb.batch();
  batch.set(deanDb.collection("institution_codes").doc(INST_A), {
    institutionId: INST_A,
    name: "Alpha Engineering College",
    tagline: "Excellence",
    accentColorKey: "INDIGO",
    deanUid: DEAN_A,
    studentPassEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(deanDb.collection("institutions").doc(INST_A), {
    institutionId: INST_A,
    name: "Alpha Engineering College",
    tagline: "Excellence",
    contactEmail: "dean@alpha.edu",
    accentColorKey: "INDIGO",
    deanUid: DEAN_A,
    deanName: "Dean Alpha",
    studentPassEnabled: true,
    studentPassHash: sha256(`${INST_A}:PASS123`),
    confidentialFeedbackEnabled: true,
    driveConnected: false,
    departments: ["CSE", "ECE"],
    categories: ["Circular", "Exam"],
    priorities: ["EMERGENCY", "URGENT", "NORMAL"],
    activityTypes: ["Technical"],
    sportTypes: ["Cricket"],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(deanDb.collection("institutions").doc(INST_A).collection("memberships").doc(DEAN_A), {
    uid: DEAN_A,
    institutionId: INST_A,
    displayName: "Dean Alpha",
    email: "dean@alpha.edu",
    role: "DEAN",
    permissions: ["CREATE_NOTICE", "MANAGE_USERS"],
    scopedDepartments: [],
    status: "ACTIVE",
    joinMethod: "DEAN_FOUNDER",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Outsider trying to self-register as DEAN or ADMIN on INST_A must fail
  const attackerDb = testEnv.authenticatedContext(OUTSIDER, { email: "evil@alpha.edu", email_verified: true }).firestore();
  await assertFails(
    attackerDb.collection("institutions").doc(INST_A).collection("memberships").doc(OUTSIDER).set({
      uid: OUTSIDER,
      institutionId: INST_A,
      displayName: "Fake Dean",
      email: "evil@alpha.edu",
      role: "DEAN",
      permissions: ["MANAGE_USERS"],
      scopedDepartments: [],
      status: "ACTIVE",
      joinMethod: "DEAN_FOUNDER",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Student entry pass validates hash server-side and denies wrong passcodes", async () => {
  await seedInstitution(INST_A, DEAN_A, "ALPHA99");
  const studentDb = testEnv.authenticatedContext(STUDENT_A, { email: "stu@alpha.edu", email_verified: true }).firestore();
  const ts = nowPast();

  // Wrong pass hash fails
  await assertFails(
    studentDb.collection("institutions").doc(INST_A).collection("memberships").doc(STUDENT_A).set({
      uid: STUDENT_A,
      institutionId: INST_A,
      displayName: "Student One",
      email: "stu@alpha.edu",
      role: "STUDENT",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK"],
      scopedDepartments: [],
      status: "ACTIVE",
      joinMethod: "STUDENT_PASS",
      passHashProof: sha256(`${INST_A}:WRONGCODE`),
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Correct pass hash succeeds
  await assertSucceeds(
    studentDb.collection("institutions").doc(INST_A).collection("memberships").doc(STUDENT_A).set({
      uid: STUDENT_A,
      institutionId: INST_A,
      displayName: "Student One",
      email: "stu@alpha.edu",
      role: "STUDENT",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK"],
      scopedDepartments: [],
      status: "ACTIVE",
      joinMethod: "STUDENT_PASS",
      passHashProof: sha256(`${INST_A}:ALPHA99`),
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Invited Staff & Student Leader join with verified email; cross-tenant isolation holds", async () => {
  await seedInstitution(INST_A, DEAN_A);
  await seedInstitution(INST_B, DEAN_B);
  const ts = nowPast();

  const deanADb = testEnv.authenticatedContext(DEAN_A).firestore();
  await assertSucceeds(
    deanADb.collection("institutions").doc(INST_A).collection("invites").doc("leader@alpha.edu").set({
      email: "leader@alpha.edu",
      institutionId: INST_A,
      displayName: "Campus Leader",
      designation: "Cultural Secretary",
      department: "CSE",
      role: "STUDENT_LEADER",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK", "MANAGE_EVENTS"],
      scopedDepartments: ["CSE"],
      status: "PENDING",
      invitedByUid: DEAN_A,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unverified email spoof rejected
  const unverifiedDb = testEnv.authenticatedContext(LEADER_A, { email: "leader@alpha.edu", email_verified: false }).firestore();
  await assertFails(
    unverifiedDb.collection("institutions").doc(INST_A).collection("memberships").doc(LEADER_A).set({
      uid: LEADER_A,
      institutionId: INST_A,
      displayName: "Campus Leader",
      email: "leader@alpha.edu",
      role: "STUDENT_LEADER",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK", "MANAGE_EVENTS"],
      scopedDepartments: ["CSE"],
      status: "ACTIVE",
      joinMethod: "INVITE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified invited user succeeds with exact role/permissions
  const leaderDb = testEnv.authenticatedContext(LEADER_A, { email: "leader@alpha.edu", email_verified: true }).firestore();
  await assertSucceeds(
    leaderDb.collection("institutions").doc(INST_A).collection("memberships").doc(LEADER_A).set({
      uid: LEADER_A,
      institutionId: INST_A,
      displayName: "Campus Leader",
      email: "leader@alpha.edu",
      role: "STUDENT_LEADER",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK", "MANAGE_EVENTS"],
      scopedDepartments: ["CSE"],
      status: "ACTIVE",
      joinMethod: "INVITE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-tenant isolation: Institution A leader or Dean cannot read Institution B notices/members
  await assertFails(leaderDb.collection("institutions").doc(INST_B).collection("notices").get());
  await assertFails(deanADb.collection("institutions").doc(INST_B).get());
  await assertFails(deanADb.collection("institutions").doc(INST_B).collection("notices").get());
});

test("Confidential feedback respects Dean toggle & restricts non-authorized reads", async () => {
  await seedInstitution(INST_A, DEAN_A, "CAMPUS2026", false); // confidential disabled
  const ts = nowPast();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("institutions").doc(INST_A).collection("memberships").doc(STUDENT_A).set({
      uid: STUDENT_A,
      institutionId: INST_A,
      displayName: "Student A",
      email: "a@alpha.edu",
      role: "STUDENT",
      permissions: ["READ_NOTICE", "VIEW_EVENTS", "SUBMIT_FEEDBACK"],
      scopedDepartments: [],
      status: "ACTIVE",
      joinMethod: "STUDENT_PASS",
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("institutions").doc(INST_A).collection("memberships").doc(STAFF_A).set({
      uid: STAFF_A,
      institutionId: INST_A,
      displayName: "Staff A",
      email: "staff@alpha.edu",
      role: "STAFF",
      permissions: ["CREATE_NOTICE", "VIEW_FEEDBACK"],
      scopedDepartments: ["CSE"],
      status: "ACTIVE",
      joinMethod: "INVITE",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const stuDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_A).firestore();
  const deanDb = testEnv.authenticatedContext(DEAN_A).firestore();

  // Confidential disabled -> confidential submit fails, normal submit succeeds
  await assertFails(
    stuDb.collection("institutions").doc(INST_A).collection("feedback").doc("fb_conf").set({
      institutionId: INST_A,
      authorUid: STUDENT_A,
      authorName: "Student A",
      confidential: true,
      category: "Safety",
      subject: "Blocked confidential",
      message: "Should be blocked when disabled",
      status: "SUBMITTED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    stuDb.collection("institutions").doc(INST_A).collection("feedback").doc("fb_norm").set({
      institutionId: INST_A,
      authorUid: STUDENT_A,
      authorName: "Student A",
      confidential: false,
      category: "Facilities",
      subject: "Library hours",
      message: "Please extend library hours during exams.",
      status: "SUBMITTED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular staff with VIEW_FEEDBACK can query normal feedback, NOT confidential
  await assertSucceeds(
    staffDb.collection("institutions").doc(INST_A).collection("feedback").where("confidential", "==", false).get()
  );
  await assertFails(
    staffDb.collection("institutions").doc(INST_A).collection("feedback").where("confidential", "==", true).get()
  );

  // Dean with VIEW_CONFIDENTIAL_FEEDBACK can query confidential
  await assertSucceeds(
    deanDb.collection("institutions").doc(INST_A).collection("feedback").where("confidential", "==", true).get()
  );
});
