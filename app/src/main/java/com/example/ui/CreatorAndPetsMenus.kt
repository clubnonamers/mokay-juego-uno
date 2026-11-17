package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CosmeticSlot
import com.example.data.GameCatalog
import com.example.game.BoxyAvatarPreview
import com.example.game.GameSessionState
import com.example.game.VectorCharacterBadge
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.RetroBgDark
import com.example.ui.theme.RetroBorderLight
import com.example.ui.theme.RetroMint
import com.example.ui.theme.RetroSurface
import com.example.ui.theme.RetroSurfaceElevated

@Composable
fun BoxySkullsAndWardrobeTab(
    state: GameSessionState,
    onUpdateHeroName: (String) -> Unit,
    onEquipSkull: (String, Int) -> Unit,
    onEquipOrBuyCosmetic: (String) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0 = Cabezas Skul de Boxy, 1 = Ropa Mokay Streetwear
    var selectedSlot by remember { mutableStateOf(CosmeticSlot.HEADGEAR) }

    val walkAnim by rememberInfiniteTransition(label = "boxyPreview").animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "walkPhase"
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: Live Animated Boxy Preview + Equipped 2 Skull Slots
        Surface(
            color = RetroSurfaceElevated,
            border = BorderStroke(2.dp, MokayLavender),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(0.40f).fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .background(RetroBgDark, MaterialTheme.shapes.small)
                        .border(1.5.dp, state.activeSkull().accentColor, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    BoxyAvatarPreview(
                        appearance = state.appearance,
                        activeSkullId = state.activeSkullId,
                        walkPhase = walkAnim,
                        modifier = Modifier.size(94.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${state.activeSkull().name} · ${state.activeSkull().styleType}",
                    style = MaterialTheme.typography.titleSmall,
                    color = state.activeSkull().accentColor
                )
                Text(
                    text = "ATK ${state.effectiveAtk()} · HP ${state.effectiveMaxHp()} · Saltos ${state.effectiveExtraJumps() + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroMint
                )

                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.appearance.heroName,
                    onValueChange = onUpdateHeroName,
                    label = { Text("Nombre", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MokayFlame,
                        unfocusedBorderColor = RetroBorderLight,
                        focusedTextColor = MokayCream,
                        unfocusedTextColor = MokayCream
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hero_name_input")
                )

                Spacer(Modifier.height(8.dp))
                Surface(color = RetroSurface, border = BorderStroke(1.dp, MokaySpark), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("RANURAS DE CABEZA / FÓSFORO (SKUL)", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                        Spacer(Modifier.height(4.dp))
                        Text("1 (Activa): ${state.activeSkull().name}", style = MaterialTheme.typography.bodyMedium, color = MokayCream)
                        Text("2 (Relevo): ${state.secondarySkull().name}", style = MaterialTheme.typography.bodyMedium, color = MokayLavender)
                    }
                }
            }
        }

        // Right: Skul Forms vs Mokay Streetwear Switcher
        Column(modifier = Modifier.weight(0.60f).fillMaxHeight()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Cabezas Skul de Boxy", 1 to "Ropa Streetwear Mokay").forEach { (idx, title) ->
                    val sel = mode == idx
                    Surface(
                        color = if (sel) MokayFlame else RetroBgDark,
                        border = BorderStroke(1.5.dp, if (sel) MokayCream else RetroBorderLight),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f).clickable { mode = idx }.testTag("boxy_sub_$idx")
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) MokayCharcoal else MokayCream,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (mode == 0) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(GameCatalog.skullForms, key = { it.id }) { skull ->
                        val unlocked = skull.id in state.unlockedSkulls
                        val inSlot1 = state.activeSkullId == skull.id
                        val inSlot2 = state.secondarySkullId == skull.id

                        Surface(
                            color = if (inSlot1 || inSlot2) RetroSurfaceElevated else RetroSurface,
                            border = BorderStroke(2.dp, if (inSlot1) skull.accentColor else if (inSlot2) MokayLavender else RetroBorderLight),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(RetroBgDark, MaterialTheme.shapes.small)
                                        .border(2.dp, skull.accentColor, MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BoxyAvatarPreview(
                                        appearance = state.appearance,
                                        activeSkullId = skull.id,
                                        walkPhase = walkAnim,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${skull.name} [${skull.tierLabel} · ${skull.styleType}]", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text("Hab 1: ${skull.skill1Name} · Hab 2: ${skull.skill2Name}", style = MaterialTheme.typography.labelSmall, color = skull.accentColor)
                                    Text("Swap: ${skull.swapSkillName} — ${skull.desc}", style = MaterialTheme.typography.bodyMedium, color = MokayCream.copy(alpha = 0.85f))
                                    if (!unlocked) {
                                        Text("Desbloquear: ${skull.costSparks}⚡ (o en Portales de Cabeza)", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { onEquipSkull(skull.id, 1) },
                                        enabled = !inSlot1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (inSlot1) RetroMint else skull.accentColor,
                                            contentColor = MokayCharcoal
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.testTag("skull_slot1_${skull.id}")
                                    ) {
                                        Text(if (inSlot1) "Activa" else if (!unlocked) "${skull.costSparks}⚡ R1" else "Ranura 1", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = { onEquipSkull(skull.id, 2) },
                                        enabled = !inSlot2,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (inSlot2) BirdySky else MokayLavender,
                                            contentColor = MokayCharcoal
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.testTag("skull_slot2_${skull.id}")
                                    ) {
                                        Text(if (inSlot2) "Relevo" else "Ranura 2", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        CosmeticSlot.HEADGEAR to "Gorras",
                        CosmeticSlot.JACKET to "Hoodies",
                        CosmeticSlot.PANTS_SHOES to "Zapas",
                        CosmeticSlot.BACKPACK to "Mochilas"
                    ).forEach { (slot, label) ->
                        val sel = selectedSlot == slot
                        Surface(
                            color = if (sel) MokayLavender else RetroBgDark,
                            border = BorderStroke(1.5.dp, if (sel) MokayCream else RetroBorderLight),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f).clickable { selectedSlot = slot }
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) MokayCharcoal else MokayCream, modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                val itemsForSlot = GameCatalog.cosmetics.filter { it.slot == selectedSlot }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(itemsForSlot, key = { it.id }) { item ->
                        val unlocked = item.id in state.unlockedCosmetics
                        val equipped = when (item.slot) {
                            CosmeticSlot.HEADGEAR -> state.appearance.headgearId == item.id
                            CosmeticSlot.JACKET -> state.appearance.jacketId == item.id
                            CosmeticSlot.PANTS_SHOES -> state.appearance.pantsShoesId == item.id
                            CosmeticSlot.BACKPACK -> state.appearance.backpackId == item.id
                        }
                        Surface(
                            color = if (equipped) RetroSurfaceElevated else RetroSurface,
                            border = BorderStroke(2.dp, if (equipped) item.primaryColor else RetroBorderLight),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(item.primaryColor, MaterialTheme.shapes.small)
                                        .border(2.dp, item.secondaryColor, MaterialTheme.shapes.small)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = MokayCream)
                                    Text(item.brandLine, style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                                    Text(item.skillDesc, style = MaterialTheme.typography.bodyMedium, color = MokayCream.copy(alpha = 0.85f))
                                    if (!unlocked) {
                                        Text("Nv.${item.unlockLevel} · ${item.costSparks}⚡", style = MaterialTheme.typography.labelSmall, color = MokayLavender)
                                    }
                                }
                                Button(
                                    onClick = { onEquipOrBuyCosmetic(item.id) },
                                    enabled = !equipped && state.heroLevel >= item.unlockLevel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (equipped) RetroMint else if (unlocked) MokayLavender else MokayFlame,
                                        contentColor = MokayCharcoal
                                    ),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("cosmetic_btn_${item.id}")
                                ) {
                                    Text(
                                        text = when {
                                            equipped -> "Puesto"
                                            unlocked -> "Equipar"
                                            state.heroLevel < item.unlockLevel -> "Nv.${item.unlockLevel}"
                                            else -> "Comprar"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
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
fun CompanionPetsTab(
    state: GameSessionState,
    onTogglePet: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = RetroSurfaceElevated,
            border = BorderStroke(1.5.dp, MokaySpark),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Pets, contentDescription = null, tint = MokaySpark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MASCOTAS MOKAY (${state.equippedPets.size}/3 ACTIVAS) · Se desbloquean en Misiones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MokayCream
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GameCatalog.pets, key = { it.id }) { pet ->
                val unlocked = pet.id in state.unlockedPets
                val equipIdx = state.equippedPets.indexOf(pet.id)
                val equipped = equipIdx >= 0

                Surface(
                    color = if (equipped) RetroSurfaceElevated else RetroSurface,
                    border = BorderStroke(2.dp, if (equipped) pet.accentColor else RetroBorderLight),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(RetroBgDark, MaterialTheme.shapes.small)
                                .border(2.dp, pet.accentColor, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            VectorCharacterBadge(
                                characterId = pet.id,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${pet.name} · ${pet.role}", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                            Text(pet.abilityName, style = MaterialTheme.typography.labelSmall, color = pet.accentColor)
                            Text(pet.abilityDesc, style = MaterialTheme.typography.bodyMedium, color = MokayCream.copy(alpha = 0.85f))
                            if (!unlocked) {
                                Text("Desbloqueo: ${pet.unlockHint}", style = MaterialTheme.typography.labelSmall, color = MokaySpark)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onTogglePet(pet.id) },
                            enabled = unlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (equipped) RetroMint else if (unlocked) MokayLavender else RetroBgDark,
                                contentColor = if (unlocked) MokayCharcoal else MokayCream
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("pet_btn_${pet.id}")
                        ) {
                            if (!unlocked) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Misión", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text(if (equipped) "Ranura ${equipIdx + 1}" else "Equipar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
