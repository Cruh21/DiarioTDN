package com.example.diariotdn.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.LoteWithRecords
import com.example.diariotdn.data.TaskStatus
import com.example.diariotdn.ui.components.LoadingButton
import com.example.diariotdn.ui.components.RecordForm
import com.example.diariotdn.ui.components.RecordItem
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotesScreen(
    viewModel: DailyRecordViewModel,
    modifier: Modifier = Modifier
) {
    val filteredLotesWithRecords by viewModel.filteredLotesWithRecords.collectAsState()
    val loteSearchQuery by viewModel.loteSearchQuery.collectAsState()
    val selectedLoteWithRecords by viewModel.selectedLoteWithRecords.collectAsState()
    val filteredSelectedLoteWithRecords by viewModel.filteredSelectedLoteWithRecords.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showBatchItemSheet by remember { mutableStateOf(false) }
    var batchRecordToEdit by remember { mutableStateOf<DailyRecord?>(null) }

    var newLoteName by remember { mutableStateOf("") }
    var newLoteProduct by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Estados para Edición de Ítem dentro de Lote
    var editRango by remember { mutableStateOf("") }
    var editApellidosNombres by remember { mutableStateOf("") }
    var editCodigoUnidad by remember { mutableStateOf("") }
    var editTextoExtra by remember { mutableStateOf("") }

    // Estado del BottomSheet para forzar que abra 100% expandido directamente
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun formatTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { char -> char.uppercase() }
        }
    }

    // Intercepta la navegación del botón físico "Atrás" cuando se está dentro de un Lote
    if (selectedLoteWithRecords != null) {
        BackHandler {
            viewModel.selectLote(null)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedLoteWithRecords == null) {
                        showCreateDialog = true
                    } else {
                        batchRecordToEdit = null
                        viewModel.clearForm()
                        dialogError = null
                        showBatchItemSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedLoteWithRecords == null) "Crear nuevo Lote" else "Agregar ítem a este Lote"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            val currentLoteWithRecords = filteredSelectedLoteWithRecords ?: selectedLoteWithRecords
            if (selectedLoteWithRecords == null || currentLoteWithRecords == null) {
                // Vista de Lista de Lotes
                LotesListView(
                    lotesWithRecords = filteredLotesWithRecords,
                    searchQuery = loteSearchQuery,
                    onSearchQueryChange = { query -> viewModel.updateLoteSearchQuery(query) },
                    onSelectLote = { loteWithRecords ->
                        viewModel.selectLote(loteWithRecords.lote.id)
                    },
                    onDeleteLote = { loteWithRecords ->
                        viewModel.deleteLote(loteWithRecords.lote)
                    }
                )
            } else {
                // Vista de Detalle del Lote (Limpia sin panel incrustado)
                LoteDetailView(
                    loteWithRecords = currentLoteWithRecords,
                    viewModel = viewModel,
                    onBack = { viewModel.selectLote(null) },
                    onEditItem = { record ->
                        batchRecordToEdit = record
                        editRango = record.rango
                        editApellidosNombres = record.apellidosNombres
                        editCodigoUnidad = record.codigoUnidad.removePrefix("P-")
                        editTextoExtra = record.textoExtra
                        dialogError = null
                        showBatchItemSheet = true
                    }
                )

                // BottomSheet Unificado para agregar / editar ítems al Lote
                if (showBatchItemSheet) {
                    ModalBottomSheet(
                        sheetState = sheetState,
                        onDismissRequest = {
                            showBatchItemSheet = false
                            batchRecordToEdit = null
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
                            val rango by viewModel.rango.collectAsState()
                            val apellidosNombres by viewModel.apellidosNombres.collectAsState()
                            val codigoUnidad by viewModel.codigoUnidad.collectAsState()
                            val textoExtra by viewModel.textoExtra.collectAsState()

                            val rangoValues by viewModel.rangoValues.collectAsState()
                            val codigoValues by viewModel.codigoValues.collectAsState()

                            if (batchRecordToEdit == null) {
                                // Modo Crear Ítem en Lote
                                RecordForm(
                                    displayDate = viewModel.todayIsoDate,
                                    cantidad = "1",
                                    producto = currentLoteWithRecords.lote.productoLote,
                                    rango = rango,
                                    apellidosNombres = apellidosNombres,
                                    codigoUnidad = codigoUnidad,
                                    textoExtra = textoExtra,
                                    rangoOptions = rangoValues,
                                    codigoOptions = codigoValues,
                                    errorMessage = dialogError,
                                    showProductField = false, // Omitir Producto
                                    showQuantityField = false, // Omitir Cantidad
                                    onCantidadChange = { viewModel.updateCantidad(it) },
                                    onProductoChange = { viewModel.updateProducto(it) },
                                    onRangoChange = {
                                        dialogError = null
                                        viewModel.updateRango(it)
                                    },
                                    onApellidosNombresChange = {
                                        dialogError = null
                                        viewModel.updateApellidosNombres(it)
                                    },
                                    onCodigoUnidadChange = {
                                        dialogError = null
                                        viewModel.updateCodigoUnidad(it)
                                    },
                                    onTextoExtraChange = { viewModel.updateTextoExtra(it) },
                                    onSaveRecord = {
                                        viewModel.saveRecord(
                                            targetIsoDate = viewModel.todayIsoDate,
                                            loteId = currentLoteWithRecords.lote.id,
                                            defaultProduct = currentLoteWithRecords.lote.productoLote,
                                            onSuccess = {
                                                dialogError = null
                                                showBatchItemSheet = false
                                            },
                                            onError = { err ->
                                                dialogError = err
                                            }
                                        )
                                    }
                                )
                            } else {
                                // Modo Editar Ítem en Lote
                                RecordForm(
                                    displayDate = currentLoteWithRecords.lote.fechaCreacion,
                                    cantidad = "1",
                                    producto = currentLoteWithRecords.lote.productoLote,
                                    rango = editRango,
                                    apellidosNombres = editApellidosNombres,
                                    codigoUnidad = editCodigoUnidad,
                                    textoExtra = editTextoExtra,
                                    rangoOptions = rangoValues,
                                    codigoOptions = codigoValues,
                                    errorMessage = dialogError,
                                    showProductField = false, // Omitir Producto
                                    showQuantityField = false, // Omitir Cantidad
                                    onCantidadChange = { },
                                    onProductoChange = { },
                                    onRangoChange = { editRango = it },
                                    onApellidosNombresChange = { editApellidosNombres = it },
                                    onCodigoUnidadChange = { editCodigoUnidad = it },
                                    onTextoExtraChange = { editTextoExtra = it },
                                    onSaveRecord = {
                                        batchRecordToEdit?.let { rec ->
                                            viewModel.updateExistingRecord(
                                                record = rec,
                                                cantidad = "1",
                                                producto = currentLoteWithRecords.lote.productoLote,
                                                rango = editRango,
                                                apellidosNombres = editApellidosNombres,
                                                codigoUnidad = editCodigoUnidad,
                                                textoExtra = editTextoExtra,
                                                onSuccess = {
                                                    batchRecordToEdit = null
                                                    dialogError = null
                                                    showBatchItemSheet = false
                                                },
                                                onError = { err ->
                                                    dialogError = err
                                                }
                                            )
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

    // Dialog para crear un nuevo Lote (Nombre y Producto) con Title Case
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newLoteName = ""
                newLoteProduct = ""
                dialogError = null
            },
            title = {
                Text(
                    text = "Crear Nuevo Lote",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Define el identificador del Lote y el producto general:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newLoteName,
                        onValueChange = { newValue ->
                            newLoteName = formatTitleCase(newValue)
                            dialogError = null
                        },
                        label = { Text("Nombre / Código del Lote *") },
                        placeholder = { Text("Ej: Lote Maurera") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        isError = dialogError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLoteProduct,
                        onValueChange = { newValue ->
                            newLoteProduct = formatTitleCase(newValue)
                        },
                        label = { Text("Producto del Lote (Ej: Brazalete)") },
                        placeholder = { Text("Ej: Brazalete / Insignia") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createLote(
                            nombreLote = newLoteName,
                            productoLote = newLoteProduct,
                            onSuccess = {
                                showCreateDialog = false
                                newLoteName = ""
                                newLoteProduct = ""
                                dialogError = null
                            },
                            onError = { err ->
                                dialogError = err
                            }
                        )
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newLoteName = ""
                        newLoteProduct = ""
                        dialogError = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun LotesListView(
    lotesWithRecords: List<LoteWithRecords>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectLote: (LoteWithRecords) -> Unit,
    onDeleteLote: (LoteWithRecords) -> Unit
) {
    // Estado de confirmación para eliminar un Lote
    var loteToDelete by remember { mutableStateOf<LoteWithRecords?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar lote...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar lote",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar"
                            )
                        }
                    }
                },
                singleLine = true
            )
        }

        item {
            Text(
                text = "Lotes de Producción (${lotesWithRecords.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (lotesWithRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .height(56.dp)
                                .width(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron lotes para \"$searchQuery\"." else "No hay lotes creados.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Intenta buscar con otro término." else "Presiona '+' para crear tu primer Lote de producción.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = lotesWithRecords,
                key = { item -> item.lote.id }
            ) { item ->
                Box(modifier = Modifier.animateItem()) {
                    LoteCard(
                        loteWithRecords = item,
                        onClick = { onSelectLote(item) },
                        onDelete = { loteToDelete = item }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Diálogo de Confirmación para Eliminar Lote Completo
    if (loteToDelete != null) {
        AlertDialog(
            onDismissRequest = { loteToDelete = null },
            title = {
                Text(
                    text = "Eliminar Lote",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Esto eliminará el lote y todos sus ítems. ¿Estás seguro?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        loteToDelete?.let { onDeleteLote(it) }
                        loteToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { loteToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun LoteCard(
    loteWithRecords: LoteWithRecords,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loteWithRecords.lote.nombreLote,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (loteWithRecords.lote.productoLote.isNotBlank()) {
                        Text(
                            text = "Producto: ${loteWithRecords.lote.productoLote}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Creado: ${loteWithRecords.lote.fechaCreacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Lote",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver Lote",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso del Lote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progreso:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = loteWithRecords.progressText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { loteWithRecords.progressRatio },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun LoteDetailView(
    loteWithRecords: LoteWithRecords,
    viewModel: DailyRecordViewModel,
    onBack: () -> Unit,
    onEditItem: (DailyRecord) -> Unit
) {
    val context = LocalContext.current

    // Estados para Programación Defensiva (Eliminar Ítem y Seguro de Estatus)
    var itemToDelete by remember { mutableStateOf<DailyRecord?>(null) }
    var recordToRevertStatus by remember { mutableStateOf<DailyRecord?>(null) }

    // Estados para Importación Masiva por Texto
    var showPasteTextDialog by remember { mutableStateOf(false) }
    var rawPastedText by remember { mutableStateOf("") }

    val itemSearchQuery by viewModel.loteItemSearchQuery.collectAsState()
    val itemStatusFilter by viewModel.loteItemStatusFilter.collectAsState()
    val draftList by viewModel.draftList.collectAsState()
    val isImportingDraft by viewModel.isImportingDraft.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado del Lote
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a la lista",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val title = if (loteWithRecords.lote.productoLote.isNotBlank()) {
                                "${loteWithRecords.lote.nombreLote} - ${loteWithRecords.lote.productoLote}"
                            } else {
                                loteWithRecords.lote.nombreLote
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Progreso: ${loteWithRecords.progressText}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { loteWithRecords.progressRatio },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Buscador Dinámico y Filtros de Estatus en Lote
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemSearchQuery,
                    onValueChange = { viewModel.updateLoteItemSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar en este lote (Nombre, Rango, Código)...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar ítem en lote",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (itemSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateLoteItemSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                // Fila de Filtros Chip por Estatus en Lote
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = (itemStatusFilter == null),
                            onClick = { viewModel.updateLoteItemStatusFilter(null) },
                            label = { Text("Todos") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = (itemStatusFilter == TaskStatus.COMPLETADO),
                            onClick = { viewModel.updateLoteItemStatusFilter(TaskStatus.COMPLETADO) },
                            label = { Text("🟢 Completados") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = (itemStatusFilter == TaskStatus.EN_PROCESO),
                            onClick = { viewModel.updateLoteItemStatusFilter(TaskStatus.EN_PROCESO) },
                            label = { Text("🟡 En Proceso") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = (itemStatusFilter == TaskStatus.PENDIENTE),
                            onClick = { viewModel.updateLoteItemStatusFilter(TaskStatus.PENDIENTE) },
                            label = { Text("🔴 Pendientes") }
                        )
                    }
                }
            }
        }

        // Barra de acción: Importar Texto Masivo y Copiar Ítems del Lote
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ítems (${loteWithRecords.records.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            rawPastedText = ""
                            showPasteTextDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Importar Texto")
                    }

                    if (loteWithRecords.records.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.copyBatchRecordsToClipboard(context, loteWithRecords)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Copiar Ítems")
                        }
                    }
                }
            }
        }

        // Lista de ítems del Lote con Indicador Circular de Color y Clickeable para Editar
        if (loteWithRecords.records.isEmpty()) {
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
                            text = if (itemSearchQuery.isNotBlank() || itemStatusFilter != null)
                                "No se encontraron ítems para los filtros seleccionados."
                            else
                                "Aún no hay ítems en este lote.\nToca '+' o 'Importar Texto' para agregar.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = loteWithRecords.records,
                key = { record -> record.id }
            ) { record ->
                Box(modifier = Modifier.animateItem()) {
                    RecordItem(
                        record = record,
                        onDelete = { rec -> itemToDelete = rec },
                        onStatusClick = { rec ->
                            if (rec.getTaskStatus() == TaskStatus.COMPLETADO) {
                                recordToRevertStatus = rec
                            } else {
                                viewModel.cycleRecordStatus(rec)
                            }
                        },
                        onClick = { onEditItem(record) },
                        showProduct = false // Omitir el texto del producto en el registro del Lote
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Diálogo 1: Pegar Texto de Lote
    if (showPasteTextDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasteTextDialog = false
                rawPastedText = ""
            },
            title = {
                Text(
                    text = "Importar Texto Masivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pega aquí el texto copiado de WhatsApp u otra fuente:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rawPastedText,
                        onValueChange = { rawPastedText = it },
                        placeholder = { Text("Ej:\n1) S/1 J. BELISARIO T. P-130\n2) PROMOCIÓN 90\n3) J. COA B P-100") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importRawBatchText(rawPastedText)
                        showPasteTextDialog = false
                        rawPastedText = ""
                    }
                ) {
                    Text("Procesar y Revisar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasteTextDialog = false
                        rawPastedText = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo 2: Revisión del Borrador Detectado
    if (draftList.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearDraftList()
            },
            title = {
                Text(
                    text = "Borrador de Importación (${draftList.size} detectados)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Revisa los datos extraídos antes de guardarlos en el lote:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = draftList,
                            key = { item -> item.idTemp }
                        ) { draft ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val mainText = listOf(draft.rango.trim(), draft.nombre.trim(), draft.codigoExtra.trim())
                                            .filter { it.isNotBlank() }
                                            .joinToString(" ")
                                        Text(
                                            text = mainText.ifBlank { "Ítem sin especificar" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (draft.rango.isNotBlank() || draft.codigoExtra.isNotBlank()) {
                                            Text(
                                                text = "Rango: ${draft.rango.ifBlank { "N/A" }} | Código: ${draft.codigoExtra.ifBlank { "N/A" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.removeDraftRecord(draft)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar de borrador",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                LoadingButton(
                    onClick = {
                        viewModel.confirmDraftBatchImport(
                            loteId = loteWithRecords.lote.id,
                            defaultProduct = loteWithRecords.lote.productoLote,
                            onSuccess = { count ->
                                Toast.makeText(context, "Se importaron $count ítem(s) con éxito al Lote", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    isLoading = isImportingDraft
                ) {
                    Text("Confirmar e Insertar (${draftList.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDraftList()
                    }
                ) {
                    Text("Descartar")
                }
            }
        )
    }

    // Diálogo de Confirmación para Eliminar Ítem de un Lote
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Eliminar Ítem del Lote",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Seguro que deseas eliminar este ítem del lote?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteRecord(it) }
                        itemToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Seguro de Estatus en Lote (Confirmación al revertir un ítem que ya está Completado)
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
