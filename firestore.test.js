const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_mokay_123";
const BOB_UID = "bob_punk_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function makeValidSavePayload(uid, createdDate, updatedDate) {
  return {
    userId: uid,
    slotLabel: "Guardado Rápido",
    heroName: "Boxy",
    headgearId: "mokay_cap_classic",
    jacketId: "mokay_hoodie_flame",
    pantsShoesId: "mokay_kicks_street",
    backpackId: "mokay_pack_original",
    activeSkullId: "boxy_classic",
    secondarySkullId: "boxy_flame",
    unlockedSkulls: ["boxy_classic", "boxy_flame", "boxy_tank"],
    equippedRelics: ["rel_gold_match", "rel_dash_boots"],
    heroLevel: 3,
    heroXp: 45,
    statPoints: 1,
    atkBonus: 1,
    hpBonus: 1,
    spdBonus: 0,
    weaponLevel: 2,
    shieldLevel: 1,
    worldSeed: 202606,
    playerX: 420.0,
    playerY: 468.0,
    currentZone: "BOSQUE_ESMERALDA",
    currentRoomIndex: 1,
    roomsClearedCount: 2,
    skullSwapsCount: 5,
    sparksCurrency: 180,
    totalEnemiesDefeated: 8,
    molotovBossDefeated: false,
    playTimeSeconds: 240,
    equippedPets: ["mokay", "bloom", "birdy"],
    unlockedPets: ["mokay", "bloom", "birdy"],
    unlockedCosmetics: [
      "mokay_cap_classic",
      "mokay_hoodie_flame",
      "mokay_kicks_street",
      "mokay_pack_original"
    ],
    unlockedGestures: ["TAP_STRIKE", "SWIPE_DASH", "CIRCLE_NOVA", "Z_SLASH"],
    completedMissionIds: ["m_first_clear", "m_swap_skulls"],
    claimedMissionIds: ["m_first_clear"],
    inventoryCounts: { food_apple: 3, drink_oasis: 2 },
    createdAt: createdDate,
    updatedAt: updatedDate,
  };
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

test("Unauthenticated user cannot list, read, or write save slots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("save_slots").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("save_slots")
      .doc("quicksave")
      .set(makeValidSavePayload(ALICE_UID, now, now))
  );
});

test("Authenticated owner can create, update, list, and delete Boxy Skul save slots", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const slotRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("save_slots")
    .doc("quicksave");

  await assertSucceeds(slotRef.set(makeValidSavePayload(ALICE_UID, created, created)));

  await assertSucceeds(
    slotRef.update({
      playerX: 1200.5,
      playerY: 468.0,
      currentZone: "BARRIO_SOMBRAVIA",
      currentRoomIndex: 2,
      activeSkullId: "boxy_birdy",
      secondarySkullId: "boxy_tank",
      weaponLevel: 3,
      molotovBossDefeated: true,
      equippedPets: ["mokay", "birdy", "zenkay"],
      sparksCurrency: 420,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").get()
  );

  await assertSucceeds(slotRef.delete());
});

test("Cross-user access is strictly denied", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("save_slots")
      .doc("quicksave")
      .set(makeValidSavePayload(BOB_UID, created, created));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("save_slots").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("save_slots")
      .doc("quicksave")
      .update({ sparksCurrency: 9999, updatedAt: new Date(Date.now() - 1000) })
  );
});

test("Rejects @DocumentId injection, >3 pets, >6 relics, invalid zone, and immutable createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const withId = makeValidSavePayload(ALICE_UID, created, created);
  withId.id = "quicksave";
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").doc("quicksave").set(withId)
  );

  const tooManyPets = makeValidSavePayload(ALICE_UID, created, created);
  tooManyPets.equippedPets = ["mokay", "bloom", "birdy", "zenkay"];
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").doc("slot_1").set(tooManyPets)
  );

  const tooManyRelics = makeValidSavePayload(ALICE_UID, created, created);
  tooManyRelics.equippedRelics = ["r1", "r2", "r3", "r4", "r5", "r6", "r7"];
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").doc("slot_1").set(tooManyRelics)
  );

  const badZone = makeValidSavePayload(ALICE_UID, created, created);
  badZone.currentZone = "INVALID_ZONE";
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").doc("slot_2").set(badZone)
  );

  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").doc("quicksave");
  await assertSucceeds(ref.set(makeValidSavePayload(ALICE_UID, created, created)));

  await assertFails(
    ref.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
