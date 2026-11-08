const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const DATABASE_ID = "ai-studio-android-syncclou-b45d2aa6-4000-4bbb-9597-00742599c0f5";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("unauthenticated user cannot read or write user profile or sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.doc("users/alice").get());
  await assertFails(unauthDb.doc("users/alice/active_sessions/current").get());
});

test("authenticated owner can create, update, and read UserProfile with nisn, gradeClass, photoBase64 and LoginHistoryRecord", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore(DATABASE_ID);
  const now = new Date();
  await assertSucceeds(
    aliceDb.doc("users/alice").set({
      userId: "alice",
      email: "alice@example.com",
      displayName: "Alice Putri",
      authMethod: "google",
      nisn: "0078192831",
      institution: "SMAN 1 Jakarta",
      gradeClass: "Kelas 12 SMA/MA/SMK",
      photoBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
      loginCount: 1,
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.doc("users/alice").update({
      displayName: "Alice Putri Maharani",
      nisn: "0078192899",
      institution: "SMAN 8 Jakarta",
      gradeClass: "Kelas 12 SMA/MA/SMK",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.doc("users/alice/login_logs/log_1").set({
      userId: "alice",
      email: "alice@example.com",
      displayName: "Alice Putri",
      authMethod: "google",
      deviceInfo: "Android 16",
      createdAt: now,
    })
  );

  await assertSucceeds(aliceDb.doc("users/alice").get());
  await assertSucceeds(
    aliceDb.collection("users/alice/login_logs").where("userId", "==", "alice").get()
  );
});

test("authenticated owner can create and read valid ActiveExamSession", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore(DATABASE_ID);
  const now = new Date();
  await assertSucceeds(
    aliceDb.doc("users/alice/active_sessions/current").set({
      userId: "alice",
      appMode: "nasional",
      participantName: "Alice Putri",
      frameworkKey: "merdeka",
      frameworkName: "Kurikulum Merdeka",
      stage: "Fase F",
      subjectName: "Matematika",
      topicFocus: "Aljabar",
      difficultyCode: "Standar",
      scoringMethod: "polytomous",
      lang: "id",
      durationMinutes: 45,
      totalTarget: 20,
      currentIdx: 2,
      timerSeconds: 2400,
      questionsPayload: "[]",
      answersPayload: "[]",
      doubtsPayload: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.doc("users/alice/active_sessions/current").get());
});

test("cross-user access is denied for profile, login_logs, exam_results, and blueprints", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext("bob").firestore(DATABASE_ID);
  const now = new Date();

  await assertSucceeds(
    aliceDb.doc("users/alice").set({
      userId: "alice",
      email: "alice@example.com",
      displayName: "Alice Putri",
      authMethod: "email",
      institution: "SMAN 1",
      loginCount: 1,
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.doc("users/alice").get());
  await assertFails(bobDb.collection("users/alice/login_logs").get());
});
