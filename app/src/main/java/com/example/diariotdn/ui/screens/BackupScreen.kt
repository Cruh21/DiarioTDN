package com.example.diariotdn.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import com.example.diariotdn.ui.components.FullScreenLoader
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.util.DateUtils

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

    // Launcher para Exportar TODOS los Datos
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

    // Launcher para Exportar Registros de una Fecha
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
                                        onSuccess = { (count, uri) ->
                                            Toast.makeText(
                                                context,
                                                "Backup guardado en Documents/Diario TDN/Backups/ ($count elementos).",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            com.example.diariotdn.util.FileManager.abrirOCompartirImagen(
                                                context,
                                                uri,
                                                "Copia de Seguridad Completa"
                                            )
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
                            Text("Guardar Backup Completo en Documents/Diario TDN/Backups")
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
                                            onSuccess = { (count, uri) ->
                                                Toast.makeText(
                                                    context,
                                                    "Backup de fecha guardado en Documents/Diario TDN/Backups/ ($count registros).",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                com.example.diariotdn.util.FileManager.abrirOCompartirImagen(
                                                    context,
                                                    uri,
                                                    "Copia de Seguridad $selectedExportDate"
                                                )
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
}
