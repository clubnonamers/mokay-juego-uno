package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.RetroMint

enum class ResourceMaterial(
    val id: String,
    val displayName: String,
    val category: String,
    val color: Color
) {
    WOOD("madera", "Madera", "Orgánico", Color(0xFFB87F46)),
    STONE("piedra", "Piedra", "Mineral", Color(0xFF9EA4B8)),
    COPPER("cobre", "Cobre", "Metal", MokayFlame),
    IRON("hierro", "Hierro", "Metal", BirdySky),
    CRYSTAL("cristal", "Cristal Mokay", "Metal Raro", MokayLavender);

    companion object {
        fun byId(id: String): ResourceMaterial? = entries.firstOrNull { it.id == id }
    }
}

enum class CraftableTool(
    val id: String,
    val displayName: String,
    val description: String,
    val maxLevel: Int,
    val accentColor: Color
) {
    AXE(
        id = "tool_axe",
        displayName = "Hacha Mokay",
        description = "Aumenta la recolección de madera y el daño contra cajas y enemigos.",
        maxLevel = 5,
        accentColor = RetroMint
    ),
    PICKAXE(
        id = "tool_pickaxe",
        displayName = "Pico Minero",
        description = "Multiplica piedra, cobre, hierro y cristales obtenidos en salas y cofres.",
        maxLevel = 5,
        accentColor = MokayLavender
    ),
    BOW(
        id = "tool_bow",
        displayName = "Arco Street",
        description = "Combina madera y metales para fabricar y potenciar el arco de combate.",
        maxLevel = 5,
        accentColor = MokaySpark
    )
}

data class ToolCraftingRecipe(
    val tool: CraftableTool,
    val targetLevel: Int,
    val requiredMaterials: Map<ResourceMaterial, Int>,
    val requiredSparks: Int
)

sealed class CraftResult {
    data class Success(
        val tool: CraftableTool,
        val newLevel: Int,
        val message: String
    ) : CraftResult()

    data class Failure(val reason: String) : CraftResult()
}

/**
 * Gestiona los materiales recolectados (madera, piedra, metales) y permite combinarlos
 * mediante recetas para fabricar o mejorar herramientas (hacha, pico, arco y flechas).
 */
class InventoryManager(
    initialMaterials: Map<ResourceMaterial, Int> = defaultStarterMaterials(),
    initialToolLevels: Map<CraftableTool, Int> = defaultStarterTools(),
    initialArrows: Int = 10
) {
    private val materials: MutableMap<ResourceMaterial, Int> = initialMaterials.toMutableMap()
    private val toolLevels: MutableMap<CraftableTool, Int> = initialToolLevels.toMutableMap()
    var arrowsCount: Int = initialArrows.coerceAtLeast(0)
        private set

    fun getMaterialCount(material: ResourceMaterial): Int = materials[material] ?: 0

    fun getAllMaterials(): Map<ResourceMaterial, Int> =
        ResourceMaterial.entries.associateWith { materials[it] ?: 0 }

    fun getToolLevel(tool: CraftableTool): Int = toolLevels[tool] ?: 0

    fun getAllToolLevels(): Map<CraftableTool, Int> =
        CraftableTool.entries.associateWith { toolLevels[it] ?: 0 }

    fun addMaterial(material: ResourceMaterial, amount: Int) {
        if (amount <= 0) return
        materials[material] = (materials[material] ?: 0) + amount
    }

    fun addMaterials(gains: Map<ResourceMaterial, Int>) {
        gains.forEach { (mat, qty) -> addMaterial(mat, qty) }
    }

    fun hasMaterials(requirements: Map<ResourceMaterial, Int>): Boolean {
        return requirements.all { (mat, needed) -> getMaterialCount(mat) >= needed }
    }

    fun consumeMaterials(requirements: Map<ResourceMaterial, Int>): Boolean {
        if (!hasMaterials(requirements)) return false
        requirements.forEach { (mat, needed) ->
            val left = (getMaterialCount(mat) - needed).coerceAtLeast(0)
            materials[mat] = left
        }
        return true
    }

    fun nextUpgradeRecipe(tool: CraftableTool): ToolCraftingRecipe? {
        val current = getToolLevel(tool)
        if (current >= tool.maxLevel) return null
        return recipeFor(tool, current + 1)
    }

    fun canCraftNextLevel(tool: CraftableTool, availableSparks: Long = Long.MAX_VALUE): Boolean {
        val recipe = nextUpgradeRecipe(tool) ?: return false
        return availableSparks >= recipe.requiredSparks && hasMaterials(recipe.requiredMaterials)
    }

    fun craftNextToolLevel(
        tool: CraftableTool,
        availableSparks: Long = Long.MAX_VALUE,
        onSpendSparks: (Int) -> Unit = {}
    ): CraftResult {
        val recipe = nextUpgradeRecipe(tool)
            ?: return CraftResult.Failure("${tool.displayName} ya está al nivel máximo (Nv.${tool.maxLevel}).")

        if (availableSparks < recipe.requiredSparks) {
            return CraftResult.Failure("Faltan ${recipe.requiredSparks}⚡ Chispas para ${tool.displayName} Nv.${recipe.targetLevel}.")
        }
        val missing = recipe.requiredMaterials.entries
            .filter { (mat, req) -> getMaterialCount(mat) < req }
            .joinToString(", ") { (mat, req) -> "${req - getMaterialCount(mat)} ${mat.displayName}" }
        if (missing.isNotEmpty()) {
            return CraftResult.Failure("Faltan materiales: $missing.")
        }

        consumeMaterials(recipe.requiredMaterials)
        onSpendSparks(recipe.requiredSparks)
        toolLevels[tool] = recipe.targetLevel
        if (tool == CraftableTool.BOW && recipe.targetLevel == 1) {
            arrowsCount += 15
        }
        return CraftResult.Success(
            tool = tool,
            newLevel = recipe.targetLevel,
            message = "¡Fabricado ${tool.displayName} Nv.${recipe.targetLevel}!"
        )
    }

    fun craftArrowsBundle(bundleSize: Int = 10): Boolean {
        val cost = mapOf(
            ResourceMaterial.WOOD to 3,
            ResourceMaterial.STONE to 2
        )
        if (!consumeMaterials(cost)) return false
        arrowsCount += bundleSize
        return true
    }

    fun consumeArrow(): Boolean {
        if (arrowsCount <= 0) return false
        arrowsCount--
        return true
    }

    /**
     * Serializes materials, tool levels, and arrows into the existing SaveSlot.inventoryCounts map
     * to maintain 100% Firestore schema compatibility.
     */
    fun writeToInventoryMap(baseInventory: Map<String, Int>): Map<String, Int> {
        val out = baseInventory.toMutableMap()
        ResourceMaterial.entries.forEach { mat ->
            out["$MAT_PREFIX${mat.id}"] = getMaterialCount(mat)
        }
        CraftableTool.entries.forEach { tool ->
            out["$TOOL_PREFIX${tool.id}"] = getToolLevel(tool)
        }
        out[ARROWS_KEY] = arrowsCount
        return out
    }

    fun copy(): InventoryManager {
        return InventoryManager(
            initialMaterials = getAllMaterials(),
            initialToolLevels = getAllToolLevels(),
            initialArrows = arrowsCount
        )
    }

    companion object {
        private const val MAT_PREFIX = "mat_"
        private const val TOOL_PREFIX = "lvl_"
        private const val ARROWS_KEY = "ammo_arrows"

        fun defaultStarterMaterials(): Map<ResourceMaterial, Int> = mapOf(
            ResourceMaterial.WOOD to 8,
            ResourceMaterial.STONE to 6,
            ResourceMaterial.COPPER to 2,
            ResourceMaterial.IRON to 0,
            ResourceMaterial.CRYSTAL to 0
        )

        fun defaultStarterTools(): Map<CraftableTool, Int> = mapOf(
            CraftableTool.AXE to 1,
            CraftableTool.PICKAXE to 1,
            CraftableTool.BOW to 0
        )

        fun fromInventoryMap(inventory: Map<String, Int>): InventoryManager {
            val mats = ResourceMaterial.entries.associateWith { mat ->
                inventory["$MAT_PREFIX${mat.id}"] ?: defaultStarterMaterials()[mat] ?: 0
            }
            val tools = CraftableTool.entries.associateWith { tool ->
                inventory["$TOOL_PREFIX${tool.id}"] ?: defaultStarterTools()[tool] ?: 0
            }
            val arrows = inventory[ARROWS_KEY] ?: 10
            return InventoryManager(mats, tools, arrows)
        }

        fun recipeFor(tool: CraftableTool, targetLevel: Int): ToolCraftingRecipe? {
            if (targetLevel !in 1..tool.maxLevel) return null
            val mats: Map<ResourceMaterial, Int> = when (tool) {
                CraftableTool.AXE -> when (targetLevel) {
                    1 -> mapOf(ResourceMaterial.WOOD to 3, ResourceMaterial.STONE to 2)
                    2 -> mapOf(ResourceMaterial.WOOD to 6, ResourceMaterial.STONE to 4)
                    3 -> mapOf(ResourceMaterial.WOOD to 8, ResourceMaterial.COPPER to 4)
                    4 -> mapOf(ResourceMaterial.WOOD to 10, ResourceMaterial.IRON to 5)
                    else -> mapOf(ResourceMaterial.WOOD to 12, ResourceMaterial.IRON to 6, ResourceMaterial.CRYSTAL to 3)
                }
                CraftableTool.PICKAXE -> when (targetLevel) {
                    1 -> mapOf(ResourceMaterial.WOOD to 2, ResourceMaterial.STONE to 4)
                    2 -> mapOf(ResourceMaterial.WOOD to 4, ResourceMaterial.STONE to 6)
                    3 -> mapOf(ResourceMaterial.WOOD to 6, ResourceMaterial.COPPER to 5)
                    4 -> mapOf(ResourceMaterial.WOOD to 8, ResourceMaterial.IRON to 6)
                    else -> mapOf(ResourceMaterial.WOOD to 10, ResourceMaterial.IRON to 7, ResourceMaterial.CRYSTAL to 3)
                }
                CraftableTool.BOW -> when (targetLevel) {
                    1 -> mapOf(ResourceMaterial.WOOD to 6, ResourceMaterial.STONE to 3)
                    2 -> mapOf(ResourceMaterial.WOOD to 8, ResourceMaterial.COPPER to 4)
                    3 -> mapOf(ResourceMaterial.WOOD to 10, ResourceMaterial.COPPER to 6, ResourceMaterial.IRON to 3)
                    4 -> mapOf(ResourceMaterial.WOOD to 12, ResourceMaterial.IRON to 6, ResourceMaterial.CRYSTAL to 2)
                    else -> mapOf(ResourceMaterial.WOOD to 15, ResourceMaterial.IRON to 8, ResourceMaterial.CRYSTAL to 5)
                }
            }
            val sparks = 20 * targetLevel
            return ToolCraftingRecipe(tool, targetLevel, mats, sparks)
        }
    }
}
