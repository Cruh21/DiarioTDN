package com.example.diariotdn.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diariotdn.data.AppDatabase
import com.example.diariotdn.data.CategoryOption
import com.example.diariotdn.data.CategoryOptionDao
import com.example.diariotdn.data.CategoryType
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.DailyRecordDao
import com.example.diariotdn.data.Lote
import com.example.diariotdn.data.LoteDao
import com.example.diariotdn.data.LoteWithRecords
import com.example.diariotdn.data.ProductoCount
import com.example.diariotdn.data.TaskStatus
import com.example.diariotdn.util.BackupManager
import com.example.diariotdn.util.DateUtils
import com.example.diariotdn.util.normalizeForSearch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailyRecordViewModel(
    private val database: AppDatabase,
    private val dao: DailyRecordDao,
    private val loteDao: LoteDao,
    private val categoryOptionDao: CategoryOptionDao
) : ViewModel() {

    init {
        // Inicializar opciones por defecto si la base de datos está vacía
        viewModelScope.launch {
            if (categoryOptionDao.getCount() == 0) {
                val defaults = listOf(
                    CategoryOption(category = CategoryType.CANTIDAD.name, value = "1"),
                    CategoryOption(category = CategoryType.CANTIDAD.name, value = "2"),
                    CategoryOption(category = CategoryType.CANTIDAD.name, value = "3"),
                    CategoryOption(category = CategoryType.CANTIDAD.name, value = "5"),
                    CategoryOption(category = CategoryType.PRODUCTO.name, value = "Brazalete"),
                    CategoryOption(category = CategoryType.PRODUCTO.name, value = "Camiseta"),
                    CategoryOption(category = CategoryType.PRODUCTO.name, value = "Gorra"),
                    CategoryOption(category = CategoryType.PRODUCTO.name, value = "Insignia"),
                    CategoryOption(category = CategoryType.RANGO.name, value = "S/1"),
                    CategoryOption(category = CategoryType.RANGO.name, value = "S/2"),
                    CategoryOption(category = CategoryType.RANGO.name, value = "S/3"),
                    CategoryOption(category = CategoryType.RANGO.name, value = "Tco/1"),
                    CategoryOption(category = CategoryType.RANGO.name, value = "Cap"),
                    CategoryOption(category = CategoryType.CODIGO.name, value = "P-130"),
                    CategoryOption(category = CategoryType.CODIGO.name, value = "P-140"),
                    CategoryOption(category = CategoryType.CODIGO.name, value = "P-150")
                )
                categoryOptionDao.insertOptions(defaults)
            }
        }
    }

    // Fecha de hoy en formato ISO (YYYY-MM-DD)
    val todayIsoDate: String = DateUtils.getTodayIsoDate()

    // 1. FUNCIÓN HELPER PRIVADA DE FECHA (ZONA HORARIA LOCAL A PRUEBA DE BALAS)
    private fun isTimestampDeHoy(timestamp: Long): Boolean {
        val hoy = java.util.Calendar.getInstance()
        val fechaItem = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return hoy.get(java.util.Calendar.YEAR) == fechaItem.get(java.util.Calendar.YEAR) &&
                hoy.get(java.util.Calendar.DAY_OF_YEAR) == fechaItem.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // Estados de Carga e Interacción Asíncrona (Loaders)
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _isSavingRecord = MutableStateFlow(false)
    val isSavingRecord: StateFlow<Boolean> = _isSavingRecord.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _isImportingDraft = MutableStateFlow(false)
    val isImportingDraft: StateFlow<Boolean> = _isImportingDraft.asStateFlow()

    // Fecha seleccionada en la pantalla de historial
    private val _selectedHistoryDate = MutableStateFlow(DateUtils.getYesterdayIsoDate())
    val selectedHistoryDate: StateFlow<String> = _selectedHistoryDate.asStateFlow()

    // Registros del día de hoy (Individuales)
    val todayRecords: StateFlow<List<DailyRecord>> = dao.getRecordsByDate(todayIsoDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Buscador General / Lista de Lotes (Insensible a tildes y mayúsculas)
    private val _loteSearchQuery = MutableStateFlow("")
    val loteSearchQuery: StateFlow<String> = _loteSearchQuery.asStateFlow()

    val allLotesWithRecords: StateFlow<List<LoteWithRecords>> = loteDao.getAllLotesWithRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeLotesCount: StateFlow<Int> = allLotesWithRecords.map { list ->
        list.count { it.totalCount == 0 || it.completedCount < it.totalCount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // 2. CÁLCULO ESTRICTO DE BACKLOG VISIBLE HOY
    val backlogVisibleHoy: StateFlow<List<DailyRecord>> = dao.getAllPastRecords(todayIsoDate)
        .map { list ->
            list.filter { rec ->
                val status = rec.getTaskStatus()
                status == TaskStatus.PENDIENTE ||
                        status == TaskStatus.EN_PROCESO ||
                        (status == TaskStatus.COMPLETADO && isTimestampDeHoy(rec.timestamp))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alias para mantener compatibilidad en las vistas
    val pendingBacklogRecords: StateFlow<List<DailyRecord>> = backlogVisibleHoy

    // BANNER: Muestra el banner mientras backlogVisibleHoy no esté vacía
    val mostrarBannerAtrasado: StateFlow<Boolean> = backlogVisibleHoy
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Conteo exacto de ítems del Backlog que AÚN permanecen pendientes/en proceso
    val pendingBacklogCount: StateFlow<Int> = backlogVisibleHoy
        .map { list -> list.count { it.getTaskStatus() != TaskStatus.COMPLETADO } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 3. UNIFICACIÓN DE PRODUCCIÓN TOTAL HOY (Resumen Ejecutivo)
    val todayCompletedProductionRecords: StateFlow<List<DailyRecord>> = combine(
        todayRecords,
        backlogVisibleHoy,
        allLotesWithRecords
    ) { todayRecs, backlogList, lotesList ->
        val completedTodayList = mutableListOf<DailyRecord>()

        // A) Registros creados hoy completados
        todayRecs.filter { it.getTaskStatus() == TaskStatus.COMPLETADO }.let {
            completedTodayList.addAll(it)
        }

        // B) Registros de Backlog completados HOY
        backlogList.filter { rec ->
            rec.getTaskStatus() == TaskStatus.COMPLETADO && isTimestampDeHoy(rec.timestamp)
        }.let {
            completedTodayList.addAll(it)
        }

        // C) Ítems de Lotes completados HOY
        lotesList.flatMap { it.records }.filter { rec ->
            rec.getTaskStatus() == TaskStatus.COMPLETADO &&
                    (rec.date == todayIsoDate || isTimestampDeHoy(rec.timestamp))
        }.let { batchCompleted ->
            val existingIds = completedTodayList.map { it.id }.toSet()
            batchCompleted.forEach { bRec ->
                if (bRec.id !in existingIds) {
                    completedTodayList.add(bRec)
                }
            }
        }

        completedTodayList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // LISTA VISUAL REPORTE: Registros individuales de hoy + Resumen agrupado de Lotes completados hoy
    val reportDisplayRecords: StateFlow<List<DailyRecord>> = combine(
        todayRecords,
        todayCompletedProductionRecords,
        allLotesWithRecords
    ) { todayRecs, completedProductionList, lotesList ->
        val reportList = mutableListOf<DailyRecord>()

        // 1. Registros individuales del día (sin lote)
        reportList.addAll(todayRecs.filter { it.loteId == null })

        // 2. Agrupación de Ítems de Lotes completados HOY por loteId
        val batchCompletedToday = completedProductionList.filter { it.loteId != null }
        val groupedByLote = batchCompletedToday.groupBy { it.loteId }
        val loteMap = lotesList.associateBy { it.lote.id }

        groupedByLote.forEach { (loteId, group) ->
            if (loteId != null && group.isNotEmpty()) {
                val loteWithRecords = loteMap[loteId]
                val loteName = loteWithRecords?.lote?.nombreLote ?: "Lote #$loteId"
                val firstItem = group.first()
                val count = group.size
                val displayDate = DateUtils.isoToDisplayDate(todayIsoDate)
                val summaryText = "$count ${firstItem.producto} del lote $loteName"

                // Verificación dinámica del estado real del Lote
                val isBatchFullyCompleted = loteWithRecords != null &&
                        loteWithRecords.totalCount > 0 &&
                        loteWithRecords.completedCount == loteWithRecords.totalCount

                val batchStatus = if (isBatchFullyCompleted) {
                    TaskStatus.COMPLETADO.name
                } else {
                    TaskStatus.EN_PROCESO.name
                }

                val virtualRecord = DailyRecord(
                    id = -loteId,
                    date = todayIsoDate,
                    formattedDate = displayDate,
                    cantidad = count.toString(),
                    producto = firstItem.producto,
                    rango = "",
                    apellidosNombres = "del lote $loteName",
                    codigoUnidad = "",
                    textoExtra = "",
                    formattedRecord = "[$displayDate] $summaryText",
                    estado = batchStatus,
                    loteId = loteId
                )
                reportList.add(virtualRecord)
            }
        }

        reportList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Consultas Reactivas Unificadas para el Dashboard / KPIs
    val todayTotalCount: StateFlow<Int> = dao.getTodayTotalCount(todayIsoDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val todayCompletedCount: StateFlow<Int> = todayCompletedProductionRecords.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Denominador (Meta Total de Hoy): Base cargada hoy + ítems extra (Backlog/Lotes) completados hoy
    val todayTotalGoalCount: StateFlow<Int> = combine(
        todayRecords,
        todayCompletedProductionRecords
    ) { todayRecs, completedList ->
        val baseTodayCount = todayRecs.size
        val todayCompletedFromBase = todayRecs.count { it.getTaskStatus() == TaskStatus.COMPLETADO }
        val completedExtraToday = (completedList.size - todayCompletedFromBase).coerceAtLeast(0)

        baseTodayCount + completedExtraToday
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Producción desglosada unificada por artículo para hoy (Completados hoy)
    val todayProductionByProduct: StateFlow<List<ProductoCount>> = todayCompletedProductionRecords.map { list ->
        list.groupBy { it.producto.trim().ifEmpty { "General" } }
            .map { (prod, records) ->
                val totalQty = records.sumOf { it.cantidad.toIntOrNull() ?: 1 }
                ProductoCount(producto = prod, total = totalQty)
            }
            .filter { it.total > 0 }
            .sortedByDescending { it.total }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Estado de Carga Inicial para retener el Splash Screen hasta que Room responda
    val isAppLoading: StateFlow<Boolean> = combine(
        todayRecords,
        backlogVisibleHoy,
        allLotesWithRecords
    ) { _, _, _ ->
        false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    // Buscador, Filtros de Estatus y Toggle para Incluir Lotes en Registros de Hoy
    private val _todaySearchQuery = MutableStateFlow("")
    val todaySearchQuery: StateFlow<String> = _todaySearchQuery.asStateFlow()

    private val _todayStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val todayStatusFilter: StateFlow<TaskStatus?> = _todayStatusFilter.asStateFlow()

    private val _mostrarLotes = MutableStateFlow(false)
    val mostrarLotes: StateFlow<Boolean> = _mostrarLotes.asStateFlow()

    val filteredTodayRecords: StateFlow<List<DailyRecord>> = combine(
        todayRecords,
        _todaySearchQuery,
        _todayStatusFilter,
        _mostrarLotes
    ) { records, query, statusFilter, showLotes ->
        val normalizedQuery = query.normalizeForSearch()
        records.filter { rec ->
            val matchesLote = showLotes || rec.loteId == null
            val matchesText = normalizedQuery.isEmpty() ||
                    rec.apellidosNombres.normalizeForSearch().contains(normalizedQuery) ||
                    rec.producto.normalizeForSearch().contains(normalizedQuery) ||
                    rec.codigoUnidad.normalizeForSearch().contains(normalizedQuery) ||
                    rec.rango.normalizeForSearch().contains(normalizedQuery) ||
                    rec.textoExtra.normalizeForSearch().contains(normalizedQuery)

            val matchesStatus = statusFilter == null || rec.getTaskStatus() == statusFilter

            matchesLote && matchesText && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateTodaySearchQuery(query: String) {
        _todaySearchQuery.value = query
    }

    fun updateTodayStatusFilter(status: TaskStatus?) {
        _todayStatusFilter.value = status
    }

    fun toggleMostrarLotes(show: Boolean) {
        _mostrarLotes.value = show
    }

    // Registros del Historial (Insensible a tildes y mayúsculas + Filtro por Estatus + Toggle Lotes)
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val historyStatusFilter: StateFlow<TaskStatus?> = _historyStatusFilter.asStateFlow()

    private val _historyMostrarLotes = MutableStateFlow(false)
    val historyMostrarLotes: StateFlow<Boolean> = _historyMostrarLotes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyRecords: StateFlow<List<DailyRecord>> = _selectedHistoryDate
        .flatMapLatest { date -> dao.getRecordsByDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredHistoryRecords: StateFlow<List<DailyRecord>> = combine(
        historyRecords,
        _historySearchQuery,
        _historyStatusFilter,
        _historyMostrarLotes
    ) { records, query, statusFilter, showLotes ->
        val normalizedQuery = query.normalizeForSearch()
        records.filter { rec ->
            val matchesLote = showLotes || rec.loteId == null
            val matchesText = normalizedQuery.isEmpty() ||
                    rec.apellidosNombres.normalizeForSearch().contains(normalizedQuery) ||
                    rec.producto.normalizeForSearch().contains(normalizedQuery) ||
                    rec.codigoUnidad.normalizeForSearch().contains(normalizedQuery) ||
                    rec.rango.normalizeForSearch().contains(normalizedQuery) ||
                    rec.textoExtra.normalizeForSearch().contains(normalizedQuery)

            val matchesStatus = statusFilter == null || rec.getTaskStatus() == statusFilter

            matchesLote && matchesText && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun updateHistoryStatusFilter(status: TaskStatus?) {
        _historyStatusFilter.value = status
    }

    fun toggleHistoryMostrarLotes(show: Boolean) {
        _historyMostrarLotes.value = show
    }

    // Modo Búsqueda Global en Toda la Base de Datos
    private val _isGlobalSearch = MutableStateFlow(false)
    val isGlobalSearch: StateFlow<Boolean> = _isGlobalSearch.asStateFlow()

    fun toggleGlobalSearch(enabled: Boolean) {
        _isGlobalSearch.value = enabled
    }

    val allRecordsGlobal: StateFlow<List<DailyRecord>> = dao.getAllRecordsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedGlobalSearchResults: StateFlow<Map<String, List<DailyRecord>>> = combine(
        allRecordsGlobal,
        _historySearchQuery,
        _historyStatusFilter,
        _historyMostrarLotes,
        _isGlobalSearch
    ) { records, query, statusFilter, showLotes, isGlobal ->
        if (!isGlobal || query.isBlank()) return@combine emptyMap()

        val normalizedQuery = query.normalizeForSearch()
        val filtered = records.filter { rec ->
            val matchesLote = showLotes || rec.loteId == null
            val matchesText = normalizedQuery.isEmpty() ||
                    rec.apellidosNombres.normalizeForSearch().contains(normalizedQuery) ||
                    rec.producto.normalizeForSearch().contains(normalizedQuery) ||
                    rec.codigoUnidad.normalizeForSearch().contains(normalizedQuery) ||
                    rec.rango.normalizeForSearch().contains(normalizedQuery) ||
                    rec.textoExtra.normalizeForSearch().contains(normalizedQuery)

            val matchesStatus = statusFilter == null || rec.getTaskStatus() == statusFilter

            matchesLote && matchesText && matchesStatus
        }

        filtered.groupBy { DateUtils.isoToDisplayDate(it.date) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Lista de todas las fechas guardadas en la BD
    val allSavedDates: StateFlow<List<String>> = dao.getAllSavedDates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredLotesWithRecords: StateFlow<List<LoteWithRecords>> = combine(
        allLotesWithRecords,
        _loteSearchQuery
    ) { lotes, query ->
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isEmpty()) {
            lotes
        } else {
            lotes.filter {
                it.lote.nombreLote.normalizeForSearch().contains(normalizedQuery) ||
                        it.lote.productoLote.normalizeForSearch().contains(normalizedQuery)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Lote Seleccionado, Buscador y Filtro por Estatus DENTRO del Lote
    private val _selectedLoteId = MutableStateFlow<Int?>(null)
    val selectedLoteId: StateFlow<Int?> = _selectedLoteId.asStateFlow()

    private val _loteItemSearchQuery = MutableStateFlow("")
    val loteItemSearchQuery: StateFlow<String> = _loteItemSearchQuery.asStateFlow()

    private val _loteItemStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val loteItemStatusFilter: StateFlow<TaskStatus?> = _loteItemStatusFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedLoteWithRecords: StateFlow<LoteWithRecords?> = _selectedLoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else loteDao.getLoteWithRecordsById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val filteredSelectedLoteWithRecords: StateFlow<LoteWithRecords?> = combine(
        selectedLoteWithRecords,
        _loteItemSearchQuery,
        _loteItemStatusFilter
    ) { loteWithRecords, query, statusFilter ->
        if (loteWithRecords == null) return@combine null
        val normalizedQuery = query.normalizeForSearch()
        val filteredRecords = loteWithRecords.records.filter { rec ->
            val matchesText = normalizedQuery.isEmpty() ||
                    rec.apellidosNombres.normalizeForSearch().contains(normalizedQuery) ||
                    rec.rango.normalizeForSearch().contains(normalizedQuery) ||
                    rec.codigoUnidad.normalizeForSearch().contains(normalizedQuery) ||
                    rec.textoExtra.normalizeForSearch().contains(normalizedQuery)

            val matchesStatus = statusFilter == null || rec.getTaskStatus() == statusFilter

            matchesText && matchesStatus
        }
        loteWithRecords.copy(records = filteredRecords)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateLoteSearchQuery(query: String) {
        _loteSearchQuery.value = query
    }

    fun updateLoteItemSearchQuery(query: String) {
        _loteItemSearchQuery.value = query
    }

    fun updateLoteItemStatusFilter(status: TaskStatus?) {
        _loteItemStatusFilter.value = status
    }

    // Opciones completas para el módulo de configuración
    val cantidadOptions: StateFlow<List<CategoryOption>> = categoryOptionDao.getOptionsByCategory(CategoryType.CANTIDAD.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productoOptions: StateFlow<List<CategoryOption>> = categoryOptionDao.getOptionsByCategory(CategoryType.PRODUCTO.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rangoOptions: StateFlow<List<CategoryOption>> = categoryOptionDao.getOptionsByCategory(CategoryType.RANGO.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codigoOptions: StateFlow<List<CategoryOption>> = categoryOptionDao.getOptionsByCategory(CategoryType.CODIGO.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Listas de texto para Dropdowns
    val cantidadValues: StateFlow<List<String>> = cantidadOptions.map { list -> list.map { it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productoValues: StateFlow<List<String>> = productoOptions.map { list -> list.map { it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rangoValues: StateFlow<List<String>> = rangoOptions.map { list -> list.map { it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codigoValues: StateFlow<List<String>> = codigoOptions.map { list -> list.map { it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado del formulario
    private val _cantidad = MutableStateFlow("1")
    val cantidad: StateFlow<String> = _cantidad.asStateFlow()

    private val _producto = MutableStateFlow("")
    val producto: StateFlow<String> = _producto.asStateFlow()

    private val _rango = MutableStateFlow("")
    val rango: StateFlow<String> = _rango.asStateFlow()

    private val _apellidosNombres = MutableStateFlow("")
    val apellidosNombres: StateFlow<String> = _apellidosNombres.asStateFlow()

    private val _codigoUnidad = MutableStateFlow("")
    val codigoUnidad: StateFlow<String> = _codigoUnidad.asStateFlow()

    private val _textoExtra = MutableStateFlow("")
    val textoExtra: StateFlow<String> = _textoExtra.asStateFlow()

    fun updateCantidad(value: String) { _cantidad.value = value }
    fun updateProducto(value: String) { _producto.value = value }
    fun updateRango(value: String) { _rango.value = value }
    fun updateApellidosNombres(value: String) { _apellidosNombres.value = value }
    fun updateCodigoUnidad(value: String) { _codigoUnidad.value = value }
    fun updateTextoExtra(value: String) { _textoExtra.value = value }

    fun setSelectedHistoryDate(isoDate: String) {
        _selectedHistoryDate.value = isoDate
        _historySearchQuery.value = ""
        _historyStatusFilter.value = null
    }

    /**
     * Cicla el estado de un registro entre PENDIENTE -> EN_PROCESO -> COMPLETADO
     * actualizando el timestamp para rastrear los completados el día de hoy.
     */
    fun cycleRecordStatus(record: DailyRecord) {
        val nextStatus = record.getTaskStatus().next()
        val updatedRecord = record.copy(
            estado = nextStatus.name,
            timestamp = System.currentTimeMillis() // Actualiza timestamp
        )
        viewModelScope.launch {
            dao.updateRecord(updatedRecord)
        }
    }

    /**
     * Guarda un nuevo registro asignado a una fecha específica o a un Lote
     */
    fun saveRecord(
        targetIsoDate: String = todayIsoDate,
        loteId: Int? = null,
        defaultProduct: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cant = _cantidad.value.trim().ifEmpty { "1" }
        val prod = _producto.value.trim().ifEmpty { defaultProduct.trim() }
        val ran = _rango.value.trim()
        val nom = _apellidosNombres.value.trim()

        // Autocompletar prefijo "P-" si el usuario ingresó solo números
        val rawCod = _codigoUnidad.value.trim().uppercase().removePrefix("P-")
        val cod = if (rawCod.isEmpty()) "" else "P-$rawCod"

        val ext = _textoExtra.value.trim().uppercase()

        if (nom.isEmpty()) {
            onError("Por favor completa el campo obligatorio: Apellidos/Nombres")
            return
        }

        val displayDate = DateUtils.isoToDisplayDate(targetIsoDate)
        val formattedStr = DailyRecord.buildFormattedRecord(
            displayDate = displayDate,
            cantidad = cant,
            producto = prod,
            rango = ran,
            apellidosNombres = nom,
            codigoUnidad = cod,
            textoExtra = ext
        )

        val newRecord = DailyRecord(
            date = targetIsoDate,
            formattedDate = displayDate,
            cantidad = cant,
            producto = prod,
            rango = ran,
            apellidosNombres = nom,
            codigoUnidad = cod,
            textoExtra = ext,
            formattedRecord = formattedStr,
            estado = TaskStatus.PENDIENTE.name,
            timestamp = System.currentTimeMillis(),
            loteId = loteId
        )

        viewModelScope.launch {
            _isSavingRecord.value = true
            try {
                dao.insertRecord(newRecord)
                clearForm()
                onSuccess()
            } finally {
                _isSavingRecord.value = false
            }
        }
    }

    /**
     * Actualiza un registro existente en Room (Update) actualizando su timestamp
     */
    fun updateExistingRecord(
        record: DailyRecord,
        cantidad: String,
        producto: String,
        rango: String,
        apellidosNombres: String,
        codigoUnidad: String,
        textoExtra: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cant = cantidad.trim().ifEmpty { "1" }
        val prod = producto.trim()
        val ran = rango.trim()
        val nom = apellidosNombres.trim()

        // Autocompletar prefijo "P-" si el usuario ingresó solo números
        val rawCod = codigoUnidad.trim().uppercase().removePrefix("P-")
        val cod = if (rawCod.isEmpty()) "" else "P-$rawCod"

        val ext = textoExtra.trim().uppercase()

        if (prod.isEmpty() || nom.isEmpty()) {
            onError("Por favor completa los campos obligatorios: Producto y Apellidos/Nombres")
            return
        }

        val displayDate = record.formattedDate
        val formattedStr = DailyRecord.buildFormattedRecord(
            displayDate = displayDate,
            cantidad = cant,
            producto = prod,
            rango = ran,
            apellidosNombres = nom,
            codigoUnidad = cod,
            textoExtra = ext
        )

        val updatedRecord = record.copy(
            cantidad = cant,
            producto = prod,
            rango = ran,
            apellidosNombres = nom,
            codigoUnidad = cod,
            textoExtra = ext,
            formattedRecord = formattedStr,
            timestamp = System.currentTimeMillis() // Actualiza timestamp
        )

        viewModelScope.launch {
            _isSavingRecord.value = true
            try {
                dao.updateRecord(updatedRecord)
                onSuccess()
            } finally {
                _isSavingRecord.value = false
            }
        }
    }

    fun deleteRecord(record: DailyRecord) {
        viewModelScope.launch {
            dao.deleteRecord(record)
        }
    }

    // Funciones del Módulo de Lotes
    fun selectLote(loteId: Int?) {
        _selectedLoteId.value = loteId
        _loteItemSearchQuery.value = ""
        _loteItemStatusFilter.value = null
    }

    fun createLote(
        nombreLote: String,
        productoLote: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val formattedName = nombreLote.trim().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { char -> char.uppercase() }
        }
        val formattedProduct = productoLote.trim().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { char -> char.uppercase() }
        }

        if (formattedName.isEmpty()) {
            onError("El nombre del lote no puede estar vacío")
            return
        }

        val newLote = Lote(
            nombreLote = formattedName,
            productoLote = formattedProduct,
            fechaCreacion = DateUtils.getTodayIsoDate(),
            estadoGeneral = TaskStatus.EN_PROCESO.name
        )

        viewModelScope.launch {
            loteDao.insertLote(newLote)
            onSuccess()
        }
    }

    fun deleteLote(lote: Lote) {
        viewModelScope.launch {
            if (_selectedLoteId.value == lote.id) {
                _selectedLoteId.value = null
            }
            // 1. Borrado en cascada manual: Elimina todos los registros hijos del lote
            dao.deleteRecordsByLoteId(lote.id)
            // 2. Elimina la entidad Lote padre
            loteDao.deleteLote(lote)
        }
    }

    // Funciones para gestionar opciones de categorías
    fun addCategoryOption(
        categoryType: CategoryType,
        value: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedValue = value.trim()
        if (trimmedValue.isEmpty()) {
            onError("El valor de la opción no puede estar vacío")
            return
        }
        viewModelScope.launch {
            categoryOptionDao.insertOption(
                CategoryOption(
                    category = categoryType.name,
                    value = trimmedValue
                )
            )
            onSuccess()
        }
    }

    fun deleteCategoryOption(option: CategoryOption) {
        viewModelScope.launch {
            categoryOptionDao.deleteOption(option)
        }
    }

    /**
     * Actualiza una opción de categoría existente en Room con las reglas de formato correspondientes
     */
    fun updateCategoryOption(
        option: CategoryOption,
        newValue: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedValue = newValue.trim()
        if (trimmedValue.isEmpty()) {
            onError("El valor de la opción no puede estar vacío")
            return
        }

        val formattedValue = when (option.category) {
            CategoryType.PRODUCTO.name -> {
                trimmedValue.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { char -> char.uppercase() }
                }
            }
            CategoryType.RANGO.name -> trimmedValue.uppercase()
            CategoryType.CODIGO.name -> {
                val clean = trimmedValue.uppercase().removePrefix("P-")
                if (clean.isEmpty()) "" else "P-$clean"
            }
            else -> trimmedValue
        }

        if (formattedValue.isEmpty()) {
            onError("El valor procesado no puede estar vacío")
            return
        }

        val updatedOption = option.copy(value = formattedValue)
        viewModelScope.launch {
            categoryOptionDao.updateOption(updatedOption)
            onSuccess()
        }
    }

    // Borrador de Importación Masiva por Texto (Draft List)
    private val _draftList = MutableStateFlow<List<com.example.diariotdn.util.DraftRecord>>(emptyList())
    val draftList: StateFlow<List<com.example.diariotdn.util.DraftRecord>> = _draftList.asStateFlow()

    fun importRawBatchText(rawText: String) {
        val parsed = com.example.diariotdn.util.BatchTextParser.procesarTextoLote(rawText)
        _draftList.value = parsed
    }

    fun removeDraftRecord(draftRecord: com.example.diariotdn.util.DraftRecord) {
        _draftList.value = _draftList.value.filter { it.idTemp != draftRecord.idTemp }
    }

    fun clearDraftList() {
        _draftList.value = emptyList()
    }

    fun confirmDraftBatchImport(
        loteId: Int,
        defaultProduct: String,
        onSuccess: (Int) -> Unit = {}
    ) {
        val itemsToImport = _draftList.value
        if (itemsToImport.isEmpty()) return

        val recordsToInsert = itemsToImport.map { draft ->
            val displayDate = DateUtils.isoToDisplayDate(todayIsoDate)
            val cleanCode = if (draft.codigoExtra.isBlank()) "" else if (draft.codigoExtra.startsWith("P-")) draft.codigoExtra else "P-${draft.codigoExtra}"
            val formattedStr = DailyRecord.buildFormattedRecord(
                displayDate = displayDate,
                cantidad = "1",
                producto = defaultProduct,
                rango = draft.rango,
                apellidosNombres = draft.nombre,
                codigoUnidad = cleanCode,
                textoExtra = ""
            )

            DailyRecord(
                date = todayIsoDate,
                formattedDate = displayDate,
                cantidad = "1",
                producto = defaultProduct,
                rango = draft.rango,
                apellidosNombres = draft.nombre,
                codigoUnidad = cleanCode,
                textoExtra = "",
                formattedRecord = formattedStr,
                estado = TaskStatus.PENDIENTE.name,
                timestamp = System.currentTimeMillis(),
                loteId = loteId
            )
        }

        viewModelScope.launch {
            _isImportingDraft.value = true
            try {
                dao.insertRecords(recordsToInsert)
                val count = recordsToInsert.size
                clearDraftList()
                onSuccess(count)
            } finally {
                _isImportingDraft.value = false
            }
        }
    }

    fun clearForm() {
        _cantidad.value = "1"
        _producto.value = ""
        _rango.value = ""
        _apellidosNombres.value = ""
        _codigoUnidad.value = ""
        _textoExtra.value = ""
    }

    // Copiado Ultra-Minimalista para un Lote
    fun copyBatchRecordsToClipboard(context: Context, loteWithRecords: LoteWithRecords) {
        if (loteWithRecords.records.isEmpty()) {
            Toast.makeText(context, "No hay ítems para copiar en este lote", Toast.LENGTH_SHORT).show()
            return
        }
        val batchTitle = loteWithRecords.lote.nombreLote.trim().uppercase()
        val productTitle = loteWithRecords.lote.productoLote.trim().uppercase()
        val header = if (productTitle.isNotEmpty()) "$batchTitle - $productTitle" else batchTitle
        val recordLines = loteWithRecords.records.joinToString("\n") { it.getContentForBatchCopy() }
        val textToCopy = "$header\n$recordLines"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lote ${loteWithRecords.lote.nombreLote}", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "Se copiaron ${loteWithRecords.records.size} ítem(s) en formato minimalista",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Copiado unificado de reporte diario que incluye registros de hoy y desglose del Backlog
     */
    fun copyDailyReportToClipboard(
        context: Context,
        todayRecords: List<DailyRecord>,
        backlogRecords: List<DailyRecord>,
        displayDate: String
    ) {
        val sb = StringBuilder()

        // Sección 1: REGISTROS DEL DÍA
        sb.append("📅 REGISTROS DEL DÍA [").append(displayDate).append("]\n")
        if (todayRecords.isEmpty()) {
            sb.append("Sin registros hoy.")
        } else {
            todayRecords.forEachIndexed { index, record ->
                sb.append(record.getContentWithStatusTag())
                if (index < todayRecords.size - 1) sb.append("\n")
            }
        }

        sb.append("\n\n")

        // Sección 2: TRABAJO ATRASADO (BACKLOG)
        sb.append("⚠️ TRABAJO ATRASADO\n")
        if (backlogRecords.isEmpty()) {
            sb.append("Todo al día.")
        } else {
            backlogRecords.forEachIndexed { index, record ->
                sb.append(record.getContentWithStatusTag())
                if (index < backlogRecords.size - 1) sb.append("\n")
            }
        }

        val textToCopy = sb.toString()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Reporte Producción $displayDate", textToCopy)
        clipboard.setPrimaryClip(clip)

        val totalCopied = todayRecords.size + backlogRecords.size
        Toast.makeText(
            context,
            "Reporte copiado al portapapeles ($totalCopied ítem/s)",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Guarda y comparte la imagen PNG capturada desde Compose GraphicsLayer
     */
    fun shareCapturedReportBitmap(
        context: Context,
        bitmap: android.graphics.Bitmap,
        displayDate: String
    ) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            try {
                com.example.diariotdn.util.ReportExporter.shareCapturedReportBitmap(context, bitmap, displayDate)
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    // Copiado estándar para fechas del historial
    fun copyRecordsToClipboard(context: Context, records: List<DailyRecord>, labelDate: String) {
        if (records.isEmpty()) {
            Toast.makeText(context, "No hay registros para copiar", Toast.LENGTH_SHORT).show()
            return
        }
        val header = "[$labelDate]"
        val recordLines = records.joinToString("\n") { it.getContentWithStatusTag() }
        val textToCopy = "$header\n$recordLines"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Registros $labelDate", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "Se copiaron ${records.size} registro(s) al portapapeles",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun copySingleRecord(context: Context, record: DailyRecord) {
        val textToCopy = "${record.getTaskStatus().emoji} ${record.formattedRecord}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Registro TDN", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Registro copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    // Importación / Exportación JSON SAF
    fun exportAllData(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val backupManager = BackupManager(context, database)
                val result = backupManager.exportAllData(uri)
                onResult(result)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportDateData(context: Context, uri: Uri, isoDate: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val backupManager = BackupManager(context, database)
                val result = backupManager.exportDateData(uri, isoDate)
                onResult(result)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val backupManager = BackupManager(context, database)
                val result = backupManager.importData(uri)
                onResult(result)
            } finally {
                _isImporting.value = false
            }
        }
    }
}

class DailyRecordViewModelFactory(
    private val database: AppDatabase,
    private val dao: DailyRecordDao,
    private val loteDao: LoteDao,
    private val categoryOptionDao: CategoryOptionDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyRecordViewModel::class.java)) {
            return DailyRecordViewModel(database, dao, loteDao, categoryOptionDao) as T
        }
        throw IllegalArgumentException("ViewModel Class Desconocida")
    }
}
