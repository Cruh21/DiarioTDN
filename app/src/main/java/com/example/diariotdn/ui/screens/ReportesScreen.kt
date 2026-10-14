package com.example.diariotdn.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.diariotdn.ui.components.LoadingButton
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.ui.viewmodel.ReportesViewModel
import com.example.diariotdn.util.ArchivoReporte
import com.example.diariotdn.util.DateUtils
import com.example.diariotdn.util.FileManager
import com.example.diariotdn.util.ReportExporter
import com.example.diariotdn.util.TipoReporte
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    dailyViewModel: DailyRecordViewModel,
    reportesViewModel: ReportesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Diario, 1: Semanal
    val tabs = listOf("Reporte Diario", "Reporte Semanal")

    // Estados para Reporte Diario (Hoy por defecto)
    val todayIsoDate = dailyViewModel.todayIsoDate
    val displayTodayDate = remember(todayIsoDate) { DateUtils.isoToDisplayDate(todayIsoDate) }

    val reportDisplayRecords by dailyViewModel.reportDisplayRecords.collectAsState()
    val pendingBacklogRecords by dailyViewModel.pendingBacklogRecords.collectAsState()
    val todayCompletedProductionRecords by dailyViewModel.todayCompletedProductionRecords.collectAsState()
    val isGeneratingReport by dailyViewModel.isGeneratingReport.collectAsState()

    // Estados para Reporte Semanal
    val totalSemanal by reportesViewModel.totalSemanal.collectAsState()
    val top3Productos by reportesViewModel.top3Productos.collectAsState()
    val desgloseProductos by reportesViewModel.desgloseProductos.collectAsState()
    val mejorDia by reportesViewModel.mejorDia.collectAsState()

    val (currentWeekStartIso, currentWeekEndIso) = remember { DateUtils.getCurrentWeekRangeIso() }
    val weekRangeDisplay = remember(currentWeekStartIso, currentWeekEndIso) {
        "Del ${DateUtils.isoToDisplayDate(currentWeekStartIso)} al ${DateUtils.isoToDisplayDate(currentWeekEndIso)}"
    }

    // Estados para Búsqueda y Gestión de Archivos Guardados en Disco
    var searchQuery by remember { mutableStateOf("") }
    var savedFilesList by remember { mutableStateOf<List<ArchivoReporte>>(emptyList()) }
    var reporteABorrar by remember { mutableStateOf<ArchivoReporte?>(null) }

    // Estados independientes de Carga para Botones de Acción
    var isSavingAndOpening by remember { mutableStateOf(false) }
    var isSharingDirectly by remember { mutableStateOf(false) }

    var baseFolderUri by remember { mutableStateOf(FileManager.obtenerUriDirectorioBase(context)) }

    fun refreshSavedFiles() {
        val currentType = if (selectedTabIndex == 0) TipoReporte.DIARIO else TipoReporte.SEMANAL
        savedFilesList = FileManager.obtenerReportesGuardados(context, currentType)
    }

    val launcherSAF = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
                FileManager.guardarUriDirectorioBase(context, it.toString())
                baseFolderUri = it.toString()
                Toast.makeText(context, "Carpeta 'Diario TDN' vinculada con éxito", Toast.LENGTH_SHORT).show()
                refreshSavedFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al vincular carpeta: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshSavedFiles()
    }

    LaunchedEffect(selectedTabIndex) {
        searchQuery = ""
        refreshSavedFiles()
    }

    val reportesFiltrados = remember(savedFilesList, searchQuery) {
        if (searchQuery.isBlank()) {
            savedFilesList
        } else {
            savedFilesList.filter { it.nombre.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Layers de captura de imagen de Compose
    val dailyGraphicsLayer = rememberGraphicsLayer()
    val weeklyGraphicsLayer = rememberGraphicsLayer()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Nodos Ocultos para Captura de Píxeles de Imagen PNG en la Composición
            Box(
                modifier = Modifier
                    .absoluteOffset(x = (-10000).dp)
                    .wrapContentSize(unbounded = true)
                    .drawWithContent {
                        dailyGraphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
            ) {
                ReportExporter.ReporteVisualContent(
                    todayRecords = reportDisplayRecords,
                    backlogRecords = pendingBacklogRecords,
                    rawCompletedRecords = todayCompletedProductionRecords,
                    displayDate = displayTodayDate
                )
            }

            Box(
                modifier = Modifier
                    .absoluteOffset(x = (-10000).dp)
                    .wrapContentSize(unbounded = true)
                    .drawWithContent {
                        weeklyGraphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
            ) {
                ReportExporter.ReporteSemanalVisualCard(
                    rangeDateDisplay = weekRangeDisplay,
                    totalSemanal = totalSemanal,
                    mejorDia = mejorDia,
                    top3Productos = top3Productos,
                    desgloseProductos = desgloseProductos
                )
            }

            // Contenido Visible
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Selector de pestañas TabRow
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transición fluida entre Pestañas
                AnimatedContent(
                    targetState = selectedTabIndex,
                    label = "tab_transition",
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    }
                ) { tabIndex ->
                    if (tabIndex == 0) {
                        // TAB 1: REPORTE DIARIO
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
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
                                                text = "Reporte Visual Diario de Hoy",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Fecha actual: $displayTodayDate",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LoadingButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isSavingAndOpening = true
                                                try {
                                                    val imageBitmap = try {
                                                        dailyGraphicsLayer.toImageBitmap()
                                                    } catch (e: Throwable) {
                                                        null
                                                    }

                                                    if (imageBitmap == null) {
                                                        Toast.makeText(context, "No se pudo procesar la imagen del reporte.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }

                                                    val bitmap = imageBitmap.asAndroidBitmap()
                                                    val fileName = FileManager.buildDailyFileName(todayIsoDate)
                                                    val uri = FileManager.guardarImagen(context, bitmap, TipoReporte.DIARIO, fileName)

                                                    if (uri != null) {
                                                        Toast.makeText(context, "Guardado en Documents/Diario TDN/Reportes Diarios", Toast.LENGTH_SHORT).show()
                                                        FileManager.abrirArchivo(context, uri)
                                                        refreshSavedFiles()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error al generar reporte: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSavingAndOpening = false
                                                }
                                            }
                                        },
                                        isLoading = isSavingAndOpening || isGeneratingReport,
                                        enabled = !isSavingAndOpening && !isSharingDirectly,
                                        isOutlined = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text("Guardar y Abrir")
                                    }

                                    LoadingButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isSharingDirectly = true
                                                try {
                                                    val imageBitmap = try {
                                                        dailyGraphicsLayer.toImageBitmap()
                                                    } catch (e: Throwable) {
                                                        null
                                                    }

                                                    if (imageBitmap == null) {
                                                        Toast.makeText(context, "No se pudo procesar la imagen del reporte.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }

                                                    val bitmap = imageBitmap.asAndroidBitmap()
                                                    val tempUri = FileManager.guardarEnCacheParaCompartir(context, bitmap)

                                                    if (tempUri != null) {
                                                        val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale("es", "ES"))
                                                        val fechaFormateada = sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                                        val textoCompartirDiario = "Reporte $fechaFormateada"
                                                        FileManager.compartirArchivo(context, tempUri, "Reporte Diario", textoCompartirDiario)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error al compartir reporte: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSharingDirectly = false
                                                }
                                            }
                                        },
                                        isLoading = isSharingDirectly || isGeneratingReport,
                                        enabled = !isSavingAndOpening && !isSharingDirectly,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text("Compartir Directo")
                                    }
                                }
                            }

                            // Buscador de Archivos Guardados
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    placeholder = { Text("Buscar reporte en disco...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Buscar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
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

                            // Sección Integrada: Lista de Archivos Guardados en Disco
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📁 Reportes Diarios Guardados (${reportesFiltrados.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(onClick = {
                                        val permisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                                        } else {
                                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                        permissionLauncher.launch(permisos)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reescanear carpeta",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (reportesFiltrados.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(),
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
                                                text = if (searchQuery.isNotBlank()) "No se encontraron reportes para \"$searchQuery\"." else "No hay reportes diarios guardados en disco.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = reportesFiltrados,
                                    key = { item -> item.id }
                                ) { archivo ->
                                    Box(modifier = Modifier.animateItem()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = archivo.nombre,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                        .format(Date(archivo.fechaModificacion))
                                                    Text(
                                                        text = "Guardado: $formattedDate",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            FileManager.abrirArchivo(context, archivo.uri)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = "Abrir",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            reporteABorrar = archivo
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: REPORTE SEMANAL
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Reporte Ejecutivo Semanal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = weekRangeDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "MÉTRICAS CLAVE",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Total Fabricado: $totalSemanal ítems",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Día más productivo: $mejorDia",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            // Sección Desglose de Productos Contraíble (Expand & Collapse)
                            item {
                                var isDesgloseExpanded by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isDesgloseExpanded = !isDesgloseExpanded },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📊 DESGLOSE DE PRODUCTOS DE LA SEMANA (${desgloseProductos.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            IconButton(onClick = { isDesgloseExpanded = !isDesgloseExpanded }) {
                                                Icon(
                                                    imageVector = if (isDesgloseExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (isDesgloseExpanded) "Contraer" else "Expandir",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = isDesgloseExpanded) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (desgloseProductos.isEmpty()) {
                                                    Text(
                                                        text = "No hay datos de producción registrados esta semana.",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                } else {
                                                    desgloseProductos.forEachIndexed { index, (prod, qty) ->
                                                        val posEmoji = when (index) {
                                                            0 -> "🥇"
                                                            1 -> "🥈"
                                                            2 -> "🥉"
                                                            else -> "🔸"
                                                        }
                                                        Text(
                                                            text = "$posEmoji ${index + 1}. $prod — $qty unds",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LoadingButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isSavingAndOpening = true
                                                try {
                                                    val imageBitmap = try {
                                                        weeklyGraphicsLayer.toImageBitmap()
                                                    } catch (e: Throwable) {
                                                        null
                                                    }

                                                    if (imageBitmap == null) {
                                                        Toast.makeText(context, "No se pudo procesar la imagen del reporte.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }

                                                    val bitmap = imageBitmap.asAndroidBitmap()
                                                    val fileName = FileManager.buildWeeklyFileName(currentWeekStartIso, currentWeekEndIso)
                                                    val uri = FileManager.guardarImagen(context, bitmap, TipoReporte.SEMANAL, fileName)

                                                    if (uri != null) {
                                                        Toast.makeText(context, "Guardado en Documents/Diario TDN/Reportes Semanales", Toast.LENGTH_SHORT).show()
                                                        FileManager.abrirArchivo(context, uri)
                                                        refreshSavedFiles()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error al generar reporte: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSavingAndOpening = false
                                                }
                                            }
                                        },
                                        isLoading = isSavingAndOpening || isGeneratingReport,
                                        enabled = !isSavingAndOpening && !isSharingDirectly,
                                        isOutlined = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text("Guardar y Abrir")
                                    }

                                    LoadingButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isSharingDirectly = true
                                                try {
                                                    val imageBitmap = try {
                                                        weeklyGraphicsLayer.toImageBitmap()
                                                    } catch (e: Throwable) {
                                                        null
                                                    }

                                                    if (imageBitmap == null) {
                                                        Toast.makeText(context, "No se pudo procesar la imagen del reporte.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }

                                                    val bitmap = imageBitmap.asAndroidBitmap()
                                                    val tempUri = FileManager.guardarEnCacheParaCompartir(context, bitmap)

                                                    if (tempUri != null) {
                                                        val textoCompartirSemanal = "Reporte Semanal $weekRangeDisplay"
                                                        FileManager.compartirArchivo(context, tempUri, "Reporte Semanal", textoCompartirSemanal)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error al compartir reporte: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSharingDirectly = false
                                                }
                                            }
                                        },
                                        isLoading = isSharingDirectly || isGeneratingReport,
                                        enabled = !isSavingAndOpening && !isSharingDirectly,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text("Compartir Directo")
                                    }
                                }
                            }

                            // Buscador de Archivos Guardados Semanales
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    placeholder = { Text("Buscar reporte en disco...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Buscar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
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

                            // Sección Integrada: Lista de Archivos Guardados en Disco
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📁 Reportes Semanales Guardados (${reportesFiltrados.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(onClick = {
                                        val permisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                                        } else {
                                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                        permissionLauncher.launch(permisos)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reescanear carpeta",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (reportesFiltrados.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(),
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
                                                text = if (searchQuery.isNotBlank()) "No se encontraron reportes para \"$searchQuery\"." else "No hay reportes semanales guardados en disco.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = reportesFiltrados,
                                    key = { item -> item.id }
                                ) { archivo ->
                                    Box(modifier = Modifier.animateItem()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = archivo.nombre,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                        .format(Date(archivo.fechaModificacion))
                                                    Text(
                                                        text = "Guardado: $formattedDate",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            FileManager.abrirArchivo(context, archivo.uri)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = "Abrir",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            reporteABorrar = archivo
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar Reporte
    reporteABorrar?.let { archivo ->
        AlertDialog(
            onDismissRequest = { reporteABorrar = null },
            title = {
                Text(
                    text = "Eliminar Reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que deseas eliminar '${archivo.nombre}'? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentType = if (selectedTabIndex == 0) TipoReporte.DIARIO else TipoReporte.SEMANAL
                        val exito = FileManager.eliminarReporteFisico(context, currentType, archivo.nombre)
                        if (exito) {
                            Toast.makeText(context, "Reporte eliminado con éxito", Toast.LENGTH_SHORT).show()
                            refreshSavedFiles()
                        } else {
                            Toast.makeText(context, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                        }
                        reporteABorrar = null
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { reporteABorrar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
