/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sborkapc.data.SavedComponent
import com.example.sborkapc.ui.theme.AppTheme

val assemblyOrder = listOf(
    "Процессоры", "Материнские платы", "Кулеры",
    "Оперативная память", "SSD", "Видеокарты",
    "Блоки питания", "Корпуса"
)

val multiInstanceCategories = listOf("Оперативная память", "SSD", "Видеокарты")

data class CategoryMeta(val icon: ImageVector, val shortName: String, val description: String)

val categoryMeta = mapOf(
    "Процессоры"       to CategoryMeta(Icons.Default.Memory,            "Процессор",  "Сердце системы"),
    "Материнские платы" to CategoryMeta(Icons.Default.DeveloperBoard,  "Мат. плата", "Основа платформы"),
    "Кулеры"           to CategoryMeta(Icons.Default.AcUnit,           "Кулер",  "Система охлаждения"),
    "Оперативная память" to CategoryMeta(Icons.Default.SdCard,         "ОЗУ",  "Быстрая память"),
    "SSD"              to CategoryMeta(Icons.Default.Storage,           "SSD",  "Накопитель"),
    "Видеокарты"       to CategoryMeta(Icons.Default.ScreenshotMonitor, "Видеокарта",  "Графический процессор"),
    "Блоки питания"    to CategoryMeta(Icons.Default.Power,            "Блок питания",  "Источник питания"),
    "Корпуса"          to CategoryMeta(Icons.Default.WebAsset,         "Корпус", "Шасси системы")
)

fun Modifier.neonGlow(color: Color, radius: Dp = 12.dp, alpha: Float = 0.6f): Modifier =
    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = alpha).toArgb())
                }
            }
            canvas.drawRoundRect(
                left   = 0f, top    = 0f,
                right  = size.width, bottom = size.height,
                radiusX = 24.dp.toPx(), radiusY = 24.dp.toPx(),
                paint  = paint
            )
        }
    }

@Composable
fun GradientButton(
    text: String,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NeonChip(
    text: String,
    color: Color = AppTheme.NeonCyan,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = CircleShape,
        color = if (selected) color.copy(alpha = 0.2f) else AppTheme.CardBgElevated,
        border = BorderStroke(1.dp, if (selected) color else AppTheme.GlassBorder)
    ) {
        Text(
            text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) color else AppTheme.TextSecondary,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ComponentSlotRow(
    category: String,
    component: SavedComponent?,
    isNext: Boolean,
    glowAlpha: Float,
    onRemove: () -> Unit,
    onAdd: (() -> Unit)? = null,
    onStoreChange: ((Int, String) -> Unit)? = null,
    showAddMore: Boolean = false,
    quantity: Int = 1
) {
    val meta = categoryMeta[category]
    val borderColor = when {
        component != null -> AppTheme.NeonGreen.copy(alpha = 0.4f)
        isNext            -> AppTheme.NeonCyan.copy(alpha = glowAlpha)
        else              -> AppTheme.GlassBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isNext) Modifier.neonGlow(AppTheme.NeonCyan, alpha = glowAlpha * 0.4f) else Modifier)
            .then(if (onAdd != null) Modifier.clickable { onAdd() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = if (component != null) AppTheme.CardBgElevated else AppTheme.CardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.SurfaceDim),
                contentAlignment = Alignment.Center
            ) {
                if (component != null) {
                    AsyncImage(model = component.imageUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(
                        meta?.icon ?: Icons.Default.Add, null,
                        tint = if (isNext) AppTheme.NeonCyan else AppTheme.TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        meta?.shortName ?: category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = when {
                            component != null -> AppTheme.NeonGreen
                            isNext            -> AppTheme.NeonCyan
                            else              -> AppTheme.TextMuted
                        }
                    )
                    if (quantity > 1) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = AppTheme.NeonCyan.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                "x$quantity",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = AppTheme.NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    component?.name ?: if (isNext) "Нажмите, чтобы выбрать →" else "Не выбрано",
                    color = if (component != null) AppTheme.TextPrimary else AppTheme.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (component != null) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (component != null) {
                    var expanded by remember { mutableStateOf(false) }
                    val hasRegard = component.priceRegard != "---" && component.priceRegard != "0" && !component.priceRegard.contains("Нет в наличии", true)
                    val hasCitilink = component.price != "---" && component.price != "0" && !component.price.contains("Нет в наличии", true)
                    val hasDns = component.priceDns != "---" && component.priceDns != "0" && !component.priceDns.contains("Нет в наличии", true)
                    
                    // Авто-выбор магазина с ценой, если в выбранном пусто
                    val effectiveStore = when {
                        component.selectedStore == "regard" && hasRegard -> "regard"
                        component.selectedStore == "citilink" && hasCitilink -> "citilink"
                        component.selectedStore == "dns" && hasDns -> "dns"
                        hasCitilink -> "citilink"
                        hasRegard -> "regard"
                        hasDns -> "dns"
                        else -> component.selectedStore
                    }
                    
                    val (currentPrice, storeColor) = when (effectiveStore) {
                        "regard" -> component.priceRegard to AppTheme.NeonCyan
                        "dns"    -> component.priceDns to AppTheme.NeonYellow
                        else     -> component.price to AppTheme.NeonGreen
                    }
                    val storeLabel = if (effectiveStore == "dns") "DNS" else effectiveStore.replaceFirstChar { it.uppercase() }

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = (hasRegard && hasCitilink) || (hasRegard && hasDns) || (hasCitilink && hasDns)) { expanded = true }
                        ) {
                            Text(
                                currentPrice,
                                color = storeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if ((hasRegard && hasCitilink) || (hasRegard && hasDns) || (hasCitilink && hasDns)) {
                                Icon(
                                    Icons.Default.ExpandMore,
                                    null,
                                    tint = storeColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp).padding(start = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                storeLabel,
                                color = AppTheme.TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(AppTheme.CardBgElevated)
                        ) {
                            if (hasCitilink) {
                                DropdownMenuItem(
                                    text = { Text("Citilink: ${component.price}", color = AppTheme.NeonGreen, fontSize = 12.sp) },
                                    onClick = { 
                                        onStoreChange?.invoke(component.id, "citilink")
                                        expanded = false 
                                    }
                                )
                            }
                            if (hasRegard) {
                                DropdownMenuItem(
                                    text = { Text("Regard: ${component.priceRegard}", color = AppTheme.NeonCyan, fontSize = 12.sp) },
                                    onClick = { 
                                        onStoreChange?.invoke(component.id, "regard")
                                        expanded = false 
                                    }
                                )
                            }
                            if (hasDns) {
                                DropdownMenuItem(
                                    text = { Text("DNS: ${component.priceDns}", color = AppTheme.NeonYellow, fontSize = 12.sp) },
                                    onClick = { 
                                        onStoreChange?.invoke(component.id, "dns")
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (component != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null,
                        tint = AppTheme.NeonRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            } else if (showAddMore && onAdd != null) {
                IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, null, tint = AppTheme.NeonCyan, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
