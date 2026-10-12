package com.example.diariotdn.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.TaskStatus
import com.example.diariotdn.ui.components.LoadingButton
import com.example.diariotdn.ui.components.RecordForm
import com.example.diariotdn.ui.components.RecordItem
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.util.DateUtils
import com.example.diariotdn.util.ReportExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DailyRecordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayIsoDate = viewModel.todayIsoDate
    val displayTodayDate = remember(todayIsoDate) { DateUtils.isoToDisplayDate(todayIsoDate) }

    val filteredTodayRecords by viewModel.filteredTodayRecords.collectAsState()
    val pendingBacklogRecords by viewModel.pendingBacklogRecords.collectAsState()
    val pendingBacklogCount by viewModel.pendingBacklogCount.collectAsState()
    val mostrarBannerAtrasado by viewModel.mostrarBannerAtrasado.collectAsState()
    val todaySearchQuery by viewModel.todaySearchQuery.collectAsState()
    val todayStatusFilter by viewModel.todayStatusFilter.collectAsState()
    val mostrarLotes by viewModel.mostrarLotes.collectAsState()

    val isSavingRecord by viewModel.isSavingRecord.collectAsState()

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

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado para mostrar ModalBottomSheet (Formulario de Creación / Edición)
    var showFormSheet by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    // Estado del BottomSheet para forzar que abra 100% expandido directamente
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados para Confirmación de Borrado, Reversión de Estado y Seguro de Completado Backlog
    var recordToDelete by remember { mutableStateOf<DailyRecord?>(null) }
    var recordToRevertStatus by remember { mutableStateOf<DailyRecord?>(null) }
    var recordToCompleteBacklog by remember { mutableStateOf<DailyRecord?>(null) }

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
                    contentDescription = "Nuevo Registro"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header del día actual
                    /*
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = "Registro del Día",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Fecha de hoy: $displayTodayDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
*/
                    // Alerta Animada de Trabajo Atrasado (Backlog)
                    item {
                        AnimatedVisibility(
                            visible = mostrarBannerAtrasado,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            var isBacklogExpanded by remember { mutableStateOf(false) }

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isBacklogExpanded = !isBacklogExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Trabajo Atrasado",
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            val bannerTitle = if (pendingBacklogCount > 0) {
                                                "⚠️ Tienes $pendingBacklogCount registro(s) pendiente(s) de días anteriores"
                                            } else {
                                                "🎉 ¡Trabajos atrasados de días anteriores completados hoy!"
                                            }

                                            Text(
                                                text = bannerTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        IconButton(onClick = { isBacklogExpanded = !isBacklogExpanded }) {
                                            Icon(
                                                imageVector = if (isBacklogExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (isBacklogExpanded) "Contraer" else "Ver pendientes",
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isBacklogExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Completa o cambia el estado de los trabajos atrasados directamente desde aquí:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                                            )

                                            pendingBacklogRecords.forEach { record ->
                                                RecordItem(
                                                    record = record,
                                                    onDelete = { rec -> recordToDelete = rec },
                                                    onStatusClick = { rec ->
                                                        val nextStatus = rec.getTaskStatus().next()
                                                        if (nextStatus == TaskStatus.COMPLETADO) {
                                                            recordToCompleteBacklog = rec
                                                        } else if (rec.getTaskStatus() == TaskStatus.COMPLETADO) {
                                                            recordToRevertStatus = rec
                                                        } else {
                                                            viewModel.cycleRecordStatus(rec)
                                                        }
                                                    },
                                                    onClick = {
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
                                }
                            }
                        }
                    }

                    // Buscador Dinámico y Filtros de Estatus
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = todaySearchQuery,
                                onValueChange = { viewModel.updateTodaySearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Buscar en registros de hoy...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (todaySearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateTodaySearchQuery("") }) {
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
                                        selected = (todayStatusFilter == null),
                                        onClick = { viewModel.updateTodayStatusFilter(null) },
                                        label = { Text("Todos") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = (todayStatusFilter == TaskStatus.COMPLETADO),
                                        onClick = { viewModel.updateTodayStatusFilter(TaskStatus.COMPLETADO) },
                                        label = { Text("🟢 Completados") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = (todayStatusFilter == TaskStatus.EN_PROCESO),
                                        onClick = { viewModel.updateTodayStatusFilter(TaskStatus.EN_PROCESO) },
                                        label = { Text("🟡 En Proceso") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = (todayStatusFilter == TaskStatus.PENDIENTE),
                                        onClick = { viewModel.updateTodayStatusFilter(TaskStatus.PENDIENTE) },
                                        label = { Text("🔴 Pendientes") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = mostrarLotes,
                                        onClick = { viewModel.toggleMostrarLotes(!mostrarLotes) },
                                        label = { Text("📦 Incluir Lotes") }
                                    )
                                }
                            }
                        }
                    }

                    // Barra de acción y título de la lista
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registros de Hoy (${filteredTodayRecords.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (filteredTodayRecords.isNotEmpty() || pendingBacklogRecords.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.copyDailyReportToClipboard(
                                            context = context,
                                            todayRecords = filteredTodayRecords,
                                            backlogRecords = pendingBacklogRecords,
                                            displayDate = displayTodayDate
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text("Copiar Texto")
                                }
                            }
                        }
                    }

                    // Lista de registros de hoy con animación de eliminación y click para editar
                    if (filteredTodayRecords.isEmpty()) {
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
                                        text = if (todaySearchQuery.isNotBlank() || todayStatusFilter != null)
                                            "No hay registros que coincidan con los filtros."
                                        else
                                            "No hay registros guardados para el día de hoy.\nToca el botón '+' para agregar el primero.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredTodayRecords,
                            key = { record -> record.id }
                        ) { record ->
                            Box(modifier = Modifier.animateItem()) {
                                RecordItem(
                                    record = record,
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
                                        // Cargar datos del registro para Modo Editar
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
    }

    // Unificación de Formulario en ModalBottomSheet (Modo Crear / Modo Editar)
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
                    // Modo Crear
                    RecordForm(
                        displayDate = displayTodayDate,
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
                                targetIsoDate = todayIsoDate,
                                onSuccess = {
                                    errorMessage = null
                                    showFormSheet = false
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        },
                        isSaving = isSavingRecord
                    )
                } else {
                    // Modo Editar
                    RecordForm(
                        displayDate = displayTodayDate,
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
                        },
                        isSaving = isSavingRecord
                    )
                }
            }
        }
    }

    // Diálogo de Confirmación antes de Eliminar
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
                Text("¿Seguro que deseas eliminar este registro? Esta acción no se puede deshacer.")
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

    // Diálogo de Confirmación para Completar Ítem Atrasado (Backlog)
    if (recordToCompleteBacklog != null) {
        AlertDialog(
            onDismissRequest = { recordToCompleteBacklog = null },
            title = {
                Text(
                    text = "Completar Ítem Atrasado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Confirmas que este ítem atrasado ya está listo y completado?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToCompleteBacklog?.let { viewModel.cycleRecordStatus(it) }
                        recordToCompleteBacklog = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToCompleteBacklog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Seguro de Estatus (Reversión)
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
