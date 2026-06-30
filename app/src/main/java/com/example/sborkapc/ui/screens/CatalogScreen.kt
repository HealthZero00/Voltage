/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sborkapc.ui.components.assemblyOrder
import com.example.sborkapc.ui.components.categoryMeta
import com.example.sborkapc.ui.theme.AppTheme
import com.example.sborkapc.ui.viewmodel.BuildUiState

@Composable
fun CatalogScreen(
    uiState: BuildUiState,
    onCategoryClick: (String) -> Unit
) {
    val filled = uiState.savedComponents.size
    val total  = assemblyOrder.size

    Scaffold(containerColor = AppTheme.DarkBg) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Вольтаж", color = AppTheme.TextPrimary,
                            fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("ПОМОЩНИК В СБОРКЕ ПК", color = AppTheme.TextMuted,
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { uiState.buildProgress },
                            modifier = Modifier.size(52.dp),
                            color = AppTheme.NeonCyan,
                            trackColor = AppTheme.Divider,
                            strokeWidth = 3.dp
                        )
                        Text("$filled/$total", color = AppTheme.NeonCyan,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("КОМПЛЕКТУЮЩИЕ", color = AppTheme.TextSecondary,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(assemblyOrder) { index, cat ->
                val animAlpha = remember { Animatable(0f) }
                LaunchedEffect(Unit) { animAlpha.animateTo(1f, tween(350, index * 50)) }

                val saved = uiState.savedComponents.find { it.category == cat }
                val meta  = categoryMeta[cat]

                Surface(
                    onClick = { onCategoryClick(cat) },
                    modifier = Modifier.fillMaxWidth().alpha(animAlpha.value),
                    color = if (saved != null) AppTheme.CardBgElevated else AppTheme.CardBg,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (saved != null) AppTheme.NeonGreen.copy(0.35f) else AppTheme.GlassBorder
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (saved != null) AppTheme.NeonGreen.copy(0.15f)
                                    else AppTheme.NeonCyan.copy(0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                meta?.icon ?: Icons.Default.Settings, null,
                                tint = if (saved != null) AppTheme.NeonGreen else AppTheme.NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(cat, color = AppTheme.TextPrimary,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                saved?.name ?: (meta?.description ?: ""),
                                color = if (saved != null) AppTheme.NeonGreen else AppTheme.TextMuted,
                                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (saved != null) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = AppTheme.NeonGreen, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.ChevronRight, null,
                                tint = AppTheme.TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
