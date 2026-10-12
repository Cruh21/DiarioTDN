package com.example.diariotdn.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diariotdn.ui.components.FullScreenLoader
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.util.ArchivoReporte
import com.example.diariotdn.util.DateUtils
import com.example.diariotdn.util.FileManager
import com.example.diariotdn.util.TipoReporte
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: DailyRecordViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var selectedExportDate by remember { mutableStateOf(DateUtils.getTodayIsoDate()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Estados para la Lista de Copias de Seguridad Guardadas en Disco
    var savedBackupsList by remember { mutableStateOf<List<ArchivoReporte>>(emptyList()) }
    var backupABorrar by remember { mutableStateOf<ArchivoReporte?>(null) }
    var backupAImportar by remember { mutableStateOf<ArchivoReporte?>(null) }

    fun refreshSavedBackups() {
        savedBackupsList = FileManager.obtenerReportesGuardados(context, TipoReporte.BACKUP)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshSavedBackups()
    }

    LaunchedEffect(Unit) {
        refreshSavedBackups()
    }

    // Launcher para Exportar TODO por SAF (Opcional)
    val exportAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportAllData(context, uri) { result ->
                result.fold(
                    onSuccess = { count ->
                        Toast.makeText(
                            context,
                            "Copia de seguridad exportada con éxito ($count elementos).",
                            Toast.LENGTH_LONG
                        ).show()
                        refreshSavedBackups()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            "Error al exportar: ${error.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    // Launcher para Exportar Fecha por SAF (Opcional)
    val exportDateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDateData(context, uri, selectedExportDate) { result ->
                result.fold(
                    onSuccess = { count ->
                        Toast.makeText(
                            context,
                            "Se exportaron $count registros del $selectedExportDate.",
                            Toast.LENGTH_LONG
                        ).show()
                        refreshSavedBackups()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            "Error al exportar: ${error.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    // Launcher para Importar Datos desde JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri) { result ->
                result.fold(
                    onSuccess = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            "Error al importar archivo: ${error.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner Informativo
            /*
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Sistema de Backup Sólido (JSON)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Exporta tus datos a un archivo JSON seguro o restaura información previa sin duplicar registros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
*/
            // Sección 1: Exportar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Exportar Copia de Seguridad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.exportAllDataDirect(context) { result ->
                                    result.fold(
                                        onSuccess = { (count, _) ->
                                            Toast.makeText(
                                                context,
                                                "Copia de seguridad guardada en Documents/Diario TDN/Backups/ ($count elementos).",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            refreshSavedBackups()
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(
                                                context,
                                                "Error al exportar: ${error.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar Copia de seguridad")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "O exporta solo una fecha específica:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(DateUtils.isoToDisplayDate(selectedExportDate))
                            }

                            Button(
                                onClick = {
                                    viewModel.exportDateDataDirect(context, selectedExportDate) { result ->
                                        result.fold(
                                            onSuccess = { (count, _) ->
                                                Toast.makeText(
                                                    context,
                                                    "Backup de fecha guardado en Documents/Diario TDN/Backups/ ($count registros).",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                refreshSavedBackups()
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(
                                                    context,
                                                    "Error al exportar: ${error.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text("Guardar Backup Fecha")
                            }
                        }
                    }
                }
            }

            // Sección 2: Importar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Restaurar Copia de Seguridad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Selecciona un archivo .json previamente exportado para restaurar registros y opciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Seleccionar Archivo JSON e Importar")
                        }
                    }
                }
            }

            // Sección 3: UI de la Lista de Backups Guardados en Disco
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📁 Copias de Seguridad Guardadas (${savedBackupsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = {
                        val permisos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permisos)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar lista de backups",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (savedBackupsList.isEmpty()) {
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
                                text = "No hay copias de seguridad guardadas en Documents/Diario TDN/Backups.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = savedBackupsList,
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
                                            backupAImportar = archivo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Restaurar esta copia",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            backupABorrar = archivo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar backup",
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

        // Loader de Pantalla Completa durante Exportación / Importación
        FullScreenLoader(
            isProcessing = isExporting || isImporting,
            message = if (isExporting) "Exportando datos de la base de datos..." else "Restaurando copia de seguridad..."
        )
    }

    // DatePicker Dialog para seleccionar fecha a exportar
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            selectedExportDate = DateUtils.millisToIsoDate(selectedMillis)
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

    // Diálogo de Confirmación para Eliminar Backup
    backupABorrar?.let { archivo ->
        AlertDialog(
            onDismissRequest = { backupABorrar = null },
            title = {
                Text(
                    text = "Eliminar Copia de Seguridad",
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
                        val deleted = FileManager.eliminarReporte(context, archivo.uri)
                        if (deleted) {
                            Toast.makeText(context, "Copia de seguridad eliminada", Toast.LENGTH_SHORT).show()
                            refreshSavedBackups()
                        } else {
                            Toast.makeText(context, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                        }
                        backupABorrar = null
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
                TextButton(onClick = { backupABorrar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Confirmación para Restaurar/Importar Backup
    backupAImportar?.let { archivo ->
        AlertDialog(
            onDismissRequest = { backupAImportar = null },
            title = {
                Text(
                    text = "Restaurar Copia de Seguridad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que deseas restaurar los datos desde '${archivo.nombre}'? Esta acción reemplazará todos los registros actuales y no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importData(context, archivo.uri) { result ->
                            result.fold(
                                onSuccess = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "Error al restaurar: ${error.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                        backupAImportar = null
                    }
                ) {
                    Text(
                        text = "Restaurar",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { backupAImportar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
