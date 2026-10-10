package com.example.diariotdn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diariotdn.data.CategoryOption
import com.example.diariotdn.data.CategoryType
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigCategoriesScreen(
    viewModel: DailyRecordViewModel,
    modifier: Modifier = Modifier
) {
    // Excluir CANTIDAD de la pantalla de configuración visual
    val editableCategories = remember { CategoryType.entries.filter { it != CategoryType.CANTIDAD } }
    var selectedCategory by remember { mutableStateOf(editableCategories.firstOrNull() ?: CategoryType.PRODUCTO) }
    var newOptionText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddOptionSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun formatTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { char -> char.uppercase() }
        }
    }

    // Estados para Programación Defensiva al eliminar / editar una Opción de Categoría
    var optionToDelete by remember { mutableStateOf<CategoryOption?>(null) }
    var optionToEdit by remember { mutableStateOf<CategoryOption?>(null) }
    var editOptionText by remember { mutableStateOf("") }
    var editDialogError by remember { mutableStateOf<String?>(null) }

    val productoOptions by viewModel.productoOptions.collectAsState()
    val rangoOptions by viewModel.rangoOptions.collectAsState()
    val codigoOptions by viewModel.codigoOptions.collectAsState()

    val currentOptions = remember(selectedCategory, productoOptions, rangoOptions, codigoOptions) {
        when (selectedCategory) {
            CategoryType.PRODUCTO -> productoOptions
            CategoryType.RANGO -> rangoOptions
            CategoryType.CODIGO -> codigoOptions
            else -> emptyList()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newOptionText = ""
                    errorMessage = null
                    showAddOptionSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar Opción"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta con información del módulo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Configuración de Opciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Gestiona las listas desplegables de la pantalla principal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Selector de Categoría (FilterChips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                editableCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            newOptionText = ""
                            errorMessage = null
                        },
                        label = { Text(category.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Título de lista de opciones guardadas
            Text(
                text = "Opciones Actuales (${currentOptions.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Lista de opciones registradas con botones para editar y eliminar
            if (currentOptions.isEmpty()) {
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
                            text = "No hay opciones registradas para ${selectedCategory.displayName}.\nAgrega una usando el botón flotante (+).",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = currentOptions,
                        key = { item -> item.id }
                    ) { option ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            optionToEdit = option
                                            editOptionText = option.value.removePrefix("P-")
                                            editDialogError = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar opción",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            optionToDelete = option
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar opción",
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

    // ModalBottomSheet para agregar nueva opción
    if (showAddOptionSheet) {
        val keyboardOpts = when (selectedCategory) {
            CategoryType.PRODUCTO -> KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            CategoryType.RANGO -> KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            CategoryType.CODIGO -> KeyboardOptions(keyboardType = KeyboardType.Number)
            else -> KeyboardOptions.Default
        }

        val placeholderText = when (selectedCategory) {
            CategoryType.PRODUCTO -> "Ej: Brazalete"
            CategoryType.RANGO -> "Ej: S/2"
            CategoryType.CODIGO -> "Ej: 130"
            else -> "Ej: Nuevo valor"
        }

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAddOptionSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nueva opción para ${selectedCategory.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = newOptionText,
                    onValueChange = { newValue ->
                        errorMessage = null
                        newOptionText = when (selectedCategory) {
                            CategoryType.PRODUCTO -> formatTitleCase(newValue)
                            CategoryType.RANGO -> newValue.uppercase()
                            CategoryType.CODIGO -> newValue.uppercase().removePrefix("P-")
                            else -> newValue
                        }
                    },
                    label = { Text("Valor de la opción *") },
                    placeholder = { Text(placeholderText) },
                    keyboardOptions = keyboardOpts,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val optionValueToSave = if (selectedCategory == CategoryType.CODIGO) {
                            val clean = newOptionText.trim().uppercase().removePrefix("P-")
                            if (clean.isEmpty()) "" else "P-$clean"
                        } else {
                            newOptionText.trim()
                        }

                        viewModel.addCategoryOption(
                            categoryType = selectedCategory,
                            value = optionValueToSave,
                            onSuccess = {
                                newOptionText = ""
                                errorMessage = null
                                showAddOptionSheet = false
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Guardar Opción")
                }
            }
        }
    }

    // Diálogo de Edición de Opción de Categoría
    if (optionToEdit != null) {
        val keyboardOpts = when (selectedCategory) {
            CategoryType.PRODUCTO -> KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            CategoryType.RANGO -> KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            CategoryType.CODIGO -> KeyboardOptions(keyboardType = KeyboardType.Number)
            else -> KeyboardOptions.Default
        }

        AlertDialog(
            onDismissRequest = {
                optionToEdit = null
                editOptionText = ""
                editDialogError = null
            },
            title = {
                Text(
                    text = "Editar opción de ${selectedCategory.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editOptionText,
                        onValueChange = { newValue ->
                            editDialogError = null
                            editOptionText = when (selectedCategory) {
                                CategoryType.PRODUCTO -> formatTitleCase(newValue)
                                CategoryType.RANGO -> newValue.uppercase()
                                CategoryType.CODIGO -> newValue.uppercase().removePrefix("P-")
                                else -> newValue
                            }
                        },
                        label = { Text("Valor de la opción *") },
                        keyboardOptions = keyboardOpts,
                        singleLine = true,
                        isError = editDialogError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editDialogError != null) {
                        Text(
                            text = editDialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        optionToEdit?.let { opt ->
                            viewModel.updateCategoryOption(
                                option = opt,
                                newValue = editOptionText,
                                onSuccess = {
                                    optionToEdit = null
                                    editOptionText = ""
                                    editDialogError = null
                                },
                                onError = { err ->
                                    editDialogError = err
                                }
                            )
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        optionToEdit = null
                        editOptionText = ""
                        editDialogError = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Confirmación para Eliminar una Opción de Categoría
    if (optionToDelete != null) {
        AlertDialog(
            onDismissRequest = { optionToDelete = null },
            title = {
                Text(
                    text = "Eliminar Opción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Seguro que deseas eliminar la opción \"${optionToDelete?.value}\" de ${selectedCategory.displayName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        optionToDelete?.let { viewModel.deleteCategoryOption(it) }
                        optionToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { optionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
