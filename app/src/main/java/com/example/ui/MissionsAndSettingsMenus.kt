package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.game.GameSessionState
import com.example.game.GameSettings
import com.example.ui.theme.BirdySky
import com.example.ui.theme.MokayCharcoal
import com.example.ui.theme.MokayCream
import com.example.ui.theme.MokayFlame
import com.example.ui.theme.MokayLavender
import com.example.ui.theme.MokaySpark
import com.example.ui.theme.MolotovCrimson
import com.example.ui.theme.RetroBgDark
import com.example.ui.theme.RetroBorderLight
import com.example.ui.theme.RetroMint
import com.example.ui.theme.RetroSurface
import com.example.ui.theme.RetroSurfaceElevated

@Composable
fun MissionsTab(
    state: GameSessionState,
    onClaimMission: (String) -> Unit,
    onSummonMolotovBoss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = RetroSurfaceElevated,
            border = BorderStroke(1.5.dp, MolotovCrimson),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MolotovCrimson, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("ENEMIGOS STREET & JEFE MOLOTOV", style = MaterialTheme.typography.titleSmall, color = MokayCream)
                        Text(
                            text = "Enfrenta a Laughing Cat, Street Dino y al Jefe Molotov Mokay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MokayCream.copy(alpha = 0.85f)
                        )
                    }
                }
                Button(
                    onClick = onSummonMolotovBoss,
                    colors = ButtonDefaults.buttonColors(containerColor = MolotovCrimson, contentColor = MokayCream),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("summon_molotov_boss_btn")
                ) {
                    Text("Retar Molotov", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(GameCatalog.missions, key = { it.id }) { m ->
                val cur = state.missionProgressCount(m.id).coerceAtMost(m.targetCount)
                val done = cur >= m.targetCount
                val claimed = m.id in state.claimedMissionIds
                val petRewardName = m.unlocksPetId?.let { GameCatalog.petById(it).name }

                Surface(
                    color = if (claimed) RetroBgDark else if (done) RetroSurfaceElevated else RetroSurface,
                    border = BorderStroke(2.dp, if (done && !claimed) MokaySpark else RetroBorderLight),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (claimed) Icons.Filled.Check else Icons.Filled.Flag,
                            contentDescription = null,
                            tint = if (claimed) RetroMint else if (done) MokaySpark else MokayLavender,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleSmall, color = MokayCream)
                            Text(m.desc, style = MaterialTheme.typography.bodyMedium, color = MokayCream.copy(alpha = 0.85f))
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (cur.toFloat() / m.targetCount.toFloat()).coerceIn(0f, 1f) },
                                color = if (done) RetroMint else MokayFlame,
                                trackColor = RetroBgDark,
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Progreso: $cur/${m.targetCount} · Premio: +${m.rewardSparks}⚡ +${m.rewardXp} XP" +
                                    (if (petRewardName != null) " + Mascota $petRewardName" else ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MokaySpark
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onClaimMission(m.id) },
                            enabled = done && !claimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (claimed) RetroSurfaceElevated else MokaySpark,
                                contentColor = if (claimed) RetroMint else MokayCharcoal
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("claim_mission_${m.id}")
                        ) {
                            Text(if (claimed) "Listo" else if (done) "Reclamar" else "$cur/${m.targetCount}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sound & Graphics Column
        Surface(
            color = RetroSurface,
            border = BorderStroke(1.5.dp, RetroBorderLight),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Text("SONIDO", style = MaterialTheme.typography.titleSmall, color = MokaySpark)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Efectos Retro 8-Bit", style = MaterialTheme.typography.bodyMedium, color = MokayCream)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(soundEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MokayFlame),
                        modifier = Modifier.testTag("setting_sound_toggle")
                    )
                }
                Text("Volumen: ${(settings.soundVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MokayLavender)
                Slider(
                    value = settings.soundVolume,
                    onValueChange = { onUpdateSettings(settings.copy(soundVolume = it)) },
                    colors = SliderDefaults.colors(thumbColor = MokayFlame, activeTrackColor = MokayFlame),
                    modifier = Modifier.testTag("setting_volume_slider")
                )

                Spacer(Modifier.height(10.dp))
                Text("GRÁFICOS", style = MaterialTheme.typography.titleSmall, color = BirdySky)
                Spacer(Modifier.height(6.dp))
                Text("Zoom de Cámara (Cerca de Boxy)", style = MaterialTheme.typography.labelSmall, color = MokayCream)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2.30f to "Muy Cerca", 2.05f to "Normal", 1.72f to "Amplio").forEach { (z, lbl) ->
                        val active = kotlin.math.abs(settings.cameraZoom - z) < 0.08f
                        Surface(
                            color = if (active) BirdySky else RetroBgDark,
                            border = BorderStroke(1.dp, if (active) MokayCream else RetroBorderLight),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateSettings(settings.copy(cameraZoom = z)) }
                                .testTag("setting_zoom_$lbl")
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) MokayCharcoal else MokayCream,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Partículas y Chispas de Combate", style = MaterialTheme.typography.bodyMedium, color = MokayCream)
                    Switch(
                        checked = settings.highParticles,
                        onCheckedChange = { onUpdateSettings(settings.copy(highParticles = it)) },
                        modifier = Modifier.testTag("setting_particles_toggle")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Iluminación y Parallax Retro", style = MaterialTheme.typography.bodyMedium, color = MokayCream)
                    Switch(
                        checked = settings.ambientLighting,
                        onCheckedChange = { onUpdateSettings(settings.copy(ambientLighting = it)) },
                        modifier = Modifier.testTag("setting_lighting_toggle")
                    )
                }
            }
        }

        // Controls / Buttons Column
        Surface(
            color = RetroSurface,
            border = BorderStroke(1.5.dp, RetroBorderLight),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Text("BOTONES Y CONTROLES", style = MaterialTheme.typography.titleSmall, color = MokayFlame)
                Spacer(Modifier.height(8.dp))
                Text("Tamaño de Botones de Acción", style = MaterialTheme.typography.labelSmall, color = MokayCream)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.85f to "Compacto", 1.0f to "Normal", 1.18f to "Grande").forEach { (sc, lbl) ->
                        val active = kotlin.math.abs(settings.buttonScale - sc) < 0.06f
                        Surface(
                            color = if (active) MokayFlame else RetroBgDark,
                            border = BorderStroke(1.dp, if (active) MokayCream else RetroBorderLight),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateSettings(settings.copy(buttonScale = sc)) }
                                .testTag("setting_btnsize_$lbl")
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) MokayCharcoal else MokayCream,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Opacidad de Controles: ${(settings.hudOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MokayLavender)
                Slider(
                    value = settings.hudOpacity,
                    onValueChange = { onUpdateSettings(settings.copy(hudOpacity = it.coerceIn(0.45f, 1f))) },
                    valueRange = 0.45f..1f,
                    colors = SliderDefaults.colors(thumbColor = MokaySpark, activeTrackColor = MokaySpark),
                    modifier = Modifier.testTag("setting_opacity_slider")
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo Zurdo (Invertir Joystick/Botones)", style = MaterialTheme.typography.bodyMedium, color = MokayCream)
                    Switch(
                        checked = settings.leftHandedControls,
                        onCheckedChange = { onUpdateSettings(settings.copy(leftHandedControls = it)) },
                        modifier = Modifier.testTag("setting_lefthanded_toggle")
                    )
                }
            }
        }
    }
}
