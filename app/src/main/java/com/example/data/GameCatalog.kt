package com.example.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.BirdySky
import com.example.ui.theme.BloomPink
import com.example.ui.theme.DinoEmerald
import com.example.ui.theme.FlokoOrange
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroMint

enum class WorldZone(
    val id: String,
    val title: String,
    val subtitle: String,
    val skyTop: Color,
    val skyBottom: Color,
    val accentColor: Color
) {
    ALDEA_MOKAY(
        id = "ALDEA_MOKAY",
        title = "Campamento Aldea",
        subtitle = "Refugio de Boxy · Prepara tu Run",
        skyTop = Color(0xFF1D2442),
        skyBottom = Color(0xFF3E4A73),
        accentColor = MokayFlame
    ),
    BOSQUE_ESMERALDA(
        id = "BOSQUE_ESMERALDA",
        title = "Acto 1 · Bosque",
        subtitle = "Ramas, Cabañas y Laughing Cats",
        skyTop = Color(0xFF142826),
        skyBottom = Color(0xFF28543F),
        accentColor = DinoEmerald
    ),
    DESIERTO_DORADO(
        id = "DESIERTO_DORADO",
        title = "Acto 2 · Desierto",
        subtitle = "Ruinas y Patrullas Street Dino",
        skyTop = Color(0xFF3A2322),
        skyBottom = Color(0xFF7D4B33),
        accentColor = MokaySpark
    ),
    CAVERNAS_MINA(
        id = "CAVERNAS_MINA",
        title = "Acto 3 · Minas",
        subtitle = "Cristales y Molotov Punks",
        skyTop = Color(0xFF15182B),
        skyBottom = Color(0xFF2A2E4D),
        accentColor = MokayLavender
    ),
    BARRIO_SOMBRAVIA(
        id = "BARRIO_SOMBRAVIA",
        title = "Acto 4 · Sombravía",
        subtitle = "Ciudadela de Molotov Mokay",
        skyTop = Color(0xFF190A24),
        skyBottom = Color(0xFF3B1545),
        accentColor = MolotovCrimson
    );

    companion object {
        fun byId(id: String): WorldZone = entries.firstOrNull { it.id == id } ?: ALDEA_MOKAY
        fun chapterOrder(): List<WorldZone> = listOf(
            ALDEA_MOKAY, BOSQUE_ESMERALDA, DESIERTO_DORADO, CAVERNAS_MINA, BARRIO_SOMBRAVIA
        )
    }
}

enum class TileKind(
    val id: Int,
    val displayName: String,
    val solid: Boolean,
    val oneWayPlatform: Boolean,
    val colorPrimary: Color,
    val colorSecondary: Color,
    val interactKind: String? = null // "CHEST", "CRATE", "BED", "KITCHEN", "TV", "ALTAR"
) {
    AIR(0, "Aire", false, false, Color.Transparent, Color.Transparent),
    SOLID_BRICK(1, "Suelo Piedra", true, false, Color(0xFF3E4259), Color(0xFF565C7A)),
    GRASS_TOP(2, "Césped Aldea", true, false, Color(0xFF4C9643), Color(0xFF6B4829)),
    SAND_STONE(3, "Arenisca", true, false, Color(0xFFD1A45A), Color(0xFF9E6E36)),
    SHADOW_FLOOR(4, "Piso Sombrío", true, false, Color(0xFF321D45), Color(0xFF5D2D7A)),
    PLATFORM_WOOD(5, "Plataforma", false, true, Color(0xFFA66F41), Color(0xFF6E4726)),
    CRATE(6, "Caja Botín", false, false, Color(0xFFB87F46), Color(0xFF7D5029), "CRATE"),
    CHEST(7, "Cofre Skul", false, false, MokaySpark, Color(0xFF8C5A32), "CHEST"),
    BED(8, "Cama Boxy", false, false, MokayFlame, MokayCream, "BED"),
    KITCHEN(9, "Cocina Mokay", false, false, Color(0xFFE6EDF2), MokayFlame, "KITCHEN"),
    TV(10, "TV Mokayverse", false, false, Color(0xFF333952), BirdySky, "TV"),
    SKULL_ALTAR(11, "Altar Cabezas", false, false, MokayLavender, MokaySpark, "ALTAR"),
    TORCH(12, "Antorcha", false, false, MokayFlame, MokaySpark);

    companion object {
        fun byId(id: Int): TileKind = entries.firstOrNull { it.id == id } ?: AIR
    }
}

data class SkullFormDef(
    val id: String,
    val name: String,
    val tierLabel: String,
    val styleType: String, // "Equilibrio", "Velocidad", "Poder", "Técnica"
    val desc: String,
    val skill1Name: String,
    val skill1Desc: String,
    val skill1CdMs: Long,
    val skill2Name: String,
    val skill2Desc: String,
    val skill2CdMs: Long,
    val swapSkillName: String,
    val swapSkillDesc: String,
    val costSparks: Int,
    val primaryColor: Color,
    val accentColor: Color,
    @DrawableRes val portraitRes: Int,
    val speedMult: Float = 1f,
    val atkMult: Float = 1f,
    val bonusHp: Int = 0,
    val extraJumps: Int = 1,
    val rangedBasic: Boolean = false
)

data class RelicItemDef(
    val id: String,
    val name: String,
    val inscription: String,
    val desc: String,
    val costSparks: Int,
    val accentColor: Color,
    val bonusAtk: Int = 0,
    val bonusHp: Int = 0,
    val bonusCritPct: Float = 0f,
    val bonusSpeedPct: Float = 0f,
    val dashDamage: Int = 0,
    val cooldownReductionPct: Float = 0f
)

enum class CosmeticSlot {
    HEADGEAR,
    JACKET,
    PANTS_SHOES,
    BACKPACK
}

data class CosmeticDef(
    val id: String,
    val name: String,
    val slot: CosmeticSlot,
    val brandLine: String,
    val skillDesc: String,
    val unlockLevel: Int,
    val costSparks: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bonusAtk: Int = 0,
    val bonusHp: Int = 0,
    val bonusStamina: Int = 0,
    val bonusSpeed: Int = 0,
    val defensePct: Float = 0f
)

data class PetDef(
    val id: String,
    val name: String,
    val role: String,
    val abilityName: String,
    val abilityDesc: String,
    val accentColor: Color,
    @DrawableRes val portraitRes: Int,
    val unlockMissionId: String,
    val unlockHint: String,
    val bonusAtkPct: Float = 0f,
    val bonusDefPct: Float = 0f,
    val bonusStaminaRegen: Float = 0f,
    val firesProjectiles: Boolean = false
)

enum class ConsumableKind {
    FOOD,   // Sube Vida (HP)
    DRINK   // Sube Estamina
}

data class ConsumableDef(
    val id: String,
    val name: String,
    val kind: ConsumableKind,
    val restoreAmount: Int,
    val desc: String,
    val costSparks: Int,
    val accentColor: Color
)

data class MissionDef(
    val id: String,
    val title: String,
    val desc: String,
    val targetCount: Int,
    val rewardSparks: Int,
    val rewardXp: Int,
    val unlocksPetId: String? = null,
    val unlocksSkullId: String? = null
)

enum class GestureSkillId(
    val id: String,
    val title: String,
    val gestureHint: String,
    val minLevel: Int,
    val cooldownMs: Long,
    val damageMultiplier: Float
) {
    TAP_STRIKE("TAP_STRIKE", "Golpe Boxy", "TOQUE", 1, 220L, 1.0f),
    SWIPE_DASH("SWIPE_DASH", "Estocada Espada", "IZQ / DER", 1, 580L, 1.55f),
    DIAGONAL_SLASH("DIAGONAL_SLASH", "Corte Creciente", "DIAGONAL ↗", 1, 820L, 1.85f),
    UP_SLAM("UP_SLAM", "Gancho Ascendente", "ARRIBA ↑", 1, 1050L, 2.0f),
    DOWN_PARRY("DOWN_PARRY", "Guardia Parry", "ABAJO ↓", 1, 850L, 1.4f),
    CIRCLE_NOVA("CIRCLE_NOVA", "Torbellino Fósforo", "CÍRCULO ◯", 2, 2000L, 2.3f),
    Z_SLASH("Z_SLASH", "Combo Relámpago Z", "ZIGZAG ⚡", 3, 2600L, 2.8f),
    HOLD_ULTIMATE("HOLD_ULTIMATE", "Supernova Boxy", "MANTENER 🔥", 5, 4500L, 3.8f);
}

object GameCatalog {
    // CABEZAS / FÓSFOROS INTERCAMBIABLES DE BOXY (ESTILO SKUL: THE HERO SLAYER)
    val skullForms: List<SkullFormDef> = listOf(
        SkullFormDef(
            id = "boxy_classic",
            name = "Boxy Clásico",
            tierLabel = "COMÚN",
            styleType = "Equilibrio",
            desc = "La caja de fósforos original Mokay. Balance perfecto con combos rápidos y lanzamiento de fósforos.",
            skill1Name = "Ráfaga de Fósforos",
            skill1Desc = "Lanza 3 fósforos ardientes que perforan enemigos.",
            skill1CdMs = 3200L,
            skill2Name = "Salto Explosivo",
            skill2Desc = "Estalla en chispas quemando todo alrededor.",
            skill2CdMs = 5200L,
            swapSkillName = "Chispa de Entrada",
            swapSkillDesc = "Al cambiar a Boxy Clásico libera una onda de fuego.",
            costSparks = 0,
            primaryColor = MokayLavender,
            accentColor = MokayFlame,
            portraitRes = R.drawable.portrait_caja,
            speedMult = 1.0f,
            atkMult = 1.0f,
            bonusHp = 0,
            extraJumps = 1
        ),
        SkullFormDef(
            id = "boxy_flame",
            name = "Boxy Fósforo Llama",
            tierLabel = "RARO",
            styleType = "Velocidad",
            desc = "Se fusiona con la llama viva de Mokay. Combos ultra rápidos, doble dash ardiente y rastro ígneo.",
            skill1Name = "Tormenta Ígnea",
            skill1Desc = "Dispara 5 bolas de fuego en abanico de alta velocidad.",
            skill1CdMs = 2800L,
            skill2Name = "Meteoro Chispa",
            skill2Desc = "Impacto flameante con daño masivo de área.",
            skill2CdMs = 4800L,
            swapSkillName = "Explosión Viva",
            swapSkillDesc = "Al entrar incinera enemigos cercanos con daño crítico.",
            costSparks = 75,
            primaryColor = MokayFlame,
            accentColor = MokaySpark,
            portraitRes = R.drawable.portrait_mokay,
            speedMult = 1.22f,
            atkMult = 1.25f,
            bonusHp = 10,
            extraJumps = 2
        ),
        SkullFormDef(
            id = "boxy_tank",
            name = "Boxy Caja Blindada",
            tierLabel = "RARO",
            styleType = "Poder",
            desc = "Caja reforzada con acero Punk. Golpes pesados sísmicos, +70 HP y gran resistencia.",
            skill1Name = "Martillo Sísmico",
            skill1Desc = "Golpe brutal que aturde y daña en gran radio.",
            skill1CdMs = 4000L,
            skill2Name = "Furia Blindada",
            skill2Desc = "Ondas chocantes en ambos lados de la plataforma.",
            skill2CdMs = 6000L,
            swapSkillName = "Aterrizaje Titán",
            swapSkillDesc = "Al entrar aplasta el suelo lanzando rocas.",
            costSparks = 95,
            primaryColor = Color(0xFF6B7394),
            accentColor = MolotovCrimson,
            portraitRes = R.drawable.portrait_floko,
            speedMult = 0.88f,
            atkMult = 1.48f,
            bonusHp = 70,
            extraJumps = 1
        ),
        SkullFormDef(
            id = "boxy_bloom",
            name = "Boxy Graffiti Neón",
            tierLabel = "ÚNICO",
            styleType = "Técnica",
            desc = "Modo artístico Posca de Bloom. Ataques de tinta a distancia y trampas de pintura explosiva.",
            skill1Name = "Lluvia Posca",
            skill1Desc = "Proyectiles mágicos de pintura rosa teledirigidos.",
            skill1CdMs = 3000L,
            skill2Name = "Mural Explosivo",
            skill2Desc = "Explosión cromática que daña a todos en pantalla.",
            skill2CdMs = 5500L,
            swapSkillName = "Salpicadura Neón",
            swapSkillDesc = "Al entrar pinta chispas dañinas en 4 direcciones.",
            costSparks = 135,
            primaryColor = BloomPink,
            accentColor = MokaySpark,
            portraitRes = R.drawable.portrait_bloom,
            speedMult = 1.12f,
            atkMult = 1.32f,
            bonusHp = 25,
            extraJumps = 2,
            rangedBasic = true
        ),
        SkullFormDef(
            id = "boxy_birdy",
            name = "Boxy Birdy Aéreo",
            tierLabel = "LEGENDARIO",
            styleType = "Velocidad",
            desc = "Alas celestes de Birdy: ¡3 saltos en el aire, ráfagas de flechas pluma y movilidad total!",
            skill1Name = "Volea de Plumas",
            skill1Desc = "Descarga 6 flechas rápidas perforantes.",
            skill1CdMs = 2600L,
            skill2Name = "Ciclón Celeste",
            skill2Desc = "Remolino aéreo que golpea en torno a Boxy.",
            skill2CdMs = 4600L,
            swapSkillName = "Descenso Halcón",
            swapSkillDesc = "Al entrar dispara flechas hacia adelante y arriba.",
            costSparks = 180,
            primaryColor = BirdySky,
            accentColor = MokayCream,
            portraitRes = R.drawable.portrait_birdy,
            speedMult = 1.28f,
            atkMult = 1.42f,
            bonusHp = 35,
            extraJumps = 3,
            rangedBasic = true
        )
    )

    // RELIQUIAS / OBJETOS PASIVOS ESTILO SKUL (Hasta 6 equipadas en la Run!)
    val relics: List<RelicItemDef> = listOf(
        RelicItemDef("rel_gold_match", "Fósforo Dorado", "Fuego", "+14 ATK y +10% Crítico.", 45, MokaySpark, bonusAtk = 14, bonusCritPct = 0.10f),
        RelicItemDef("rel_dash_boots", "Zapas Dash Mokay", "Velocidad", "+18% Velocidad y el Dash hace 28 de daño.", 55, BirdySky, bonusSpeedPct = 0.18f, dashDamage = 28),
        RelicItemDef("rel_iron_box", "Carcasa Titanio", "Fortaleza", "+55 Vida Máxima para aguantar jefes.", 50, DinoEmerald, bonusHp = 55),
        RelicItemDef("rel_cooldown_chip", "Chip Chispa Turbo", "Técnica", "-20% Enfriamiento de Habilidades 1 y 2.", 70, MokayLavender, cooldownReductionPct = 0.20f),
        RelicItemDef("rel_punk_chain", "Cadena Punk Molotov", "Furia", "+24 ATK y +15% Crítico.", 90, MolotovCrimson, bonusAtk = 24, bonusCritPct = 0.15f),
        RelicItemDef("rel_crown_mokay", "Corona Rey Mokay", "Legendaria", "+32 ATK, +45 HP y +15% Velocidad.", 140, RetroGold, bonusAtk = 32, bonusHp = 45, bonusSpeedPct = 0.15f)
    )

    // ROPA STREETWEAR 100% MARCA MOKAY PARA BOXY
    val cosmetics: List<CosmeticDef> = listOf(
        CosmeticDef(
            id = "mokay_cap_classic",
            name = "Gorra Mokay Crown OG",
            slot = CosmeticSlot.HEADGEAR,
            brandLine = "MOKAY STREETWEAR · DROP 01",
            skillDesc = "+15 HP y +5% Defensa urbana",
            unlockLevel = 1,
            costSparks = 0,
            primaryColor = MokayFlame,
            secondaryColor = MokayCream,
            bonusHp = 15,
            defensePct = 0.05f
        ),
        CosmeticDef(
            id = "mokay_beanie_knit",
            name = "Beanie Mokay Street",
            slot = CosmeticSlot.HEADGEAR,
            brandLine = "MOKAY STREETWEAR · WINTER",
            skillDesc = "+25 Estamina máx. y +8 ATK",
            unlockLevel = 2,
            costSparks = 45,
            primaryColor = MokayLavender,
            secondaryColor = MokaySpark,
            bonusAtk = 8,
            bonusStamina = 25
        ),
        CosmeticDef(
            id = "mokay_bandana_paisley",
            name = "Bandana Mokay Paisley",
            slot = CosmeticSlot.HEADGEAR,
            brandLine = "MOKAY STREETWEAR · PUNK",
            skillDesc = "+18 ATK y +10% Defensa",
            unlockLevel = 4,
            costSparks = 95,
            primaryColor = MolotovCrimson,
            secondaryColor = MokaySpark,
            bonusAtk = 18,
            defensePct = 0.10f
        ),
        CosmeticDef(
            id = "mokay_hoodie_flame",
            name = "Hoodie Mokay Flame",
            slot = CosmeticSlot.JACKET,
            brandLine = "MOKAY STREETWEAR · CORE",
            skillDesc = "+20 HP y logo llama Mokay",
            unlockLevel = 1,
            costSparks = 0,
            primaryColor = Color(0xFF282C4A),
            secondaryColor = MokayFlame,
            bonusHp = 20
        ),
        CosmeticDef(
            id = "mokay_jacket_varsity",
            name = "Chaqueta Varsity Mokay",
            slot = CosmeticSlot.JACKET,
            brandLine = "MOKAY STREETWEAR · VARSITY",
            skillDesc = "+35 HP, +14 ATK y +12% Defensa",
            unlockLevel = 2,
            costSparks = 65,
            primaryColor = MokayLavender,
            secondaryColor = MokayCream,
            bonusHp = 35,
            bonusAtk = 14,
            defensePct = 0.12f
        ),
        CosmeticDef(
            id = "mokay_vest_spiked",
            name = "Chaleco Punk Mokay Crown",
            slot = CosmeticSlot.JACKET,
            brandLine = "MOKAY STREETWEAR · ANARCHY",
            skillDesc = "+65 HP, +26 ATK y +18% Defensa",
            unlockLevel = 4,
            costSparks = 130,
            primaryColor = MokayCharcoal,
            secondaryColor = MolotovCrimson,
            bonusHp = 65,
            bonusAtk = 26,
            defensePct = 0.18f
        ),
        CosmeticDef(
            id = "mokay_kicks_street",
            name = "Zapas Chunky Mokay Lilac",
            slot = CosmeticSlot.PANTS_SHOES,
            brandLine = "MOKAY KICKS · ORIGINALS",
            skillDesc = "+15 Estamina y pisada firme",
            unlockLevel = 1,
            costSparks = 0,
            primaryColor = MokayLavender,
            secondaryColor = MokayCream,
            bonusStamina = 15
        ),
        CosmeticDef(
            id = "mokay_kicks_chunky",
            name = "Zapas Turbo Street Mokay",
            slot = CosmeticSlot.PANTS_SHOES,
            brandLine = "MOKAY KICKS · TURBO XL",
            skillDesc = "+22 Velocidad, +30 Estamina y +10 ATK",
            unlockLevel = 3,
            costSparks = 75,
            primaryColor = DinoEmerald,
            secondaryColor = FlokoOrange,
            bonusAtk = 10,
            bonusStamina = 30,
            bonusSpeed = 22
        ),
        CosmeticDef(
            id = "mokay_pack_original",
            name = "Mochila Mokay Urban Pack",
            slot = CosmeticSlot.BACKPACK,
            brandLine = "MOKAY BAGS · URBAN",
            skillDesc = "+20 Estamina para combos y dash",
            unlockLevel = 1,
            costSparks = 0,
            primaryColor = MokayLavender,
            secondaryColor = MokayFlame,
            bonusStamina = 20
        ),
        CosmeticDef(
            id = "mokay_pack_posca",
            name = "Mochila Mokay Studio Pro",
            slot = CosmeticSlot.BACKPACK,
            brandLine = "MOKAY BAGS · PRO",
            skillDesc = "+45 HP, +40 Estamina y +12 ATK",
            unlockLevel = 3,
            costSparks = 90,
            primaryColor = BloomPink,
            secondaryColor = MokaySpark,
            bonusHp = 45,
            bonusAtk = 12,
            bonusStamina = 40
        )
    )

    // MASCOTAS MOKAY DESBLOQUEABLES (Máx 3 acompañando a Boxy)
    val pets: List<PetDef> = listOf(
        PetDef(
            id = "mokay",
            name = "Mokay El Fósforo",
            role = "Guía Chispa",
            abilityName = "Bola de Fuego",
            abilityDesc = "Dispara bolas de fuego automáticas a enemigos cercanos.",
            accentColor = MokayFlame,
            portraitRes = R.drawable.portrait_mokay,
            unlockMissionId = "starter",
            unlockHint = "Acompañante inicial",
            bonusAtkPct = 0.12f,
            firesProjectiles = true
        ),
        PetDef(
            id = "bloom",
            name = "Bloom",
            role = "Artista Posca",
            abilityName = "Aura Creativa",
            abilityDesc = "+15% ATK y regenera +5 Estamina/seg.",
            accentColor = BloomPink,
            portraitRes = R.drawable.portrait_bloom,
            unlockMissionId = "m_first_clear",
            unlockHint = "Misión: Limpia 1 sala de combate",
            bonusAtkPct = 0.15f,
            bonusStaminaRegen = 5f
        ),
        PetDef(
            id = "birdy",
            name = "Birdy",
            role = "Halcón Celeste",
            abilityName = "Viento de Combo",
            abilityDesc = "Dispara plumas veloces y +6 Estamina/seg.",
            accentColor = BirdySky,
            portraitRes = R.drawable.portrait_birdy,
            unlockMissionId = "m_swap_skulls",
            unlockHint = "Misión: Haz 5 Cambios de Cabeza",
            bonusAtkPct = 0.12f,
            bonusStaminaRegen = 6f,
            firesProjectiles = true
        ),
        PetDef(
            id = "floko",
            name = "Floko",
            role = "Escudo Nieve",
            abilityName = "Escarcha Protectora",
            abilityDesc = "+22% Defensa contra Street Dinos y Molotov.",
            accentColor = FlokoOrange,
            portraitRes = R.drawable.portrait_floko,
            unlockMissionId = "m_reach_desert",
            unlockHint = "Misión: Llega al Acto 2 (Desierto)",
            bonusDefPct = 0.22f
        ),
        PetDef(
            id = "zenkay",
            name = "Zenkay",
            role = "Maestro Zen",
            abilityName = "Espinas Cristal",
            abilityDesc = "+24% ATK y disparos cristal teledirigidos.",
            accentColor = Color(0xFF4BBE4E),
            portraitRes = R.drawable.portrait_zenkay,
            unlockMissionId = "m_defeat_crew",
            unlockHint = "Misión: Derrota 8 enemigos Street",
            bonusAtkPct = 0.24f,
            firesProjectiles = true
        )
    )

    val consumables: List<ConsumableDef> = listOf(
        ConsumableDef("food_apple", "Manzana Silvestre", ConsumableKind.FOOD, 35, "Restaura +35 de Vida.", 10, RetroMint),
        ConsumableDef("food_sandwich", "Sándwich Mokay", ConsumableKind.FOOD, 75, "Restaura +75 de Vida.", 22, MokayFlame),
        ConsumableDef("food_pizza", "Pizza Chispa XL", ConsumableKind.FOOD, 130, "Restaura +130 de Vida.", 38, MokaySpark),
        ConsumableDef("drink_oasis", "Agua Oasis", ConsumableKind.DRINK, 45, "Recupera +45 de Estamina.", 10, BirdySky),
        ConsumableDef("drink_soda", "Mokay Soda Street", ConsumableKind.DRINK, 80, "Recupera +80 de Estamina.", 22, MokayLavender),
        ConsumableDef("drink_turbo", "Mokay Turbo Energy", ConsumableKind.DRINK, 120, "Recupera +120 de Estamina.", 36, MolotovCrimson)
    )

    val missions: List<MissionDef> = listOf(
        MissionDef(
            id = "m_first_clear",
            title = "Primera Sala Limpia",
            desc = "Entra al Portal en Campamento Aldea y derrota a todos los enemigos de la sala.",
            targetCount = 1,
            rewardSparks = 75,
            rewardXp = 60,
            unlocksPetId = "bloom"
        ),
        MissionDef(
            id = "m_swap_skulls",
            title = "Maestro del Cambio (Skul)",
            desc = "Usa el botón CAMBIAR para alternar cabezas/fósforos de Boxy 5 veces en combate.",
            targetCount = 5,
            rewardSparks = 95,
            rewardXp = 75,
            unlocksPetId = "birdy",
            unlocksSkullId = "boxy_flame"
        ),
        MissionDef(
            id = "m_reach_desert",
            title = "Avanza al Acto 2 (Desierto)",
            desc = "Cruza portales limpiando salas hasta alcanzar el Desierto Dorado.",
            targetCount = 1,
            rewardSparks = 120,
            rewardXp = 100,
            unlocksPetId = "floko"
        ),
        MissionDef(
            id = "m_defeat_crew",
            title = "Cazador de Dino y Cat",
            desc = "Derrota a 8 enemigos entre Laughing Cat, Street Dino y Molotov Punk.",
            targetCount = 8,
            rewardSparks = 160,
            rewardXp = 130,
            unlocksPetId = "zenkay"
        ),
        MissionDef(
            id = "m_upgrade_weapon",
            title = "Forja de Boxy Nv.2+",
            desc = "Mejora el Arma de Boxy a Nivel 2+ o consigue 2 Reliquias Skul.",
            targetCount = 1,
            rewardSparks = 130,
            rewardXp = 110
        ),
        MissionDef(
            id = "m_defeat_molotov",
            title = "Jefe Final: Molotov Mokay",
            desc = "Llega al Acto 4 (o rétalo en Misiones) y vence al Jefe Molotov Mokay Rey Punk.",
            targetCount = 1,
            rewardSparks = 350,
            rewardXp = 320
        )
    )

    fun skullById(id: String): SkullFormDef = skullForms.firstOrNull { it.id == id } ?: skullForms.first()
    fun relicById(id: String): RelicItemDef? = relics.firstOrNull { it.id == id }
    fun petById(id: String): PetDef = pets.firstOrNull { it.id == id } ?: pets.first()
    fun cosmeticById(id: String): CosmeticDef? = cosmetics.firstOrNull { it.id == id }
    fun consumableById(id: String): ConsumableDef? = consumables.firstOrNull { it.id == id }
    fun xpRequiredForLevel(level: Int): Int = 55 + (level - 1) * 45
}
