/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sborkapc.ui.components.*
import com.example.sborkapc.ui.theme.AppTheme
import com.example.sborkapc.ui.theme.Red40
import com.example.sborkapc.ui.viewmodel.BuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    viewModel: BuildViewModel,
    onNavigateToCategory: (String) -> Unit,
    onFinish: () -> Unit = {}
) {
    val state = viewModel.uiState
    var errorsExpanded by remember { mutableStateOf(false) }
    var priceExpanded by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f, label = "glow",
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val hasErrors = state.compatibilityErrors.isNotEmpty()
    val uniqueFilledCount = remember(state.savedComponents) {
        state.savedComponents.map { it.category }.distinct().size
    }
    val isComplete = uniqueFilledCount == assemblyOrder.size
    val nextStep = assemblyOrder.firstOrNull { cat ->
        state.savedComponents.none { it.category == cat }
    }

    if (showOptimizeDialog) {
        AlertDialog(
            onDismissRequest = { showOptimizeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = AppTheme.NeonCyan)
                    Spacer(Modifier.width(12.dp))
                    Text("Магическая оптимизация", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    "Приложение проанализирует цены во всех доступных магазинах и автоматически переключит каждый товар на вариант с самой низкой ценой. Вы уверены, что хотите оптимизировать стоимость сборки?",
                    color = AppTheme.TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.optimizeBuildPrice()
                    showOptimizeDialog = false
                }) {
                    Text("ОПТИМИЗИРОВАТЬ", color = AppTheme.NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptimizeDialog = false }) {
                    Text("ОТМЕНА", color = AppTheme.TextSecondary)
                }
            },
            containerColor = AppTheme.CardBgElevated,
            titleContentColor = AppTheme.TextPrimary,
            textContentColor = AppTheme.TextSecondary,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "РЕДАКТИРОВАНИЕ" else "МОЯ СБОРКА",
                        color = AppTheme.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (state.isEditing) {
                        TextButton(onClick = {
                            viewModel.cancelEditing()
                            onFinish()
                        }) {
                            Text("ОТМЕНА", color = Red40, fontSize = 12.sp)
                        }
                    }
                    if (state.savedComponents.isNotEmpty()) {
                        IconButton(onClick = { showOptimizeDialog = true }) {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = "Оптимизировать цену",
                                tint = AppTheme.NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                color = AppTheme.CardBgElevated,
                border = BorderStroke(1.dp, AppTheme.GlassBorderActive),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

                    // ── Прогресс ──────────────────────────────────────────
                    val animatedProgress by animateFloatAsState(
                        targetValue = state.buildProgress,
                        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
                        label = "progress"
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(
                            "ПРОГРЕСС",
                            color = AppTheme.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$uniqueFilledCount / ${assemblyOrder.size}",
                            color = AppTheme.NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(AppTheme.Divider, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(AppTheme.NeonCyan, CircleShape)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Цена + Кнопка ─────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priceExpanded = !priceExpanded }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "ИТОГО",
                                    color = AppTheme.TextMuted,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Icon(
                                    if (priceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = AppTheme.TextMuted,
                                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                )
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(visible = priceExpanded) {
                                Column(Modifier.padding(vertical = 8.dp)) {
                                    PriceSummaryRow("Гибридная", state.totalPrice, AppTheme.TextPrimary)
                                    if (state.totalCitilink > 0) PriceSummaryRow("Citilink", state.totalCitilink, AppTheme.NeonGreen)
                                    if (state.totalRegard > 0) PriceSummaryRow("Regard", state.totalRegard, AppTheme.NeonCyan)
                                    if (state.totalDns > 0) PriceSummaryRow("DNS", state.totalDns, AppTheme.NeonYellow)
                                }
                            }

                            if (!priceExpanded) {
                                Text(
                                    "${String.format("%,d", state.totalPrice)} ₽",
                                    color = AppTheme.TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(modifier = Modifier.padding(bottom = 4.dp)) {
                            when {
                                isComplete && !hasErrors -> {
                                    GradientButton(
                                        text = if (state.isEditing) "ОБНОВИТЬ" else "СОХРАНИТЬ",
                                        brush = AppTheme.GradientSuccess,
                                        modifier = Modifier
                                            .height(46.dp)
                                            .widthIn(min = 120.dp, max = 160.dp),
                                        onClick = {
                                            viewModel.saveCurrentBuild()
                                            onFinish()
                                        },
                                        icon = Icons.Default.CheckCircle
                                    )
                                }
                                nextStep != null -> {
                                    GradientButton(
                                        text = categoryMeta[nextStep]?.shortName ?: nextStep.take(10),
                                        brush = AppTheme.GradientCyan,
                                        modifier = Modifier
                                            .height(46.dp)
                                            .widthIn(min = 120.dp, max = 160.dp),
                                        onClick = { onNavigateToCategory(nextStep) },
                                        icon = Icons.Default.Add
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { p ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshActiveBuild() },
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
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    val statusColor = when {
                        hasErrors  -> AppTheme.NeonRed
                        isComplete -> AppTheme.NeonGreen
                        else       -> AppTheme.NeonCyan
                    }
                    val statusText = when {
                        hasErrors        -> "Обнаружены конфликты"
                        isComplete       -> "Сборка готова к сохранению"
                        nextStep != null -> "Следующий шаг: $nextStep"
                        else             -> "Проверка..."
                    }
                    val statusIcon = when {
                        hasErrors  -> Icons.Default.Warning
                        isComplete -> Icons.Default.Verified
                        else       -> Icons.Default.Info
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasErrors || isComplete)
                                    Modifier.neonGlow(statusColor, alpha = glowAlpha * 0.5f)
                                else Modifier
                            )
                            .clickable { if (hasErrors || state.compatibilityAdvice.isNotEmpty()) errorsExpanded = !errorsExpanded },
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            statusColor.copy(alpha = if (hasErrors) glowAlpha else 0.4f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (hasErrors || state.compatibilityAdvice.isNotEmpty()) {
                                    Icon(
                                        if (errorsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null,
                                        tint = statusColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(visible = errorsExpanded) {
                                Column {
                                    if (state.compatibilityErrors.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        state.compatibilityErrors.forEach { err ->
                                            Text("• $err", color = AppTheme.NeonRed.copy(0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                                        }
                                    }
                                    if (state.compatibilityAdvice.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        state.compatibilityAdvice.forEach { adv ->
                                            Text("💡 $adv", color = AppTheme.NeonAmber.copy(0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                assemblyOrder.forEach { category ->
                    val components = state.savedComponents.filter { it.category == category }
                    val isNext     = category == nextStep

                    if (components.isEmpty()) {
                        item(key = category) {
                            ComponentSlotRow(
                                category  = category,
                                component = null,
                                isNext    = isNext,
                                glowAlpha = glowAlpha,
                                onRemove  = {},
                                onAdd     = { onNavigateToCategory(category) }
                            )
                        }
                    } else {
                        val grouped = components.groupBy { it.remoteId }
                        grouped.values.forEachIndexed { groupIndex, groupList ->
                            val comp = groupList.first()
                            item(key = "${category}_${comp.remoteId}") {
                                ComponentSlotRow(
                                    category  = if (groupIndex == 0) category else "",
                                    component = comp,
                                    isNext    = false,
                                    glowAlpha = glowAlpha,
                                    quantity  = groupList.size,
                                    onRemove  = { viewModel.removeComponentById(comp.id) },
                                    onAdd     = { onNavigateToCategory(category) },
                                    onStoreChange = { id, store -> viewModel.updateComponentStore(id, store) }
                                )
                            }
                        }

                        if (category in multiInstanceCategories) {
                            item(key = "${category}_add_more") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TextButton(
                                        onClick = { onNavigateToCategory(category) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.NeonCyan),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ДОБАВИТЬ ЕЩЕ", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun PriceSummaryRow(label: String, amount: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AppTheme.TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${String.format("%,d", amount)} ₽",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
