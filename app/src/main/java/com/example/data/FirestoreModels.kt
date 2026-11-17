package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class SaveSlot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val slotLabel: String = "Guardado Rápido",
    val heroName: String = "Boxy",
    val headgearId: String = "mokay_cap_classic",
    val jacketId: String = "mokay_hoodie_flame",
    val pantsShoesId: String = "mokay_kicks_street",
    val backpackId: String = "mokay_pack_original",
    val activeSkullId: String = "boxy_classic",
    val secondarySkullId: String = "boxy_flame",
    val unlockedSkulls: List<String> = listOf("boxy_classic", "boxy_flame"),
    val equippedRelics: List<String> = emptyList(),
    val heroLevel: Long = 1L,
    val heroXp: Long = 0L,
    val statPoints: Long = 1L,
    val atkBonus: Long = 0L,
    val hpBonus: Long = 0L,
    val spdBonus: Long = 0L,
    val weaponLevel: Long = 1L,
    val shieldLevel: Long = 1L,
    val worldSeed: Long = 202606L,
    val playerX: Double = 220.0,
    val playerY: Double = 460.0,
    val currentZone: String = "ALDEA_MOKAY",
    val currentRoomIndex: Long = 0L,
    val roomsClearedCount: Long = 0L,
    val skullSwapsCount: Long = 0L,
    val sparksCurrency: Long = 120L,
    val totalEnemiesDefeated: Long = 0L,
    val molotovBossDefeated: Boolean = false,
    val playTimeSeconds: Long = 0L,
    val equippedPets: List<String> = listOf("mokay"),
    val unlockedPets: List<String> = listOf("mokay"),
    val unlockedCosmetics: List<String> = listOf(
        "mokay_cap_classic",
        "mokay_hoodie_flame",
        "mokay_kicks_street",
        "mokay_pack_original"
    ),
    val unlockedGestures: List<String> = listOf("TAP_STRIKE", "SWIPE_DASH", "DIAGONAL_SLASH", "UP_SLAM", "DOWN_PARRY"),
    val completedMissionIds: List<String> = emptyList(),
    val claimedMissionIds: List<String> = emptyList(),
    val inventoryCounts: Map<String, Long> = mapOf(
        "food_apple" to 3L,
        "drink_oasis" to 3L
    ),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "slotLabel" to slotLabel.take(40).ifBlank { "Guardado Rápido" },
            "heroName" to heroName.take(20).ifBlank { "Boxy" },
            "headgearId" to headgearId,
            "jacketId" to jacketId,
            "pantsShoesId" to pantsShoesId,
            "backpackId" to backpackId,
            "activeSkullId" to activeSkullId,
            "secondarySkullId" to secondarySkullId,
            "unlockedSkulls" to unlockedSkulls.take(10),
            "equippedRelics" to equippedRelics.take(6),
            "heroLevel" to heroLevel.coerceIn(1L, 99L),
            "heroXp" to heroXp.coerceIn(0L, 999999L),
            "statPoints" to statPoints.coerceIn(0L, 999L),
            "atkBonus" to atkBonus.coerceIn(0L, 999L),
            "hpBonus" to hpBonus.coerceIn(0L, 999L),
            "spdBonus" to spdBonus.coerceIn(0L, 999L),
            "weaponLevel" to weaponLevel.coerceIn(1L, 5L),
            "shieldLevel" to shieldLevel.coerceIn(1L, 5L),
            "worldSeed" to worldSeed.coerceIn(1L, 999999999L),
            "playerX" to playerX.coerceIn(0.0, 10000.0),
            "playerY" to playerY.coerceIn(0.0, 10000.0),
            "currentZone" to currentZone,
            "currentRoomIndex" to currentRoomIndex.coerceIn(0L, 99L),
            "roomsClearedCount" to roomsClearedCount.coerceIn(0L, 9999L),
            "skullSwapsCount" to skullSwapsCount.coerceIn(0L, 99999L),
            "sparksCurrency" to sparksCurrency.coerceIn(0L, 9999999L),
            "totalEnemiesDefeated" to totalEnemiesDefeated.coerceIn(0L, 9999999L),
            "molotovBossDefeated" to molotovBossDefeated,
            "playTimeSeconds" to playTimeSeconds.coerceIn(0L, 99999999L),
            "equippedPets" to equippedPets.take(3),
            "unlockedPets" to unlockedPets.take(10),
            "unlockedCosmetics" to unlockedCosmetics.take(30),
            "unlockedGestures" to unlockedGestures.take(20),
            "completedMissionIds" to completedMissionIds.take(20),
            "claimedMissionIds" to claimedMissionIds.take(20),
            "inventoryCounts" to inventoryCounts,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "slotLabel" to slotLabel.take(40).ifBlank { "Guardado Rápido" },
            "heroName" to heroName.take(20).ifBlank { "Boxy" },
            "headgearId" to headgearId,
            "jacketId" to jacketId,
            "pantsShoesId" to pantsShoesId,
            "backpackId" to backpackId,
            "activeSkullId" to activeSkullId,
            "secondarySkullId" to secondarySkullId,
            "unlockedSkulls" to unlockedSkulls.take(10),
            "equippedRelics" to equippedRelics.take(6),
            "heroLevel" to heroLevel.coerceIn(1L, 99L),
            "heroXp" to heroXp.coerceIn(0L, 999999L),
            "statPoints" to statPoints.coerceIn(0L, 999L),
            "atkBonus" to atkBonus.coerceIn(0L, 999L),
            "hpBonus" to hpBonus.coerceIn(0L, 999L),
            "spdBonus" to spdBonus.coerceIn(0L, 999L),
            "weaponLevel" to weaponLevel.coerceIn(1L, 5L),
            "shieldLevel" to shieldLevel.coerceIn(1L, 5L),
            "worldSeed" to worldSeed.coerceIn(1L, 999999999L),
            "playerX" to playerX.coerceIn(0.0, 10000.0),
            "playerY" to playerY.coerceIn(0.0, 10000.0),
            "currentZone" to currentZone,
            "currentRoomIndex" to currentRoomIndex.coerceIn(0L, 99L),
            "roomsClearedCount" to roomsClearedCount.coerceIn(0L, 9999L),
            "skullSwapsCount" to skullSwapsCount.coerceIn(0L, 99999L),
            "sparksCurrency" to sparksCurrency.coerceIn(0L, 9999999L),
            "totalEnemiesDefeated" to totalEnemiesDefeated.coerceIn(0L, 9999999L),
            "molotovBossDefeated" to molotovBossDefeated,
            "playTimeSeconds" to playTimeSeconds.coerceIn(0L, 99999999L),
            "equippedPets" to equippedPets.take(3),
            "unlockedPets" to unlockedPets.take(10),
            "unlockedCosmetics" to unlockedCosmetics.take(30),
            "unlockedGestures" to unlockedGestures.take(20),
            "completedMissionIds" to completedMissionIds.take(20),
            "claimedMissionIds" to claimedMissionIds.take(20),
            "inventoryCounts" to inventoryCounts,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
