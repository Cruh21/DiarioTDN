package com.example.diariotdn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diariotdn.data.DailyRecordDao
import com.example.diariotdn.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportesViewModel(
    private val dao: DailyRecordDao
) : ViewModel() {

    // Fecha seleccionada para el reporte diario (por defecto hoy)
    private val _selectedReportDate = MutableStateFlow(DateUtils.getTodayIsoDate())
    val selectedReportDate: StateFlow<String> = _selectedReportDate.asStateFlow()

    // Métricas del Reporte Semanal
    private val _totalSemanal = MutableStateFlow(0)
    val totalSemanal: StateFlow<Int> = _totalSemanal.asStateFlow()

    // Desglose completo de productos fabricados en la semana (ordenados de mayor a menor)
    private val _desgloseProductos = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val desgloseProductos: StateFlow<List<Pair<String, Int>>> = _desgloseProductos.asStateFlow()

    // Alias para mantener compatibilidad en toda la app
    val top3Productos: StateFlow<List<Pair<String, Int>>> = _desgloseProductos

    private val _mejorDia = MutableStateFlow("Sin datos")
    val mejorDia: StateFlow<String> = _mejorDia.asStateFlow()

    private val _isWeeklyLoading = MutableStateFlow(false)
    val isWeeklyLoading: StateFlow<Boolean> = _isWeeklyLoading.asStateFlow()

    init {
        val (startIso, endIso) = DateUtils.getCurrentWeekRangeIso()
        calcularMetricasSemanales(startIso, endIso)
    }

    fun setSelectedReportDate(isoDate: String) {
        _selectedReportDate.value = isoDate
    }

    fun calcularMetricasSemanales(fechaInicio: String, fechaFin: String) {
        viewModelScope.launch {
            _isWeeklyLoading.value = true
            try {
                val records = dao.obtenerRegistrosPorRango(fechaInicio, fechaFin)

                val totalQty = records.sumOf { it.cantidad.toIntOrNull() ?: 1 }
                _totalSemanal.value = totalQty

                val allProductsBreakdown = records
                    .groupBy { it.producto.trim().ifEmpty { "General" } }
                    .mapValues { entry -> entry.value.sumOf { it.cantidad.toIntOrNull() ?: 1 } }
                    .toList()
                    .sortedByDescending { it.second }
                _desgloseProductos.value = allProductsBreakdown

                val dayGrouped = records.groupBy { it.date }
                if (dayGrouped.isEmpty()) {
                    _mejorDia.value = "Sin datos en el rango"
                } else {
                    val bestDayEntry = dayGrouped
                        .mapValues { entry -> entry.value.sumOf { it.cantidad.toIntOrNull() ?: 1 } }
                        .maxByOrNull { it.value }

                    if (bestDayEntry != null) {
                        val formattedDayName = formatDayOfWeekName(bestDayEntry.key)
                        _mejorDia.value = "$formattedDayName (${bestDayEntry.value} ítems)"
                    } else {
                        _mejorDia.value = "Sin datos"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _totalSemanal.value = 0
                _desgloseProductos.value = emptyList()
                _mejorDia.value = "Error al calcular"
            } finally {
                _isWeeklyLoading.value = false
            }
        }
    }

    private fun formatDayOfWeekName(isoDate: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInput.parse(isoDate)
            if (date != null) {
                val dayName = SimpleDateFormat("EEEE", Locale("es", "ES")).format(date)
                val displayDate = DateUtils.isoToDisplayDate(isoDate)
                "${dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ($displayDate)"
            } else {
                DateUtils.isoToDisplayDate(isoDate)
            }
        } catch (e: Exception) {
            DateUtils.isoToDisplayDate(isoDate)
        }
    }
}

class ReportesViewModelFactory(
    private val dao: DailyRecordDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportesViewModel::class.java)) {
            return ReportesViewModel(dao) as T
        }
        throw IllegalArgumentException("ViewModel Class Desconocida")
    }
}
