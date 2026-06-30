/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sborkapc.data.SavedBuild
import com.example.sborkapc.data.SavedComponent
import com.example.sborkapc.ui.components.GradientButton
import com.example.sborkapc.ui.theme.AppTheme
import com.example.sborkapc.ui.viewmodel.BuildViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedBuildsScreen(
    viewModel: BuildViewModel,
    onEditClick: () -> Unit
) {
    val state = viewModel.uiState
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val gson = remember { Gson() }

    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("ИСТОРИЯ СБОРОК", color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { p ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshHistoryBuilds() },
            modifier = Modifier.padding(p),
            state = pullRefreshState,
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = state.isRefreshing,
                    containerColor = AppTheme.SurfaceDim,
                    color = AppTheme.NeonCyan,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (state.savedBuilds.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = AppTheme.TextMuted, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("У вас пока нет сохраненных сборок", color = AppTheme.TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(state.savedBuilds) { build ->
                        BuildHistoryItem(
                            build = build,
                            dateFormat = dateFormat,
                            gson = gson,
                            onDelete = { viewModel.deleteSavedBuild(build.id) },
                            onEdit = {
                                viewModel.loadSavedBuild(build)
                                onEditClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildHistoryItem(
    build: SavedBuild,
    dateFormat: SimpleDateFormat,
    gson: Gson,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val components = remember(build.componentsJson) {
        gson.fromJson(build.componentsJson, Array<SavedComponent>::class.java).toList()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppTheme.CardBg,
        border = BorderStroke(1.dp, if (expanded) AppTheme.NeonCyan.copy(0.3f) else AppTheme.GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        dateFormat.format(Date(build.timestamp)),
                        color = AppTheme.NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${String.format("%,d", build.totalPrice)} ₽",
                        color = AppTheme.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${components.size} компонентов",
                        color = AppTheme.TextMuted,
                        fontSize = 12.sp
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null, tint = AppTheme.NeonRed.copy(0.6f))
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = AppTheme.TextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Divider(color = AppTheme.GlassBorder)
                    Spacer(Modifier.height(12.dp))
                    
                    components.forEach { comp ->
                        Row(
                            Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = comp.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(AppTheme.SurfaceDim),
                                contentScale = ContentScale.Crop
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(comp.name, color = AppTheme.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(comp.category, color = AppTheme.TextMuted, fontSize = 10.sp)
                            }
                            val priceToShow = when(comp.selectedStore) {
                                "regard" -> comp.priceRegard
                                "dns" -> comp.priceDns
                                else -> comp.price
                            }
                            Text(priceToShow, color = AppTheme.NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    GradientButton(
                        text = "РЕДАКТИРОВАТЬ",
                        brush = AppTheme.GradientPurple,
                        onClick = onEdit,
                        icon = Icons.Default.Edit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

