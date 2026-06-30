/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.sborkapc.network.Component
import com.example.sborkapc.ui.components.GradientButton
import com.example.sborkapc.ui.components.NeonChip
import com.example.sborkapc.ui.theme.AppTheme
import com.example.sborkapc.ui.viewmodel.BuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    catName: String,
    viewModel: BuildViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(catName) { viewModel.fetchComponents(catName) }
    val state = viewModel.uiState
    val filterState = state.categoryFilters[catName] ?: com.example.sborkapc.ui.viewmodel.CategoryFilterState()
    
    var selectedItem by remember { mutableStateOf<Component?>(null) }
    
    // Запоминаем, что было выбрано в момент открытия категории, чтобы список не "прыгал"
    val initiallySelectedIds = remember(catName) {
        state.savedComponents.filter { it.category == catName }.map { it.remoteId }.toSet()
    }
    
    val searchQuery = filterState.searchQuery
    val selectedFilters = filterState.selectedFilters
    val filtersExpanded = filterState.filtersExpanded
    val minPriceQuery = filterState.minPrice
    val maxPriceQuery = filterState.maxPrice
    val sortOrder = filterState.sortOrder
    val storeFilter = filterState.storeFilter

    val availablePriceRange = remember(state.componentsList) {
        val prices = state.componentsList
            .flatMap { item ->
                listOf(item.price, item.priceRegard, item.priceDns)
                    .map { it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
            }
            .filter { it > 0 }
        if (prices.isEmpty()) null else prices.minOrNull()!! to prices.maxOrNull()!!
    }

    // ── Конфигурация фильтров на основе таблицы (Top Priority) ────────────
    val filterLabels = remember(catName) {
        when (catName) {
            "Процессоры"        -> listOf("СОКЕТ", "ЯДРА/ПОТОКИ", "TDP")
            "Материнские платы" -> listOf("СОКЕТ", "ЧИПСЕТ", "ТИП ПАМЯТИ", "ФОРМ-ФАКТОР")
            "Видеокарты"        -> listOf("VRAM", "ДЛИНА", "ДОП. ПИТАНИЕ")
            "Оперативная память"-> listOf("ТИП ПАМЯТИ", "ЧАСТОТА", "ОБЪЕМ", "МОДУЛЕЙ")
            "Блоки питания"     -> listOf("МОЩНОСТЬ", "СЕРТИФИКАТ", "ТИП КАБЕЛЕЙ")
            "SSD"               -> listOf("ИНТЕРФЕЙС", "ОБЪЕМ", "РЕСУРС (TBW)")
            "Корпуса"           -> listOf("МАКС. ГПУ", "ФОРМ-ФАКТОР ПЛАТ", "МАКС. КУЛЕР")
            "Кулеры"            -> listOf("TDP (W)", "СОКЕТ", "ВЫСОТА")
            else                -> emptyList()
        }
    }

    // ── Извлечение опций для фильтров ─────────────────────────────────────
    val filterOptions = remember(state.componentsList, filterLabels) {
        filterLabels.associateWith { label ->
            state.componentsList
                .flatMap { item ->
                    val value = getFilterValue(item, label) ?: ""
                    // Разделяем значения для сокетов и форматов плат
                    if (label == "СОКЕТ" || label == "ФОРМ-ФАКТОР ПЛАТ") {
                        value.split(",").map { it.trim() }
                    } else listOf(value)
                }
                .filter { it.isNotBlank() && it != "---" && it != "null" }
                .distinct()
                .sortedWith(compareBy<String> { it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }.thenBy { it })
        }
    }

    // ── Фильтрация и Сортировка ───────────────────────────────────────────
    val filteredList = remember(state.componentsList, filterState) {
        val filtered = state.componentsList.filter { item ->
            // Обязательно наличие хотя бы одной цены
            val hasCitilink = item.price != "---" && item.price != "0" && !item.price.contains("Нет в наличии", true)
            val hasRegard = item.priceRegard != "---" && item.priceRegard != "0" && !item.priceRegard.contains("Нет в наличии", true)
            val hasDns = item.priceDns != "---" && item.priceDns != "0" && !item.priceDns.contains("Нет в наличии", true)
            
            if (!hasCitilink && !hasRegard && !hasDns) return@filter false

            // Фильтр по магазину
            val matchesStore = when (storeFilter) {
                "citilink" -> hasCitilink
                "regard"   -> hasRegard
                "dns"      -> hasDns
                else       -> true
            }
            if (!matchesStore) return@filter false

            // Используем лучшую доступную цену для фильтрации по диапазону
            val prices = listOf(item.price, item.priceRegard, item.priceDns)
                .map { it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                .filter { it > 0 }
            
            val itemPrice = if (prices.isEmpty()) 0 else prices.minOrNull()!!
            
            val minP = minPriceQuery.toIntOrNull() ?: 0
            val maxP = maxPriceQuery.toIntOrNull() ?: Int.MAX_VALUE

            val matchesPrice = itemPrice in minP..maxP || (itemPrice == 0 && minP == 0)
            val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesFilters = selectedFilters.all { (label, selectedValues) ->
                if (selectedValues.isEmpty()) return@all true
                val itemValue = getFilterValue(item, label) ?: ""
                
                if (label == "СОКЕТ" || label == "ФОРМ-ФАКТОР ПЛАТ") {
                    val itemParts = itemValue.split(",").map { it.trim().lowercase() }
                    selectedValues.any { sel -> 
                        itemParts.any { it.contains(sel.lowercase()) } 
                    }
                } else {
                    selectedValues.contains(itemValue)
                }
            }
            matchesSearch && matchesFilters && matchesPrice
        }

        val sorted = when (sortOrder) {
            1 -> filtered.sortedBy { item ->
                val pStr = when (storeFilter) {
                    "citilink" -> item.price
                    "regard"   -> item.priceRegard
                    "dns"      -> item.priceDns
                    else       -> listOf(item.price, item.priceRegard, item.priceDns)
                        .filter { it != "---" && it != "0" && !it.contains("Нет в наличии", true) }
                        .minByOrNull { it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: Int.MAX_VALUE } ?: ""
                }
                pStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: Int.MAX_VALUE
            }
            2 -> filtered.sortedByDescending { item ->
                val pStr = when (storeFilter) {
                    "citilink" -> item.price
                    "regard"   -> item.priceRegard
                    "dns"      -> item.priceDns
                    else       -> listOf(item.price, item.priceRegard, item.priceDns)
                        .filter { it != "---" && it != "0" && !it.contains("Нет в наличии", true) }
                        .maxByOrNull { it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 } ?: ""
                }
                pStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
            else -> filtered
        }

        // ПЕРЕМЕЩАЕМ ТОЛЬКО ТЕ, ЧТО БЫЛИ ВЫБРАНЫ ПРИ ВХОДЕ, ВВЕРХ
        sorted.sortedByDescending { item ->
            initiallySelectedIds.contains(item.id)
        }
    }

    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(catName, color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${filteredList.size} моделей", color = AppTheme.TextMuted, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.TextPrimary)
                    }
                },
                actions = {
                    val hasFilters = selectedFilters.isNotEmpty() || searchQuery.isNotEmpty() || 
                                     minPriceQuery.isNotEmpty() || maxPriceQuery.isNotEmpty() || 
                                     sortOrder != 0 || storeFilter != "all"
                    if (hasFilters) {
                        TextButton(onClick = { 
                            viewModel.updateCategoryFilter(catName) { 
                                it.copy(
                                    searchQuery = "",
                                    selectedFilters = emptyMap(),
                                    minPrice = "",
                                    maxPrice = "",
                                    sortOrder = 0,
                                    storeFilter = "all"
                                )
                            }
                        }) {
                            Text("СБРОС", color = AppTheme.NeonCyan, fontSize = 12.sp)
                        }
                    }

                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = if (sortOrder != 0 || storeFilter != "all") AppTheme.NeonCyan else AppTheme.TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(AppTheme.CardBgElevated)
                        ) {
                            Text("СОРТИРОВКА", color = AppTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp))
                            DropdownMenuItem(
                                text = { Text("По умолчанию", color = if (sortOrder == 0) AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(sortOrder = 0) }
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Сначала дешевые", color = if (sortOrder == 1) AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(sortOrder = 1) }
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Сначала дорогие", color = if (sortOrder == 2) AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(sortOrder = 2) }
                                    showSortMenu = false 
                                }
                            )
                            
                            HorizontalDivider(color = AppTheme.GlassBorder.copy(alpha = 0.1f))
                            Text("МАГАЗИН", color = AppTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp))
                            
                            DropdownMenuItem(
                                text = { Text("Все магазины", color = if (storeFilter == "all") AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "all") }
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Citilink", color = if (storeFilter == "citilink") AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "citilink") }
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Regard", color = if (storeFilter == "regard") AppTheme.NeonCyan else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "regard") }
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("DNS", color = if (storeFilter == "dns") AppTheme.NeonYellow else AppTheme.TextPrimary) },
                                onClick = { 
                                    viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "dns") }
                                    showSortMenu = false 
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            
            // ── Поиск ─────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.SurfaceDim,
                border = BorderStroke(1.dp, AppTheme.GlassBorder)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = AppTheme.TextMuted, modifier = Modifier.size(20.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { newVal ->
                            viewModel.updateCategoryFilter(catName) { it.copy(searchQuery = newVal) }
                        },
                        placeholder = { Text("Поиск по названию...", fontSize = 14.sp, color = AppTheme.TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AppTheme.NeonCyan, focusedTextColor = AppTheme.TextPrimary, unfocusedTextColor = AppTheme.TextPrimary
                        ),
                        textStyle = TextStyle(fontSize = 14.sp),
                        singleLine = true
                    )
                    IconButton(onClick = { 
                        viewModel.updateCategoryFilter(catName) { it.copy(filtersExpanded = !it.filtersExpanded) }
                    }) {
                        Icon(
                            if (filtersExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (selectedFilters.isNotEmpty()) AppTheme.NeonCyan else AppTheme.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Быстрый выбор магазина ────────────────────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    NeonChip(
                        text = "Все цены",
                        selected = storeFilter == "all",
                        onClick = { viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "all") } }
                    )
                }
                item {
                    NeonChip(
                        text = "Citilink",
                        color = AppTheme.NeonGreen,
                        selected = storeFilter == "citilink",
                        onClick = { viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "citilink") } }
                    )
                }
                item {
                    NeonChip(
                        text = "Regard",
                        color = AppTheme.NeonCyan,
                        selected = storeFilter == "regard",
                        onClick = { viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "regard") } }
                    )
                }
                item {
                    NeonChip(
                        text = "DNS",
                        color = AppTheme.NeonYellow,
                        selected = storeFilter == "dns",
                        onClick = { viewModel.updateCategoryFilter(catName) { it.copy(storeFilter = "dns") } }
                    )
                }
            }

            // ── Умные фильтры (Теги) ──────────────────────────────────────

            // ── Умные фильтры (Теги) ──────────────────────────────────────
            if (filtersExpanded) {
                Column(Modifier.padding(bottom = 8.dp)) {
                    // Фильтр по цене
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "ЦЕНА (РУБ.)", 
                                color = AppTheme.TextMuted, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 1.2.sp
                            )
                            if (availablePriceRange != null) {
                                Text(
                                    "от ${availablePriceRange.first} до ${availablePriceRange.second}",
                                    color = AppTheme.NeonCyan.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PriceInputField(
                                value = minPriceQuery,
                                onValueChange = { newVal -> 
                                    if (newVal.all { it.isDigit() }) {
                                        viewModel.updateCategoryFilter(catName) { it.copy(minPrice = newVal) }
                                    }
                                },
                                placeholder = "От",
                                modifier = Modifier.weight(1f)
                            )
                            Text("—", color = AppTheme.TextMuted, fontWeight = FontWeight.Bold)
                            PriceInputField(
                                value = maxPriceQuery,
                                onValueChange = { newVal ->
                                    if (newVal.all { it.isDigit() }) {
                                        viewModel.updateCategoryFilter(catName) { it.copy(maxPrice = newVal) }
                                    }
                                },
                                placeholder = "До",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (filterOptions.any { it.value.isNotEmpty() }) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            filterOptions.forEach { (label, options) ->
                                if (options.isNotEmpty()) {
                                    item(key = label) {
                                        Column {
                                            Text(label, color = AppTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                            Spacer(Modifier.height(4.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(options) { option ->
                                                    val isSelected = selectedFilters[label]?.contains(option) == true
                                                    NeonChip(
                                                        text = option,
                                                        selected = isSelected,
                                                        onClick = {
                                                            viewModel.updateCategoryFilter(catName) { state ->
                                                                val currentSet = state.selectedFilters[label] ?: emptySet()
                                                                val newSet = if (isSelected) currentSet - option else currentSet + option
                                                                val newFilters = state.selectedFilters.toMutableMap()
                                                                if (newSet.isEmpty()) newFilters.remove(label)
                                                                else newFilters[label] = newSet
                                                                state.copy(selectedFilters = newFilters)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp), color = AppTheme.GlassBorder)
                }
            }

            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.fetchComponents(catName, force = true) },
                modifier = Modifier.weight(1f),
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
                if (state.isLoading && !state.isRefreshing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppTheme.NeonCyan, strokeWidth = 2.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Загрузка компонентов...", color = AppTheme.TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(filteredList) { item ->
                            val savedOfThisType = state.savedComponents.filter { 
                                it.category == item.category && it.name == item.name 
                            }
                            val count = savedOfThisType.size
                            val isMulti = item.category in listOf("SSD", "Оперативная память", "Видеокарты")
                            
                            ProductCard(
                                item = item,
                                count = count,
                                isMulti = isMulti,
                                activeStore = storeFilter,
                                onDetails = { selectedItem = item },
                                onAdd = { viewModel.addComponent(item) },
                                onRemove = {
                                    savedOfThisType.firstOrNull()?.let {
                                        viewModel.removeComponentById(it.id)
                                    }
                                }
                            )
                        }
                        
                        if (filteredList.isEmpty() && (searchQuery.isNotEmpty() || selectedFilters.isNotEmpty() || minPriceQuery.isNotEmpty() || maxPriceQuery.isNotEmpty())) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Ничего не найдено", color = AppTheme.TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        val currentItem = selectedItem!!
        val isMulti = currentItem.category in listOf("SSD", "Оперативная память", "Видеокарты")
        val count = state.savedComponents.count { 
            it.category == currentItem.category && it.name == currentItem.name 
        }

        ComponentDetailDialog(
            item = currentItem,
            count = count,
            isMulti = isMulti,
            onDismiss = { selectedItem = null },
            onAdd = {
                viewModel.addComponent(currentItem)
                if (!isMulti) selectedItem = null
            },
            onRemove = {
                state.savedComponents.find { 
                    it.category == currentItem.category && it.name == currentItem.name 
                }?.let { viewModel.removeComponentById(it.id) }
            }
        )
    }
}

@Composable
fun PriceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = AppTheme.CardBg,
        border = BorderStroke(1.dp, AppTheme.GlassBorder)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = AppTheme.TextPrimary, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(AppTheme.NeonCyan),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 12.sp,
                            color = AppTheme.TextMuted
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun getFilterValue(item: Component, label: String): String? {
    val allSpecs = (item.specs ?: emptyMap()) + (item.specsCitilink ?: emptyMap()) + (item.specsRegard ?: emptyMap()) + (item.specsDns ?: emptyMap())
    
    return when (label) {
        "СОКЕТ" -> item.socket.takeIf { it != "---" } ?: allSpecs["Сокет"]?.toString() ?: allSpecs["Socket"]?.toString()
        
        "ЯДРА/ПОТОКИ" -> allSpecs["Число ядер"]?.toString() ?: allSpecs["Количество ядер"]?.toString()

        "TDP", "TDP (W)" -> {
            val tdp = if (item.maxTdp > 0) item.maxTdp else item.tdp
            if (tdp > 0) "${tdp} Вт" 
            else allSpecs["Тепловыделение"]?.toString() ?: allSpecs["Макс. TDP"]?.toString() ?: allSpecs["Макс, рассеиваемая мощность (TDP, Вт)"]?.toString()
        }

        "ЧИПСЕТ" -> item.chipset.takeIf { it != "---" } ?: allSpecs["Чипсет"]?.toString()
        
        "ТИП ПАМЯТИ" -> item.ramType.takeIf { it != "---" } ?: allSpecs["Тип памяти"]?.toString()
        
        "ФОРМ-ФАКТОР" -> item.formFactor.takeIf { it != "---" } ?: allSpecs["Форм-фактор"]?.toString() ?: allSpecs["Типоразмер"]?.toString()
        
        "VRAM" -> if (item.vram > 0) "${item.vram} ГБ" else allSpecs["Объем видеопамяти"]?.toString()
        
        "ДЛИНА" -> if (item.gpuLength > 0) "${item.gpuLength} мм" else allSpecs["Длина видеокарты"]?.toString()
        
        "ДОП. ПИТАНИЕ" -> item.gpuPowerPin.takeIf { it != "---" } ?: allSpecs["Разъемы дополнительного питания"]?.toString()

        "ЧАСТОТА" -> if (item.ramMaxFreq > 0) "${item.ramMaxFreq} МГц" else allSpecs["Тактовая частота"]?.toString()
        
        "ОБЪЕМ" -> {
            val specTotal = allSpecs["Суммарный объем памяти всего комплекта"]?.toString()
                ?: allSpecs["Объем накопителя"]?.toString()
            if (specTotal != null && specTotal != "---" && specTotal != "null") return specTotal
            val ramSpec = allSpecs["Объем"]?.toString()
            if (ramSpec != null && (ramSpec.contains("х") || ramSpec.contains("x"))) {
                val parts = ramSpec.lowercase().split(Regex("[хx]"))
                val count = parts[0].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                val size = parts.getOrNull(1)?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                if (size > 0) return "${count * size} ГБ"
            }
            val cap = if (item.ssdCapacityGb > 0) item.ssdCapacityGb else if (item.ramCapacity > 4) item.ramCapacity else 0
            if (cap > 0) "${cap} ГБ" else null
        }

        "МОДУЛЕЙ" -> {
            val ramSpec = allSpecs["Объем"]?.toString()
            if (ramSpec != null && (ramSpec.contains("х") || ramSpec.contains("x"))) {
                val count = ramSpec.lowercase().split(Regex("[хx]"))[0].replace(Regex("[^0-9]"), "")
                if (count.isNotEmpty()) return "${count} шт."
            }
            if (item.ramCapacity in 1..4) return "${item.ramCapacity} шт."
            null
        }

        "ВЫСОТА" -> {
            val h = if (item.coolerHeight > 0) item.coolerHeight else item.ramHeight
            if (h > 0) "${h} мм" else allSpecs["Высота кулера"]?.toString() ?: allSpecs["Высота"]?.toString()
        }

        "МОЩНОСТЬ" -> if (item.psuWattage > 0) "${item.psuWattage} Вт" else allSpecs["Мощность"]?.toString()
        "СЕРТИФИКАТ" -> item.psuEfficiency.takeIf { it != "---" } ?: allSpecs["Сертификат 80 PLUS"]?.toString() ?: allSpecs["Сертифицирован в стандарте"]?.toString()
        "ТИП КАБЕЛЕЙ" -> allSpecs["Отсоединяющиеся кабели"]?.toString()?.let { if (it.contains("есть", true)) "Модульный" else "Обычный" } ?: allSpecs["Тип кабелей"]?.toString()
        "ИНТЕРФЕЙС" -> item.ssdInterface.takeIf { it != "---" } ?: allSpecs["Интерфейс"]?.toString()
        "РЕСУРС (TBW)" -> allSpecs["Ресурс TBW"]?.toString() ?: allSpecs["TBW"]?.toString()
        "МАКС. ГПУ" -> if (item.maxGpuLength > 0) "${item.maxGpuLength} мм" else allSpecs["Максимальная длина видеокарты"]?.toString()
        "ФОРМ-ФАКТОР ПЛАТ" -> allSpecs["Форм-фактор совместимых материнских плат"]?.toString() ?: allSpecs["Форм-фактор материнских плат"]?.toString()
        "МАКС. КУЛЕР" -> if (item.maxCpuCoolerHeight > 0) "${item.maxCpuCoolerHeight} мм" else allSpecs["Максимальная высота кулера процессора"]?.toString()
        else -> null
    }
}

@Composable
fun ProductCard(
    item: Component,
    count: Int,
    isMulti: Boolean,
    activeStore: String = "all",
    onDetails: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val isSelected = count > 0
    var expanded by remember { mutableStateOf(false) }
    val hasRegard = item.priceRegard != "---" && item.priceRegard.isNotBlank() && item.priceRegard != "0" && !item.priceRegard.contains("Нет в наличии", true)
    val hasCitilink = item.price != "---" && item.price.isNotBlank() && item.price != "0" && !item.price.contains("Нет в наличии", true)
    val hasDns = item.priceDns != "---" && item.priceDns.isNotBlank() && item.priceDns != "0" && !item.priceDns.contains("Нет в наличии", true)
    val canCompare = (hasRegard && hasCitilink) || (hasRegard && hasDns) || (hasCitilink && hasDns)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = if (isSelected) AppTheme.CardBgElevated else AppTheme.CardBg,
        border   = BorderStroke(1.dp, if (isSelected) AppTheme.NeonGreen.copy(0.5f) else AppTheme.GlassBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.SurfaceDim).clickable { onDetails() }) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(text = item.name, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    
                    if (!expanded) {
                        // Приоритет выбранному магазину из фильтра
                        val (mainPrice, storeName, priceColor) = when (activeStore) {
                            "regard" -> Triple(item.priceRegard, "Regard", AppTheme.NeonCyan)
                            "citilink" -> Triple(item.price, "Citilink", AppTheme.NeonGreen)
                            "dns" -> Triple(item.priceDns, "DNS", AppTheme.NeonYellow)
                            else -> {
                                if (hasCitilink) Triple(item.price, "Citilink", AppTheme.NeonGreen)
                                else if (hasRegard) Triple(item.priceRegard, "Regard", AppTheme.NeonCyan)
                                else if (hasDns) Triple(item.priceDns, "DNS", AppTheme.NeonYellow)
                                else Triple("---", "", AppTheme.TextMuted)
                            }
                        }

                        Column(modifier = Modifier.clickable(enabled = canCompare) { expanded = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = mainPrice, color = priceColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (canCompare) {
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = "Show all prices",
                                        tint = AppTheme.NeonCyan.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp).size(18.dp)
                                    )
                                }
                            }
                            if (storeName.isNotEmpty()) {
                                Text(text = storeName, color = AppTheme.TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
                if (isMulti) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (count > 0) {
                            IconButton(onClick = onRemove) { 
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = AppTheme.TextMuted, modifier = Modifier.size(24.dp)) 
                            }
                            Text(
                                text = count.toString(), 
                                color = AppTheme.TextPrimary, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp, 
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        IconButton(onClick = onAdd) { 
                            Icon(Icons.Default.AddCircle, null, tint = AppTheme.NeonCyan, modifier = Modifier.size(28.dp)) 
                        }
                    }
                } else {
                    if (isSelected) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Default.CheckCircle, null, tint = AppTheme.NeonGreen, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircle, null, tint = AppTheme.NeonCyan, modifier = Modifier.size(28.dp)) }
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = AppTheme.GlassBorder.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (hasCitilink) PriceBadge("Citilink", item.price, AppTheme.NeonGreen)
                    if (hasRegard) PriceBadge("Regard", item.priceRegard, AppTheme.NeonCyan)
                    if (hasDns) PriceBadge("DNS", item.priceDns, AppTheme.NeonYellow)
                    
                    IconButton(onClick = { expanded = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ExpandLess, null, tint = AppTheme.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PriceBadge(store: String, price: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(store, color = AppTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(price, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StorePriceColumn(
    modifier: Modifier,
    store: String,
    price: String,
    url: String,
    color: Color,
    context: android.content.Context
) {
    Column(modifier/* .clickable {
        if (url.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    } */) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(store, color = AppTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            /* if (url.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.Launch, null, tint = AppTheme.TextMuted, modifier = Modifier.size(10.dp))
            } */
        }
        Text(price, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ComponentDetailDialog(
    item: Component,
    count: Int,
    isMulti: Boolean,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val isSelected = count > 0
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = AppTheme.CardBg, border = BorderStroke(1.dp, AppTheme.GlassBorderActive)) {
            Column(Modifier.padding(24.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(AppTheme.SurfaceDim)) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                Spacer(Modifier.height(16.dp)); Text(item.name, color = AppTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.price != "---" && item.price.isNotBlank() && item.price != "0" && !item.price.contains("Нет в наличии", true)) {
                        StorePriceColumn(Modifier.weight(1f), "Citilink", item.price, item.productUrl, AppTheme.NeonGreen, context)
                    }
                    if (item.priceRegard != "---" && item.priceRegard.isNotBlank() && item.priceRegard != "0" && !item.priceRegard.contains("Нет в наличии", true)) {
                        StorePriceColumn(Modifier.weight(1f), "Regard", item.priceRegard, item.productUrlRegard, AppTheme.NeonCyan, context)
                    }
                    if (item.priceDns != "---" && item.priceDns.isNotBlank() && item.priceDns != "0" && !item.priceDns.contains("Нет в наличии", true)) {
                        StorePriceColumn(Modifier.weight(1f), "DNS", item.priceDns, item.productUrlDns, AppTheme.NeonYellow, context)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = AppTheme.SurfaceDim, modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text("ХАРАКТЕРИСТИКИ", color = AppTheme.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(12.dp))
                        val specsToShow = remember(item) { 
                            val allSpecs = (item.specs ?: emptyMap()) + (item.specsCitilink ?: emptyMap()) + (item.specsRegard ?: emptyMap()) + (item.specsDns ?: emptyMap())
                            allSpecs.mapValues { it.value.toString() }.filter { it.value.isNotBlank() && it.value != "null" && it.value != "---" } 
                        }
                        val fallbackSpecs = remember(item) { listOfNotNull(
                            if (item.socket.isNotBlank() && item.socket != "---") "Сокет" to item.socket else null,
                            if (item.ramType.isNotBlank() && item.ramType != "---") "Тип памяти" to item.ramType else null,
                            if (item.ramSlots > 0) "Слоты памяти" to "${item.ramSlots} шт." else null,
                            if (item.ramMaxFreq > 0) "Макс. частота ОЗУ" to "${item.ramMaxFreq} МГц" else null,
                            if (item.formFactor.isNotBlank() && item.formFactor != "---") "Форм-фактор" to item.formFactor else null,
                            if (item.tdp > 0) "TDP" to "${item.tdp} Вт" else null,
                            if (item.gpuTdp > 0) "TDP видеокарты" to "${item.gpuTdp} Вт" else null,
                            if (item.vram > 0) "Объём VRAM" to "${item.vram} ГБ" else null,
                            if (item.gpuLength > 0) "Длина GPU" to "${item.gpuLength} мм" else null,
                            if (item.gpuChipset.isNotBlank() && item.gpuChipset != "---") "Видеочипсет" to item.gpuChipset else null,
                            if (item.psuWattage > 0) "Мощность" to "${item.psuWattage} Вт" else null,
                            if (item.psuEfficiency.isNotBlank() && item.psuEfficiency != "---") "Сертификат" to item.psuEfficiency else null,
                            if (item.coolerHeight > 0) "Высота кулера" to "${item.coolerHeight} мм" else null,
                            if (item.maxTdp > 0) "Макс. TDP" to "${item.maxTdp} Вт" else null,
                            if (item.ssdInterface.isNotBlank() && item.ssdInterface != "---") "Интерфейс SSD" to item.ssdInterface else null,
                            if (item.ssdCapacityGb > 0) "Объём SSD" to "${item.ssdCapacityGb} ГБ" else null,
                            if (item.maxGpuLength > 0) "Макс. длина GPU" to "${item.maxGpuLength} мм" else null
                        ) }
                        val rowsToShow = if (specsToShow.isNotEmpty()) specsToShow.entries.map { it.key to it.value } else if (fallbackSpecs.isNotEmpty()) fallbackSpecs else listOf("Данные" to "Характеристики не загружены")
                        rowsToShow.forEachIndexed { idx, (k, v) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = k, color = AppTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(text = v, color = AppTheme.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1.4f).padding(start = 8.dp), textAlign = TextAlign.End)
                            }
                            if (idx < rowsToShow.lastIndex) HorizontalDivider(color = AppTheme.GlassBorder.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppTheme.GlassBorderActive), contentPadding = PaddingValues(0.dp)) { Text("НАЗАД", color = AppTheme.TextSecondary, fontSize = 12.sp) }
                    
                    if (isMulti && isSelected) {
                        Surface(
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = AppTheme.CardBgElevated,
                            border = BorderStroke(1.dp, AppTheme.NeonCyan.copy(0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = onRemove) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = AppTheme.TextMuted)
                                }
                                Text(
                                    text = count.toString(),
                                    color = AppTheme.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                IconButton(onClick = onAdd) {
                                    Icon(Icons.Default.AddCircle, null, tint = AppTheme.NeonCyan)
                                }
                            }
                        }
                    } else if (!isSelected) {
                        GradientButton(
                            text = "ВЫБРАТЬ",
                            brush = AppTheme.GradientCyan, 
                            onClick = onAdd, 
                            modifier = Modifier.weight(1.3f).height(48.dp), 
                            icon = Icons.Default.AddCircle
                        )
                    } else {
                        Button(
                            onClick = {
                                onRemove()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.3f).height(48.dp), 
                            shape = RoundedCornerShape(12.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.NeonGreen.copy(0.2f)), 
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = AppTheme.NeonGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("ВЫБРАНО", color = AppTheme.NeonGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
