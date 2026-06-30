/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.sborkapc.data.AppDatabase
import com.example.sborkapc.data.SavedBuild
import com.example.sborkapc.data.SavedComponent
import com.example.sborkapc.network.Component
import com.example.sborkapc.network.RetrofitClient
import com.example.sborkapc.ui.components.assemblyOrder
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "BuildViewModel"

data class CategoryFilterState(
    val searchQuery: String = "",
    val selectedFilters: Map<String, Set<String>> = emptyMap(),
    val minPrice: String = "",
    val maxPrice: String = "",
    val sortOrder: Int = 0,
    val filtersExpanded: Boolean = false,
    val storeFilter: String = "all"
)

data class BuildUiState(
    val savedComponents: List<SavedComponent> = emptyList(),
    val componentsList: List<Component> = emptyList(),
    val savedBuilds: List<SavedBuild> = emptyList(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editingBuildId: Int? = null,
    val currentCategory: String = "",
    val compatibilityErrors: List<String> = emptyList(),
    val compatibilityAdvice: List<String> = emptyList(),
    val totalPrice: Int = 0,
    val totalCitilink: Int = 0,
    val totalRegard: Int = 0,
    val totalDns: Int = 0,
    val buildProgress: Float = 0f,
    val isRefreshing: Boolean = false,
    val categoryFilters: Map<String, CategoryFilterState> = emptyMap()
)

class BuildViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application, AppDatabase::class.java, "pc-build-db"
    ).fallbackToDestructiveMigration().build()

    private val dao      = db.componentDao()
    private val buildDao = db.buildDao()
    private val gson     = Gson()

    private var validationJob: Job? = null
    private var fetchJob: Job? = null

    var uiState by mutableStateOf(BuildUiState())
        private set

    init {
        viewModelScope.launch {
            dao.getAllSelected().collect { saved ->
                val hybrid = saved.sumOf { 
                    when (it.selectedStore) {
                        "regard" -> parsePrice(it.priceRegard)
                        "dns"    -> parsePrice(it.priceDns)
                        else     -> {
                            val p = parsePrice(it.price)
                            if (p > 0) p else {
                                val pr = parsePrice(it.priceRegard)
                                if (pr > 0) pr else parsePrice(it.priceDns)
                            }
                        }
                    }
                }
                val citilink = saved.sumOf { parsePrice(it.price) }
                val regard = saved.sumOf { parsePrice(it.priceRegard) }
                val dns = saved.sumOf { parsePrice(it.priceDns) }

                uiState = uiState.copy(
                    savedComponents = saved,
                    totalPrice      = hybrid,
                    totalCitilink   = citilink,
                    totalRegard     = regard,
                    totalDns        = dns,
                    buildProgress   = calculateProgress(saved)
                )
                validateBuildRemotely(saved)
            }
        }
        viewModelScope.launch {
            buildDao.getAllBuilds().collect { builds ->
                uiState = uiState.copy(savedBuilds = builds)
            }
        }
    }


    private fun calculateProgress(saved: List<SavedComponent>): Float {
        val uniqueCategories = saved.map { it.category }.distinct().size
        return uniqueCategories.toFloat() / assemblyOrder.size
    }


    private fun validateBuildRemotely(saved: List<SavedComponent>) {
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(500)
            try {
                // Одиночные компоненты
                val cpuId    = saved.find { it.category == "Процессоры" }?.remoteId
                val mbId     = saved.find { it.category == "Материнские платы" }?.remoteId
                val psuId    = saved.find { it.category == "Блоки питания" }?.remoteId
                val caseId   = saved.find { it.category == "Корпуса" }?.remoteId
                val coolerId = saved.find { it.category == "Кулеры" }?.remoteId

                val gpuIds = saved.filter { it.category == "Видеокарты" }
                    .map { it.remoteId }
                val ramIds = saved.filter { it.category == "Оперативная память" }
                    .map { it.remoteId }
                val ssdIds = saved.filter { it.category == "SSD" }
                    .map { it.remoteId }

                Log.d(TAG, "Валидация: cpu=$cpuId mb=$mbId " +
                        "gpu=$gpuIds(${gpuIds.size}) " +
                        "ram=$ramIds(${ramIds.size}) " +
                        "ssd=$ssdIds(${ssdIds.size})")

                val response = RetrofitClient.api.checkCompatibility(
                    cpuId    = cpuId,
                    mbId     = mbId,
                    gpuIds   = gpuIds.ifEmpty { null },
                    ramIds   = ramIds.ifEmpty { null },
                    psuId    = psuId,
                    caseId   = caseId,
                    coolerId = coolerId,
                    ssdIds   = ssdIds.ifEmpty { null }
                )

                Log.d(TAG, "Результат: ${response.status} " +
                        "critical=${response.critical.size} " +
                        "warning=${response.warning.size} " +
                        "advisory=${response.advisory.size}")

                uiState = uiState.copy(
                    compatibilityErrors = response.critical.map { it.title },
                    compatibilityAdvice = (response.warning + response.advisory).map { it.title }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удалённой валидации: ${e.message}")
                uiState = uiState.copy(
                    compatibilityErrors = computeLocalErrors(saved),
                    compatibilityAdvice = computeLocalAdvice(saved)
                )
            }
        }
    }


    fun fetchComponents(category: String, force: Boolean = false) {
        if (!force && uiState.currentCategory == category &&
            uiState.componentsList.isNotEmpty()) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoading       = !force,
                isRefreshing    = force,
                currentCategory = category,
                componentsList  = if (!force && uiState.currentCategory != category)
                    emptyList() else uiState.componentsList
            )
            try {
                val response = RetrofitClient.api.getComponents(category)
                uiState = uiState.copy(componentsList = response.components)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки компонентов: ${e.message}")
                uiState = uiState.copy(componentsList = emptyList())
            } finally {
                uiState = uiState.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refreshActiveBuild() {
        if (uiState.savedComponents.isEmpty()) return
        
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            try {
                val categories = uiState.savedComponents.map { it.category }.distinct()

                val deferreds = categories.map { cat ->
                    async { cat to RetrofitClient.api.getComponents(cat) }
                }
                val freshComponentsMap = deferreds.awaitAll().toMap().mapValues { it.value.components }

                uiState.savedComponents.forEach { saved ->
                    val fresh = freshComponentsMap[saved.category]?.find { it.id == saved.remoteId }
                    if (fresh != null) {
                        val updated = saved.copy(
                            price = fresh.price,
                            priceRegard = fresh.priceRegard,
                            priceDns = fresh.priceDns,
                            productUrl = fresh.productUrl,
                            productUrlRegard = fresh.productUrlRegard,
                            productUrlDns = fresh.productUrlDns
                        )
                        dao.saveComponent(updated)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления цен: ${e.message}")
            } finally {
                uiState = uiState.copy(isRefreshing = false)
            }
        }
    }

    fun refreshHistoryBuilds() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            try {
                val builds = uiState.savedBuilds
                if (builds.isEmpty()) return@launch

                val idToCategory = mutableMapOf<Long, String>()
                builds.forEach { b ->
                    val comps: List<SavedComponent> = gson.fromJson(
                        b.componentsJson,
                        object : com.google.gson.reflect.TypeToken<List<SavedComponent>>() {}.type
                    )
                    comps.forEach { idToCategory[it.remoteId] = it.category }
                }

                val categories = idToCategory.values.distinct()

                val deferreds = categories.map { cat ->
                    async { RetrofitClient.api.getComponents(cat).components }
                }
                val freshData = deferreds.awaitAll().flatten().associateBy { it.id }

                builds.forEach { build ->
                    val comps: List<SavedComponent> = gson.fromJson(
                        build.componentsJson,
                        object : com.google.gson.reflect.TypeToken<List<SavedComponent>>() {}.type
                    )
                    
                    var changed = false
                    val updatedComps = comps.map { sc ->
                        val f = freshData[sc.remoteId]
                        if (f != null) {
                            if (sc.price != f.price || sc.priceRegard != f.priceRegard || sc.priceDns != f.priceDns) {
                                changed = true
                            }
                            sc.copy(
                                price = f.price,
                                priceRegard = f.priceRegard,
                                priceDns = f.priceDns,
                                productUrl = f.productUrl,
                                productUrlRegard = f.productUrlRegard,
                                productUrlDns = f.productUrlDns
                            )
                        } else sc
                    }

                    if (changed) {
                        val newTotal = updatedComps.sumOf { 
                            when (it.selectedStore) {
                                "regard" -> parsePrice(it.priceRegard)
                                "dns"    -> parsePrice(it.priceDns)
                                else     -> parsePrice(it.price)
                            }
                        }
                        buildDao.saveBuild(build.copy(
                            componentsJson = gson.toJson(updatedComps),
                            totalPrice = newTotal
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления истории: ${e.message}")
            } finally {
                uiState = uiState.copy(isRefreshing = false)
            }
        }
    }

    fun updateCategoryFilter(category: String, update: (CategoryFilterState) -> CategoryFilterState) {
        val currentFilters = uiState.categoryFilters
        val stateForCategory = currentFilters[category] ?: CategoryFilterState()
        val newState = update(stateForCategory)
        uiState = uiState.copy(
            categoryFilters = currentFilters + (category to newState)
        )
    }

    fun addComponent(c: Component) {
        viewModelScope.launch {
            val singleOnly = listOf(
                "Процессоры", "Материнские платы",
                "Корпуса", "Блоки питания", "Кулеры"
            )
            if (c.category in singleOnly) {
                dao.deleteComponentsByCategory(c.category)
            }

            val specsString = c.specs
                ?.map { "${it.key}: ${it.value}" }
                ?.joinToString("|")
                ?: c.socket

            val initialStore = if (c.price != "---" && c.price != "0") "citilink"
                               else if (c.priceRegard != "---" && c.priceRegard != "0") "regard"
                               else if (c.priceDns != "---" && c.priceDns != "0") "dns"
                               else "citilink"

            dao.saveComponent(
                SavedComponent(
                    category   = c.category,
                    remoteId   = c.id,
                    name       = c.name,
                    socketData = specsString,
                    imageUrl   = c.imageUrl,
                    price      = c.price,
                    priceRegard = c.priceRegard,
                    priceDns    = c.priceDns,
                    productUrl  = c.productUrl,
                    productUrlRegard = c.productUrlRegard,
                    productUrlDns = c.productUrlDns,
                    selectedStore = initialStore
                )
            )
        }
    }

    fun removeComponentById(id: Int) {
        viewModelScope.launch { dao.deleteComponentById(id) }
    }

    fun removeComponent(category: String) {
        viewModelScope.launch { dao.deleteComponentsByCategory(category) }
    }

    fun updateComponentStore(id: Int, store: String) {
        viewModelScope.launch {
            val component = uiState.savedComponents.find { it.id == id } ?: return@launch
            dao.saveComponent(component.copy(selectedStore = store))
        }
    }

    fun optimizeBuildPrice() {
        viewModelScope.launch {
            uiState.savedComponents.forEach { comp ->
                val pCl = parsePrice(comp.price).let { if (it == 0) Int.MAX_VALUE else it }
                val pRg = parsePrice(comp.priceRegard).let { if (it == 0) Int.MAX_VALUE else it }
                val pDn = parsePrice(comp.priceDns).let { if (it == 0) Int.MAX_VALUE else it }
                
                val minPrice = minOf(pCl, pRg, pDn)
                val cheapestStore = when (minPrice) {
                    pDn -> "dns"
                    pRg -> "regard"
                    else -> "citilink"
                }
                
                if (comp.selectedStore != cheapestStore) {
                    dao.saveComponent(comp.copy(selectedStore = cheapestStore))
                }
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch { dao.clearBuild() }
    }

    fun saveCurrentBuild() {
        if (uiState.savedComponents.isEmpty()) return
        viewModelScope.launch {
            val build = SavedBuild(
                id             = uiState.editingBuildId ?: 0,
                timestamp      = System.currentTimeMillis(),
                totalPrice     = uiState.totalPrice,
                componentsJson = gson.toJson(uiState.savedComponents)
            )
            buildDao.saveBuild(build)
            dao.clearBuild()
            uiState = uiState.copy(isEditing = false, editingBuildId = null)
        }
    }

    fun deleteSavedBuild(id: Int) {
        viewModelScope.launch { buildDao.deleteBuild(id) }
    }

    fun loadSavedBuild(build: SavedBuild) {
        viewModelScope.launch {
            val components: List<SavedComponent> = gson.fromJson(
                build.componentsJson,
                object : com.google.gson.reflect.TypeToken<List<SavedComponent>>() {}.type
            )
            dao.clearBuild()
            components.forEach { dao.saveComponent(it.copy(id = 0)) }
            uiState = uiState.copy(isEditing = true, editingBuildId = build.id)
        }
    }

    fun cancelEditing() {
        viewModelScope.launch {
            dao.clearBuild()
            uiState = uiState.copy(isEditing = false, editingBuildId = null)
        }
    }


    private fun parsePrice(priceStr: String) =
        priceStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

    private fun extractInt(item: SavedComponent?, keyPart: String): Int {
        if (item == null) return 0
        val line = item.socketData.split("|")
            .find { it.contains(keyPart, ignoreCase = true) } ?: return 0
        val valuePart = line.substringAfter(":")
        return Regex("(\\d+)").find(valuePart)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun findValue(item: SavedComponent, keys: List<String>): Int {
        keys.forEach { key ->
            val v = extractInt(item, key)
            if (v > 0) return v
        }
        return 0
    }

    private fun getParam(item: SavedComponent?, keyName: String): String {
        if (item == null) return ""
        val line = item.socketData.split("|")
            .find { it.contains(keyName, ignoreCase = true) }
        return line?.substringAfter(":")?.trim() ?: ""
    }

    private fun normalizeSocket(socket: String): String =
        socket.lowercase()
            .replace("socket", "")
            .replace("lga", "")
            .replace(" ", "")
            .trim()

    private fun computeLocalErrors(saved: List<SavedComponent>): List<String> {
        val errors = mutableListOf<String>()

        val cpu   = saved.find { it.category == "Процессоры" }
        val mobo  = saved.find { it.category == "Материнские платы" }
        val gpus  = saved.filter { it.category == "Видеокарты" }
        val rams  = saved.filter { it.category == "Оперативная память" }
        val ssds  = saved.filter { it.category == "SSD" }
        val case_ = saved.find { it.category == "Корпуса" }

        if (cpu != null && mobo != null) {
            val cpuSocket  = getParam(cpu,  "Сокет")
            val moboSocket = getParam(mobo, "Сокет")
            if (cpuSocket.isNotEmpty() && moboSocket.isNotEmpty() &&
                normalizeSocket(cpuSocket) != normalizeSocket(moboSocket)
            ) {
                errors.add("Конфликт сокетов: CPU $cpuSocket ≠ MB $moboSocket")
            }
        }

        if (mobo != null && rams.isNotEmpty()) {
            var ramSlots = findValue(mobo, listOf(
                "Количество слотов памяти", "Слотов памяти", "ramSlots"
            ))
            if (ramSlots == 0) {
                val ff = getParam(mobo, "Форм-фактор").lowercase()
                ramSlots = when {
                    "mini-itx" in ff -> 2
                    "e-atx"    in ff -> 8
                    else             -> 4
                }
            }
            if (rams.size > ramSlots) {
                errors.add(
                    "Не хватает слотов ОЗУ: на плате $ramSlots, " +
                            "выбрано ${rams.size} планок"
                )
            }
        }

        if (mobo != null && ssds.isNotEmpty()) {
            val nvmeSsds = ssds.filter {
                it.socketData.contains("NVMe", ignoreCase = true) ||
                        it.socketData.contains("M.2",  ignoreCase = true)
            }
            if (nvmeSsds.isNotEmpty()) {
                var m2Slots = findValue(mobo, listOf(
                    "Разъемов M.2", "Количество разъемов M.2", "m2Slots"
                ))
                if (m2Slots == 0) {
                    val chipset = getParam(mobo, "Чипсет").uppercase()
                    m2Slots = when {
                        chipset.contains("X670") || chipset.contains("X570") ||
                                chipset.contains("Z790") || chipset.contains("Z890") -> 3
                        chipset.contains("B650") || chipset.contains("B550") ||
                                chipset.contains("Z690") || chipset.contains("B760") -> 2
                        else -> 1
                    }
                }
                if (nvmeSsds.size > m2Slots) {
                    errors.add(
                        "Недостаточно разъемов M.2: на плате $m2Slots, " +
                                "выбрано ${nvmeSsds.size} NVMe SSD"
                    )
                }
            }
        }

        if (mobo != null && gpus.size > 1) {
            var pcieSlots = findValue(mobo, listOf(
                "Всего слотов PCI-E x16", "Слотов PCI-E x16", "pcieX16Slots"
            ))
            if (pcieSlots == 0) pcieSlots = 1
            if (gpus.size > pcieSlots) {
                errors.add(
                    "Недостаточно слотов PCIe x16: на плате $pcieSlots, " +
                            "выбрано ${gpus.size} GPU"
                )
            }
        }

        if (case_ != null) {
            val caseMaxLen = findValue(case_, listOf(
                "Максимальная длина видеокарты", "maxGpuLength"
            ))
            if (caseMaxLen > 0) {
                gpus.forEach { gpu ->
                    val gpuLen = findValue(gpu, listOf(
                        "Длина видеокарты", "Длина", "gpuLength"
                    ))
                    if (gpuLen > 0 && gpuLen > caseMaxLen) {
                        errors.add(
                            "GPU '${gpu.name.take(25)}' " +
                                    "(${gpuLen}мм) не влезет в корпус (макс. ${caseMaxLen}мм)"
                        )
                    }
                }
            }
        }

        return errors
    }

    private fun computeLocalAdvice(saved: List<SavedComponent>): List<String> {
        val advice = mutableListOf<String>()

        val psu  = saved.find { it.category == "Блоки питания" }
        val rams = saved.filter { it.category == "Оперативная память" }
        val mobo = saved.find { it.category == "Материнские платы" }

        if (psu != null && !psu.socketData.contains("Gold", ignoreCase = true))
            advice.add("Рекомендуем БП с сертификатом 80+ Gold и выше")

        if (rams.size == 1) {
            val mbSlots = if (mobo != null)
                extractInt(mobo, "Количество слотов памяти").takeIf { it > 0 } ?: 2
            else 2
            if (mbSlots >= 2)
                advice.add("Добавьте второй модуль ОЗУ для двухканального режима (+10–30% производительности)")
        }

        return advice
    }
}