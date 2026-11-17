package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ConsumableKind
import com.example.data.GameCatalog
import com.example.data.SaveSlot
import com.example.game.CraftableTool
import com.example.game.GameSessionState
import com.example.game.GameSettings
import com.example.game.ResourceMaterial
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.RetroBgDark
import com.example.ui.theme.RetroBorderLight
import com.example.ui.theme.RetroDanger
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroMint
import com.example.ui.theme.RetroSurface
import com.example.ui.theme.RetroSurfaceElevated

enum class MenuTab(val label: String, val icon: ImageVector) {
    BOXY("Boxy", Icons.Filled.Face),
    SHOP("Reliquias", Icons.Filled.Build),
    MISSIONS("Misiones", Icons.Filled.Flag),
    PETS("Mascotas", Icons.Filled.Pets),
    SETTINGS("Ajustes", Icons.Filled.Settings),
    SAVES("Nube", Icons.Filled.Save)
}

@Composable
fun RetroMenuDialog(
    initialTab: MenuTab,
    state: GameSessionState,
    saveSlots: List<SaveSlot>,
    isSyncing: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdateHeroName: (String) -> Unit,
    onEquipSkull: (String, Int) -> Unit,
    onEquipOrBuyCosmetic: (String) -> Unit,
    onBuyOrToggleRelic: (String) -> Unit,
    onUpgradeWeaponOrShield: (Boolean) -> Unit,
    onCraftTool: (CraftableTool) -> Unit,
    onCraftArrows: () -> Unit,
    onBuyConsumable: (String) -> Unit,
    onUseConsumable: (String) -> Unit,
    onAllocateStat: (String) -> Unit,
    onClaimMission: (String) -> Unit,
    onSummonMolotovBoss: () -> Unit,
    onTogglePet: (String) -> Unit,
    onUpdateSettings: (GameSettings) -> Unit,
    onSaveToSlot: (String, String) -> Unit,
    onLoadSlot: (SaveSlot) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = RetroBgDark,
            border = BorderStroke(3.dp, MokayLavender),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxSize(0.95f).padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        MenuTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                color = if (selected) MokayFlame else RetroSurfaceElevated,
                                border = BorderStroke(1.5.dp, if (selected) MokayCream else RetroBorderLight),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTab = tab }
                                    .testTag("tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 3.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(tab.icon, contentDescription = tab.label, tint = if (selected) MokayCharcoal else MokayCream, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MokayCharcoal else MokayCream,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Surface(color = RetroSurfaceElevated, border = BorderStroke(1.5.dp, MokaySpark), shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "Nv.${state.heroLevel} · ${state.sparksCurrency}⚡",
                            style = MaterialTheme.typography.labelSmall,
                            color = MokaySpark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_menu_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar menú", tint = MokayCream)
                    }
                }

                if (statusMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(color = RetroSurfaceElevated, border = BorderStroke(1.dp, RetroGold), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(statusMessage, style = MaterialTheme.typography.labelSmall, color = RetroGold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        MenuTab.BOXY -> BoxySkullsAndWardrobeTab(state, onUpdateHeroName, onEquipSkull, onEquipOrBuyCosmetic)
                        MenuTab.SHOP -> RelicsAndShopTab(
                            state = state,
                            onBuyOrToggleRelic = onBuyOrToggleRelic,
                            onUpgradeWeaponOrShield = onUpgradeWeaponOrShield,
                            onCraftTool = onCraftTool,
                            onCraftArrows = onCraftArrows,
                            onBuyConsumable = onBuyConsumable,
                            onUseConsumable = onUseConsumable,
                            onAllocateStat = onAllocateStat
                        )
                        MenuTab.MISSIONS -> MissionsTab(state, onClaimMission, onSummonMolotovBoss)
                        MenuTab.PETS -> CompanionPetsTab(state, onTogglePet)
                        MenuTab.SETTINGS -> SettingsTab(state.settings, onUpdateSettings)
                        MenuTab.SAVES -> CloudSavesTab(state, saveSlots, isSyncing, onSaveToSlot, onLoadSlot, onDeleteSlot, onSignOut)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelicsAndShopTab(
    state: GameSessionState,
    onBuyOrToggleRelic: (String) -> Unit,
    onUpgradeWeaponOrShield: (Boolean) -> Unit,
    onCraftTool: (CraftableTool) -> Unit,
    onCraftArrows: () -> Unit,
    onBuyConsumable: (String) -> Unit,
    onUseConsumable: (String) -> Unit,
    onAllocateStat: (String) -> Unit
) {
    var subCat by remember { mutableStateOf(0) } // 0=Reliquias Skul, 1=Forja Boxy, 2=Alimentos y Bebidas

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RELIQUIAS EQUIPADAS: ${state.equippedRelics.size}/6 · Arma Nv.${state.weaponLevel} · Escudo Nv.${state.shieldLevel}",
                style = MaterialTheme.typography.labelSmall,
                color = MokaySpark
            )
            if (state.statPoints > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ATK", "HP", "SPD").forEach { k ->
                        Button(
                            onClick = { onAllocateStat(k) },
                            colors = ButtonDefaults.buttonColors(containerColor = MokaySpark, contentColor = MokayCharcoal),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(28.dp).testTag("stat_btn_$k")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text(k, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Reliquias Skul (${state.equippedRelics.size}/6)", 1 to "Forja Arma / Escudo", 2 to "Alimentos y Bebidas").forEach { (idx, label) ->
                val sel = subCat == idx
                Surface(
                    color = if (sel) BirdySky else RetroSurfaceElevated,
                    border = BorderStroke(1.5.dp, if (sel) MokayCream else RetroBorderLight),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).clickable { subCat = idx }.testTag("shop_sub_$idx")
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) MokayCharcoal else MokayCream, modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            when (subCat) {
                0 -> {
                    items(GameCatalog.relics, key = { it.id }) { relic ->
                        val equipped = relic.id in state.equippedRelics
                        Surface(
                            color = if (equipped) RetroSurfaceElevated else RetroSurface,
                            border = BorderStroke(2.dp, if (equipped) relic.accentColor else RetroBorderLight),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${relic.name} [${relic.inscription}]", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text(relic.desc, style = MaterialTheme.typography.bodyMedium, color = relic.accentColor)
                                }
                                Button(
                                    onClick = { onBuyOrToggleRelic(relic.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (equipped) RetroMint else MokayFlame,
                                        contentColor = MokayCharcoal
                                    ),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("relic_btn_${relic.id}")
                                ) {
                                    Text(if (equipped) "Equipada" else "${relic.costSparks}⚡", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        val invMgr = state.inventoryManager()
                        Surface(
                            color = RetroSurfaceElevated,
                            border = BorderStroke(1.5.dp, MokaySpark),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("MATERIALES RECOLECTADOS (InventoryManager)", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ResourceMaterial.entries.forEach { mat ->
                                        Surface(
                                            color = RetroBgDark,
                                            border = BorderStroke(1.dp, mat.color),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${mat.displayName}: ${invMgr.getMaterialCount(mat)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = mat.color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(CraftableTool.entries, key = { it.id }) { tool ->
                        val invMgr = state.inventoryManager()
                        val curLvl = invMgr.getToolLevel(tool)
                        val recipe = invMgr.nextUpgradeRecipe(tool)
                        val reqText = recipe?.requiredMaterials?.entries?.joinToString(" + ") { (m, q) -> "$q ${m.displayName}" } ?: "Nivel Máximo"
                        Surface(color = RetroSurface, border = BorderStroke(1.5.dp, tool.accentColor), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${tool.displayName} · Nv.$curLvl/${tool.maxLevel}", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = MokayCream.copy(alpha = 0.85f))
                                    Text(
                                        text = if (recipe != null) "Combinar: $reqText + ${recipe.requiredSparks}⚡" else "Completado al máximo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tool.accentColor
                                    )
                                }
                                Button(
                                    onClick = { onCraftTool(tool) },
                                    enabled = recipe != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = tool.accentColor, contentColor = MokayCharcoal),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("craft_${tool.id}_btn")
                                ) {
                                    Text(if (recipe == null) "MAX" else if (curLvl == 0) "Fabricar" else "Mejorar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    item {
                        val invMgr = state.inventoryManager()
                        Surface(color = RetroSurface, border = BorderStroke(1.5.dp, MokaySpark), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Carcaj de Flechas (${invMgr.arrowsCount})", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text("Combina 3 Madera + 2 Piedra para fabricar +10 Flechas.", style = MaterialTheme.typography.bodyMedium, color = MokaySpark)
                                }
                                Button(
                                    onClick = onCraftArrows,
                                    colors = ButtonDefaults.buttonColors(containerColor = MokaySpark, contentColor = MokayCharcoal),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("craft_arrows_btn")
                                ) { Text("+10 Flechas", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                    item {
                        val nextW = (state.weaponLevel + 1).coerceAtMost(5)
                        val costW = 35 * nextW
                        Surface(color = RetroSurface, border = BorderStroke(1.5.dp, MokayFlame), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Arma Chispa de Boxy · Nv.${state.weaponLevel}/5", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text("Aumenta todo el daño de ataques básicos, habilidades y Swap (+12 ATK base/Nv).", style = MaterialTheme.typography.bodyMedium, color = MokaySpark)
                                }
                                Button(
                                    onClick = { onUpgradeWeaponOrShield(false) },
                                    enabled = state.weaponLevel < 5,
                                    colors = ButtonDefaults.buttonColors(containerColor = MokayFlame, contentColor = MokayCharcoal),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("upgrade_weapon_btn")
                                ) {
                                    Text(if (state.weaponLevel >= 5) "MAX" else "${costW}⚡ Subir", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    item {
                        val nextS = (state.shieldLevel + 1).coerceAtMost(5)
                        val costS = 35 * nextS
                        Surface(color = RetroSurface, border = BorderStroke(1.5.dp, BirdySky), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Escudo Caja Mokay · Nv.${state.shieldLevel}/5", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text("Reduce más daño al mantener ESCUDO frente a ataques enemigos.", style = MaterialTheme.typography.bodyMedium, color = BirdySky)
                                }
                                Button(
                                    onClick = { onUpgradeWeaponOrShield(true) },
                                    enabled = state.shieldLevel < 5,
                                    colors = ButtonDefaults.buttonColors(containerColor = BirdySky, contentColor = MokayCharcoal),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("upgrade_shield_btn")
                                ) {
                                    Text(if (state.shieldLevel >= 5) "MAX" else "${costS}⚡ Subir", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                else -> {
                    items(GameCatalog.consumables, key = { it.id }) { cons ->
                        val qty = state.inventoryCounts[cons.id] ?: 0
                        val typeLabel = if (cons.kind == ConsumableKind.FOOD) "ALIMENTO (VIDA)" else "BEBIDA (ESTAMINA)"
                        Surface(color = RetroSurface, border = BorderStroke(1.5.dp, cons.accentColor), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${cons.name} (x$qty)", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text("$typeLabel · ${cons.desc}", style = MaterialTheme.typography.labelSmall, color = cons.accentColor)
                                }
                                if (qty > 0) {
                                    Button(
                                        onClick = { onUseConsumable(cons.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RetroMint, contentColor = MokayCharcoal),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.padding(end = 6.dp).testTag("use_${cons.id}")
                                    ) {
                                        Text(if (cons.kind == ConsumableKind.FOOD) "Comer" else "Beber", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Button(
                                    onClick = { onBuyConsumable(cons.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = cons.accentColor, contentColor = MokayCharcoal),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("buy_${cons.id}")
                                ) {
                                    Text("${cons.costSparks}⚡", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSavesTab(
    state: GameSessionState,
    saveSlots: List<SaveSlot>,
    isSyncing: Boolean,
    onSaveToSlot: (String, String) -> Unit,
    onLoadSlot: (SaveSlot) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSaveToSlot("quicksave", "Guardado Rápido") },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = MokayFlame, contentColor = MokayCharcoal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).testTag("save_quicksave_btn")
            ) { Text("Guardado Rápido", style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = { onSaveToSlot("slot_1", "Run ${state.appearance.heroName}") },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = MokayLavender, contentColor = MokayCharcoal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).testTag("save_slot1_btn")
            ) { Text("Guardar Ranura 1", style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = RetroSurfaceElevated, contentColor = RetroDanger),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salir", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(saveSlots, key = { it.id.ifBlank { it.slotLabel } }) { slot ->
                Surface(color = RetroSurface, border = BorderStroke(1.5.dp, RetroBorderLight), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${slot.slotLabel} · ${slot.heroName} (Nv.${slot.heroLevel})", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                            Text("${slot.currentZone} Sala ${slot.currentRoomIndex} · ${slot.sparksCurrency}⚡", style = MaterialTheme.typography.labelSmall, color = MokayLavender)
                        }
                        Button(
                            onClick = { onLoadSlot(slot) },
                            colors = ButtonDefaults.buttonColors(containerColor = RetroMint, contentColor = MokayCharcoal),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("load_slot_${slot.id}")
                        ) { Text("Cargar", style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { onDeleteSlot(slot.id) }, modifier = Modifier.testTag("delete_slot_${slot.id}")) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar ranura", tint = RetroDanger)
                        }
                    }
                }
            }
        }
    }
}
