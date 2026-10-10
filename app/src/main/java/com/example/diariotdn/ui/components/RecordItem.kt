package com.example.diariotdn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.TaskStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ColorStatusIndicator(
    status: TaskStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = when (status) {
        TaskStatus.PENDIENTE -> Color(0xFFD32F2F) // Rojo
        TaskStatus.EN_PROCESO -> Color(0xFFFFB300) // Amarillo/Ámbar
        TaskStatus.COMPLETADO -> Color(0xFF2E7D32) // Verde
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350),
        label = "statusColorAnimation"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "statusScaleAnimation"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(animatedColor)
            .clickable {
                scope.launch {
                    isPressed = true
                    delay(100)
                    isPressed = false
                }
                onClick()
            }
    )
}

@Composable
fun RecordItem(
    record: DailyRecord,
    onDelete: (DailyRecord) -> Unit,
    modifier: Modifier = Modifier,
    onCopySingle: ((DailyRecord) -> Unit)? = null,
    onStatusClick: ((DailyRecord) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showProduct: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onStatusClick != null) {
                ColorStatusIndicator(
                    status = record.getTaskStatus(),
                    onClick = { onStatusClick(record) },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                val textToShow = if (!showProduct) {
                    // Modo Lote: Se omite la cantidad y el producto. Muestra "{Rango} {Apellidos/Nombres} {Codigo} ({Extra})"
                    val contentWithoutProductOrQty = listOf(
                        record.rango.trim(),
                        record.apellidosNombres.trim(),
                        record.codigoUnidad.trim()
                    ).filter { it.isNotBlank() }.joinToString(" ")

                    val extra = record.textoExtra.trim()
                    if (extra.isNotEmpty()) "$contentWithoutProductOrQty ($extra)" else contentWithoutProductOrQty
                } else {
                    // Modo Estándar: Muestra el texto del registro SIN la fecha repetida en la UI
                    record.getContentWithoutDate()
                }

                Text(
                    text = textToShow,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Solo botón de eliminar (papelera)
            IconButton(onClick = { onDelete(record) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar registro",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
