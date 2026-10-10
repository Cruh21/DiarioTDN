package com.example.diariotdn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.TaskStatus
import com.example.diariotdn.ui.components.RecordForm
import com.example.diariotdn.ui.components.RecordItem
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: DailyRecordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsState()
    val displaySelectedDate = remember(selectedHistoryDate) {
        DateUtils.isoToDisplayDate(selectedHistoryDate)
    }

    val filteredHistoryRecords by viewModel.filteredHistoryRecords.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()
    val historyStatusFilter by viewModel.historyStatusFilter.collectAsState()
    val historyMostrarLotes by viewModel.historyMostrarLotes.collectAsState()
    val allSavedDates by viewModel.allSavedDates.collectAsState()

    val cantidad by viewModel.cantidad.collectAsState()
    val producto by viewModel.producto.collectAsState()
    val rango by viewModel.rango.collectAsState()
    val apellidosNombres by viewModel.apellidosNombres.collectAsState()
    val codigoUnidad by viewModel.codigoUnidad.collectAsState()
    val textoExtra by viewModel.textoExtra.collectAsState()

    val cantidadValues by viewModel.cantidadValues.collectAsState()
    val productoValues by viewModel.productoValues.collectAsState()
    val rangoValues by viewModel.rangoValues.collectAsState()
    val codigoValues by viewModel.codigoValues.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado para ModalBottomSheet (Formulario de Creación / Edición)
    var showFormSheet by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    // Estado del BottomSheet para forzar que abra 100% expandido directamente
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados para Confirmación de Borrado y Reversión de Estado
    var recordToDelete by remember { mutableStateOf<DailyRecord?>(null) }
    var recordToRevertStatus by remember { mutableStateOf<DailyRecord?>(null) }

    // Estados para el Formulario de Edición
    var editCantidad by remember { mutableStateOf("") }
    var editProducto by remember { mutableStateOf("") }
    var editRango by remember { mutableStateOf("") }
    var editApellidosNombres by remember { mutableStateOf("") }
    var editCodigoUnidad by remember { mutableStateOf("") }
    var editTextoExtra by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    recordToEdit = null
                    viewModel.clearForm()
                    errorMessage = null
                    showFormSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar registro al historial"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Selector de Fecha
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Historial de Registros",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Fecha seleccionada: $displaySelectedDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showDatePicker = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text("Cambiar")
                                }
                            }

                            // Lista de chips con fechas que tienen registros guardados
                            if (allSavedDates.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fechas con registros:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(allSavedDates) { isoDate ->
                                        val chipDisplay = DateUtils.isoToDisplayDate(isoDate)
                                        FilterChip(
                                            selected = (isoDate == selectedHistoryDate),
                                            onClick = { viewModel.setSelectedHistoryDate(isoDate) },
                                            label = { Text(chipDisplay) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Buscador Dinámico e Insensible a Tildes + Filtros de Estatus en Historial
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = historySearchQuery,
                            onValueChange = { viewModel.updateHistorySearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar en registros del $displaySelectedDate...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar en historial",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (historySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateHistorySearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Fila de Filtros Chip por Estatus
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = (historyStatusFilter == null),
                                    onClick = { viewModel.updateHistoryStatusFilter(null) },
                                    label = { Text("Todos") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = (historyStatusFilter == TaskStatus.COMPLETADO),
                                    onClick = { viewModel.updateHistoryStatusFilter(TaskStatus.COMPLETADO) },
                                    label = { Text("🟢 Completados") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = (historyStatusFilter == TaskStatus.EN_PROCESO),
                                    onClick = { viewModel.updateHistoryStatusFilter(TaskStatus.EN_PROCESO) },
                                    label = { Text("🟡 En Proceso") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = (historyStatusFilter == TaskStatus.PENDIENTE),
                                    onClick = { viewModel.updateHistoryStatusFilter(TaskStatus.PENDIENTE) },
                                    label = { Text("🔴 Pendientes") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = historyMostrarLotes,
                                    onClick = { viewModel.toggleHistoryMostrarLotes(!historyMostrarLotes) },
                                    label = { Text("📦 Incluir Lotes") }
                                )
                            }
                        }
                    }
                }

                // Encabezado de lista de registros y Botón Copiar Registros de este Día
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registros del $displaySelectedDate (${filteredHistoryRecords.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (filteredHistoryRecords.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.copyRecordsToClipboard(
                                        context = context,
                                        records = filteredHistoryRecords,
                                        labelDate = displaySelectedDate
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text("Copiar Registros")
                            }
                        }
                    }
                }

                // Lista de registros correspondientes con clickeable para Editar
                if (filteredHistoryRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (historySearchQuery.isNotBlank() || historyStatusFilter != null)
                                        "No hay registros que coincidan con los filtros."
                                    else
                                        "No hay registros guardados para el $displaySelectedDate.\nToca el botón '+' para agregar uno.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredHistoryRecords,
                        key = { record -> record.id }
                    ) { record ->
                        Box(modifier = Modifier.animateItem()) {
                            RecordItem(
                                record = record,
                                onCopySingle = { rec ->
                                    viewModel.copySingleRecord(context, rec)
                                },
                                onDelete = { rec ->
                                    recordToDelete = rec
                                },
                                onStatusClick = { rec ->
                                    if (rec.getTaskStatus() == TaskStatus.COMPLETADO) {
                                        recordToRevertStatus = rec
                                    } else {
                                        viewModel.cycleRecordStatus(rec)
                                    }
                                },
                                onClick = {
                                    // Cargar datos del registro para Modo Editar en Historial
                                    recordToEdit = record
                                    editCantidad = record.cantidad
                                    editProducto = record.producto
                                    editRango = record.rango
                                    editApellidosNombres = record.apellidosNombres
                                    editCodigoUnidad = record.codigoUnidad.removePrefix("P-")
                                    editTextoExtra = record.textoExtra
                                    errorMessage = null
                                    showFormSheet = true
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Unificación de Formulario en ModalBottomSheet (Modo Crear / Modo Editar Historial)
    if (showFormSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showFormSheet = false
                recordToEdit = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                if (recordToEdit == null) {
                    // Modo Crear Registro en la fecha del Historial
                    RecordForm(
                        displayDate = displaySelectedDate,
                        cantidad = cantidad,
                        producto = producto,
                        rango = rango,
                        apellidosNombres = apellidosNombres,
                        codigoUnidad = codigoUnidad,
                        textoExtra = textoExtra,
                        cantidadOptions = cantidadValues,
                        productoOptions = productoValues,
                        rangoOptions = rangoValues,
                        codigoOptions = codigoValues,
                        errorMessage = errorMessage,
                        onCantidadChange = {
                            errorMessage = null
                            viewModel.updateCantidad(it)
                        },
                        onProductoChange = {
                            errorMessage = null
                            viewModel.updateProducto(it)
                        },
                        onRangoChange = {
                            errorMessage = null
                            viewModel.updateRango(it)
                        },
                        onApellidosNombresChange = {
                            errorMessage = null
                            viewModel.updateApellidosNombres(it)
                        },
                        onCodigoUnidadChange = {
                            errorMessage = null
                            viewModel.updateCodigoUnidad(it)
                        },
                        onTextoExtraChange = {
                            viewModel.updateTextoExtra(it)
                        },
                        onSaveRecord = {
                            viewModel.saveRecord(
                                targetIsoDate = selectedHistoryDate,
                                onSuccess = {
                                    errorMessage = null
                                    showFormSheet = false
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        }
                    )
                } else {
                    // Modo Editar Registro en la fecha del Historial
                    RecordForm(
                        displayDate = displaySelectedDate,
                        cantidad = editCantidad,
                        producto = editProducto,
                        rango = editRango,
                        apellidosNombres = editApellidosNombres,
                        codigoUnidad = editCodigoUnidad,
                        textoExtra = editTextoExtra,
                        cantidadOptions = cantidadValues,
                        productoOptions = productoValues,
                        rangoOptions = rangoValues,
                        codigoOptions = codigoValues,
                        errorMessage = errorMessage,
                        onCantidadChange = { editCantidad = it },
                        onProductoChange = { editProducto = it },
                        onRangoChange = { editRango = it },
                        onApellidosNombresChange = { editApellidosNombres = it },
                        onCodigoUnidadChange = { editCodigoUnidad = it },
                        onTextoExtraChange = { editTextoExtra = it },
                        onSaveRecord = {
                            recordToEdit?.let { rec ->
                                viewModel.updateExistingRecord(
                                    record = rec,
                                    cantidad = editCantidad,
                                    producto = editProducto,
                                    rango = editRango,
                                    apellidosNombres = editApellidosNombres,
                                    codigoUnidad = editCodigoUnidad,
                                    textoExtra = editTextoExtra,
                                    onSuccess = {
                                        recordToEdit = null
                                        errorMessage = null
                                        showFormSheet = false
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val newIsoDate = DateUtils.millisToIsoDate(selectedMillis)
                            viewModel.setSelectedHistoryDate(newIsoDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Diálogo de Confirmación para Eliminar Registro del Historial
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    text = "Eliminar Registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Seguro que deseas eliminar este registro del historial? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteRecord(it) }
                        recordToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Seguro de Estatus
    if (recordToRevertStatus != null) {
        AlertDialog(
            onDismissRequest = { recordToRevertStatus = null },
            title = {
                Text(
                    text = "Cambiar Estado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Este registro ya está completado. ¿Seguro que deseas pasarlo a pendiente/en proceso?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToRevertStatus?.let { viewModel.cycleRecordStatus(it) }
                        recordToRevertStatus = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToRevertStatus = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
