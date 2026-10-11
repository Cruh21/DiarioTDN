package com.example.diariotdn.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.diariotdn.R
import com.example.diariotdn.data.DailyRecord
import com.example.diariotdn.data.TaskStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private fun isTimestampDeHoy(timestamp: Long): Boolean {
        val hoy = java.util.Calendar.getInstance()
        val fechaItem = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return hoy.get(java.util.Calendar.YEAR) == fechaItem.get(java.util.Calendar.YEAR) &&
                hoy.get(java.util.Calendar.DAY_OF_YEAR) == fechaItem.get(java.util.Calendar.DAY_OF_YEAR)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ReporteVisualContent(
        todayRecords: List<DailyRecord>,
        backlogRecords: List<DailyRecord>,
        rawCompletedRecords: List<DailyRecord> = emptyList(),
        displayDate: String
    ) {
        val totalToday = todayRecords.size
        val totalBacklog = backlogRecords.size
        val generationTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // 1. Unificación en memoria de la producción completada HOY (Cruda)
        val effectiveCompletedRecords = remember(todayRecords, backlogRecords, rawCompletedRecords) {
            if (rawCompletedRecords.isNotEmpty()) {
                rawCompletedRecords
            } else {
                val completedList = mutableListOf<DailyRecord>()

                todayRecords.filter { it.getTaskStatus() == TaskStatus.COMPLETADO }.let {
                    completedList.addAll(it)
                }

                backlogRecords.filter { rec ->
                    rec.getTaskStatus() == TaskStatus.COMPLETADO &&
                            isTimestampDeHoy(rec.timestamp)
                }.let {
                    completedList.addAll(it)
                }

                completedList
            }
        }

        val totalCompletedToday = effectiveCompletedRecords.size

        // 2. Agrupación en memoria de la producción completada hoy por tipo de artículo
        val productionByProduct = remember(effectiveCompletedRecords) {
            effectiveCompletedRecords
                .groupBy { it.producto.trim().ifEmpty { "General" } }
                .mapValues { entry -> entry.value.sumOf { it.cantidad.toIntOrNull() ?: 1 } }
                .filterValues { it > 0 }
        }

        Column(
            modifier = Modifier
                .requiredWidth(1080.dp) // Ancho estricto HD de 1080.dp
                .wrapContentHeight(unbounded = true) // Crecimiento vertical desatado/infinito
                .background(Color.White)
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabecera Corporativa con distribución de espacios rígidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Logo Nazareth (Alineado a la izquierda)
                Image(
                    painter = painterResource(id = R.drawable.logo_nazareth),
                    contentDescription = "Logo Nazareth",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // Columna de Títulos (Centro) ocupando todo el espacio restante disponible
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "DIARIO TDN - NAZARETH",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E),
                        maxLines = 1
                    )
                    Text(
                        text = "Reporte Oficial de Producción Diaria",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5C6BC0),
                        maxLines = 1
                    )
                }

                // Columna de Fecha y Hora (Derecho) estampando la FECHA EXACTA recibida por parámetro
                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "FECHA DE EMISIÓN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = displayDate, // Usar la fecha exacta enviada
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Hora: $generationTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.5.dp)

            // 1. REUBICACIÓN: RESUMEN EJECUTIVO DE PRODUCCIÓN (Movido al inicio)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RESUMEN EJECUTIVO DE PRODUCCIÓN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Completados hoy: $totalCompletedToday ítem(s) en total",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A237E)
                        )
                    }

                    Text(
                        text = "Generado por Diario TDN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        textAlign = TextAlign.End
                    )
                }

                if (productionByProduct.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                    Text(
                        text = "DETALLE DE PRODUCCIÓN POR ARTÍCULO (COMPLETADOS HOY):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        productionByProduct.forEach { (producto, total) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8EAF6))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$producto: $total",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A237E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 1: Producción del Día
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📅 REGISTROS DEL DÍA ($totalToday)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )

                if (todayRecords.isEmpty()) {
                    Text(
                        text = "• Sin registros iniciados hoy.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    todayRecords.forEach { record ->
                        ReporteItemRow(record = record)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sección 2: Trabajo Atrasado (Backlog)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "⚠️ TRABAJO ATRASADO - BACKLOG ($totalBacklog)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalBacklog > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )

                if (backlogRecords.isEmpty()) {
                    Text(
                        text = "• Al día: No hay pendientes atrasados.",
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    backlogRecords.forEach { record ->
                        ReporteItemRow(record = record)
                    }
                }
            }
        }
    }

    @Composable
    private fun ReporteItemRow(record: DailyRecord) {
        val status = record.getTaskStatus()
        val statusColor = when (status) {
            TaskStatus.PENDIENTE -> Color(0xFFD32F2F)
            TaskStatus.EN_PROCESO -> Color(0xFFF57F17)
            TaskStatus.COMPLETADO -> Color(0xFF2E7D32)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.emoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Text(
                text = record.getContentWithoutDate(),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = status.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }

    // REDISEÑO: REPORTE SEMANAL VISUAL CARD (Dashboard Gerencial)
    @Composable
    fun ReporteSemanalVisualCard(
        rangeDateDisplay: String,
        totalSemanal: Int,
        mejorDia: String,
        top3Productos: List<Pair<String, Int>> = emptyList(),
        desgloseProductos: List<Pair<String, Int>> = top3Productos
    ) {
        val generationTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val itemsToDisplay = if (desgloseProductos.isNotEmpty()) desgloseProductos else top3Productos

        Column(
            modifier = Modifier
                .requiredWidth(1080.dp) // Ancho estricto HD de 1080.dp
                .wrapContentHeight(unbounded = true)
                .background(Color.White)
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Cabecera Corporativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_nazareth),
                    contentDescription = "Logo Nazareth",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DIARIO TDN - NAZARETH",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E),
                        maxLines = 1
                    )
                    Text(
                        text = "REPORTE SEMANAL DE PRODUCCIÓN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6BC0),
                        maxLines = 1
                    )
                }

                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "PERÍODO SEMANAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = rangeDateDisplay,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Hora: $generationTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.5.dp)

            // 2. REDISEÑO DE MÉTRICAS CLAVE (Total Centrado Gigante 48.sp + Card para Día Más Productivo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F9FA))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MÉTRICAS CLAVE DE LA SEMANA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // Total Fabricado Gigante
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalSemanal",
                        fontSize = 48.sp, // Tamaño Gigante
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = "ítems fabricados esta semana",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6BC0)
                    )
                }

                // Día más productivo envuelto en un Card/Box resaltado suave
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9)) // Verde suave resaltante
                        .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🚀 Día más productivo:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = mejorDia,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // 3. DESGLOSE DE PRODUCTOS FABRICADOS (TODOS LOS PRODUCTOS DE LA SEMANA)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "📊 DESGLOSE DE PRODUCTOS FABRICADOS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )

                if (itemsToDisplay.isEmpty()) {
                    Text(
                        text = "• Sin datos de producción registrados esta semana.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    itemsToDisplay.forEachIndexed { index, (producto, cantidad) ->
                        val posEmoji = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "🔸"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFFFFF))
                                .border(1.5.dp, Color(0xFFE3F2FD), RoundedCornerShape(10.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = posEmoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(end = 14.dp)
                                    )
                                    Text(
                                        text = "${index + 1}. $producto",
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                }

                                Text(
                                    text = "$cantidad unds",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1A237E)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.5.dp)

            // Pie de página
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Diario TDN - Centro de Mando", fontSize = 12.sp, color = Color.Gray)
                Text(text = "Sistema de Gestión Nazareth", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }

    /**
     * Guarda el mapa de bits nativo capturado desde Compose GraphicsLayer
     * y lanza Intent.ACTION_SEND con compresión PNG al 100% libre de pérdidas.
     */
    fun shareCapturedReportBitmap(
        context: Context,
        bitmap: Bitmap?,
        displayDate: String
    ) {
        if (bitmap == null) {
            Toast.makeText(context, "Error: La imagen del reporte no se pudo generar.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val imageFile = File(reportsDir, "reporte_produccion_${System.currentTimeMillis()}.png")

            // Compresión PNG sin pérdidas al 100% de calidad nativa
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Producción $displayDate")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte de producción del $displayDate.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir Reporte Visual")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir la imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
