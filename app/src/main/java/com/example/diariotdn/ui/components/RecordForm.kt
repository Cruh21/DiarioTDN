package com.example.diariotdn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diariotdn.data.DailyRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrictQuantityDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val staticCantidadOptions = remember { (1..100).map { it.toString() } }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value.ifBlank { "1" },
            onValueChange = {},
            readOnly = true, // Estricto: Impide que se abra el teclado virtual
            label = { Text("Cantidad *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 220.dp) // Scroll cómodo de los 100 números
        ) {
            staticCantidadOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RecordForm(
    displayDate: String,
    cantidad: String,
    producto: String,
    rango: String,
    apellidosNombres: String,
    codigoUnidad: String,
    textoExtra: String,
    cantidadOptions: List<String> = emptyList(),
    productoOptions: List<String> = emptyList(),
    rangoOptions: List<String> = emptyList(),
    codigoOptions: List<String> = emptyList(),
    errorMessage: String?,
    onCantidadChange: (String) -> Unit,
    onProductoChange: (String) -> Unit,
    onRangoChange: (String) -> Unit,
    onApellidosNombresChange: (String) -> Unit,
    onCodigoUnidadChange: (String) -> Unit,
    onTextoExtraChange: (String) -> Unit,
    onSaveRecord: () -> Unit,
    modifier: Modifier = Modifier,
    showProductField: Boolean = true,
    showQuantityField: Boolean = true,
    isSaving: Boolean = false
) {
    fun formatTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { char -> char.uppercase() }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val titleText = if (showProductField && showQuantityField) {
                "Nuevo Registro para [$displayDate]"
            } else {
                "Ítem del Lote"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Fila 1: Cantidad Estricta (No Abre Teclado) y/o Producto
            if (showQuantityField || showProductField) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showQuantityField) {
                        StrictQuantityDropdownField(
                            value = cantidad,
                            onValueChange = onCantidadChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (showProductField) {
                        HybridDropdownField(
                            value = producto,
                            onValueChange = { newValue ->
                                onProductoChange(formatTitleCase(newValue))
                            },
                            options = productoOptions,
                            label = "Producto *",
                            placeholder = "Ej: Brazalete",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }

            // Fila 2: Rango (Opcional con ALL CAPS) y Código (Numérico sin "P-")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HybridDropdownField(
                    value = rango,
                    onValueChange = { newValue ->
                        onRangoChange(newValue.uppercase())
                    },
                    options = rangoOptions,
                    label = "Rango",
                    placeholder = "Ej: S/2",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1.5f)
                )
                HybridDropdownField(
                    value = codigoUnidad.removePrefix("P-"),
                    onValueChange = { newValue ->
                        val cleanNum = newValue.uppercase().removePrefix("P-")
                        onCodigoUnidadChange(cleanNum)
                    },
                    options = codigoOptions.map { it.removePrefix("P-") },
                    label = "Código",
                    placeholder = "Ej: 130",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f)
                )
            }

            // Fila 3: Apellidos/Nombres (Obligatorio con Capitalización Automática)
            OutlinedTextField(
                value = apellidosNombres,
                onValueChange = { newValue ->
                    onApellidosNombresChange(formatTitleCase(newValue))
                },
                label = { Text("Apellidos / Nombres *") },
                placeholder = { Text("Ej: J. Belisario T.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Fila 4: Texto Extra (Opcional con ALL CAPS)
            OutlinedTextField(
                value = textoExtra,
                onValueChange = { newValue ->
                    onTextoExtraChange(newValue.uppercase())
                },
                label = { Text("Texto Extra (Opcional)") },
                placeholder = { Text("Ej: D-523") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Vista previa en vivo del texto limpia
            val formattedCodeForPreview = if (codigoUnidad.isBlank()) "" else if (codigoUnidad.startsWith("P-")) codigoUnidad else "P-$codigoUnidad"

            val previewText = if (showProductField && showQuantityField) {
                DailyRecord.buildRecordContentWithoutDate(
                    cantidad = cantidad.ifBlank { "1" },
                    producto = producto.ifBlank { "..." },
                    rango = rango,
                    apellidosNombres = apellidosNombres.ifBlank { "..." },
                    codigoUnidad = formattedCodeForPreview,
                    textoExtra = textoExtra
                )
            } else {
                val parts = listOf(
                    rango.trim(),
                    apellidosNombres.ifBlank { "..." }.trim(),
                    formattedCodeForPreview.trim()
                ).filter { it.isNotBlank() }

                val mainText = parts.joinToString(" ")
                val extra = textoExtra.trim()
                if (extra.isNotEmpty()) "$mainText ($extra)" else mainText
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Vista previa del formato:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            LoadingButton(
                onClick = onSaveRecord,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Guardar Ítem")
            }
        }
    }
}
