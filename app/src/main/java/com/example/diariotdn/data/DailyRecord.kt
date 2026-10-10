package com.example.diariotdn.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_records",
    foreignKeys = [
        ForeignKey(
            entity = Lote::class,
            parentColumns = ["id"],
            childColumns = ["loteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("loteId")]
)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // Fecha ISO "YYYY-MM-DD"
    val formattedDate: String, // Fecha "DD/MM/AA"
    val cantidad: String,
    val producto: String,
    val rango: String = "",
    val apellidosNombres: String,
    val codigoUnidad: String = "",
    val textoExtra: String = "",
    val formattedRecord: String,
    val timestamp: Long = System.currentTimeMillis(),
    val estado: String = TaskStatus.PENDIENTE.name, // "PENDIENTE", "EN_PROCESO", "COMPLETADO"
    val loteId: Int? = null // Nullable ID de relación con Lote
) {
    fun getTaskStatus(): TaskStatus = TaskStatus.fromString(estado)

    /**
     * Retorna la línea minimalista para copiar dentro de un lote (Sin Cantidad):
     * {EMOJI} {Rango} {Nombre} {Codigo} ({Extra})
     * Ejemplo: 🔴 S/2 J Belisario T P-130
     */
    fun getContentForBatchCopy(): String {
        val emoji = getTaskStatus().emoji
        val parts = listOf(
            rango.trim(),
            apellidosNombres.trim(),
            codigoUnidad.trim()
        ).filter { it.isNotBlank() }

        val mainText = parts.joinToString(" ")
        val extra = textoExtra.trim()

        val text = if (extra.isNotEmpty()) {
            "$mainText ($extra)"
        } else {
            mainText
        }

        return "$emoji $text"
    }

    /**
     * Retorna la línea minimalista del registro con el emoji de estado:
     * {EMOJI} {Cantidad} {Producto} {Rango} {Nombre} {Codigo} ({Extra})
     * Ejemplo: 🔴 1 Camiseta J Belisario T
     */
    fun getContentWithStatusTag(): String {
        val emoji = getTaskStatus().emoji
        val content = getContentWithoutDate()
        return "$emoji $content"
    }

    /**
     * Retorna la línea del registro sin la fecha repetida:
     * {Cantidad} {Producto} {Rango} {Nombre} {Codigo} ({Extra})
     */
    fun getContentWithoutDate(): String {
        return buildRecordContentWithoutDate(
            cantidad = cantidad,
            producto = producto,
            rango = rango,
            apellidosNombres = apellidosNombres,
            codigoUnidad = codigoUnidad,
            textoExtra = textoExtra
        )
    }

    companion object {
        /**
         * Arma el contenido del registro SIN la fecha:
         * {Cantidad} {Producto} {Rango} {Nombre} {Codigo} ({Extra})
         */
        fun buildRecordContentWithoutDate(
            cantidad: String,
            producto: String,
            rango: String,
            apellidosNombres: String,
            codigoUnidad: String,
            textoExtra: String
        ): String {
            val parts = listOf(
                cantidad.trim(),
                producto.trim(),
                rango.trim(),
                apellidosNombres.trim(),
                codigoUnidad.trim()
            ).filter { it.isNotBlank() }

            val mainText = parts.joinToString(" ")
            val extra = textoExtra.trim()

            return if (extra.isNotEmpty()) {
                "$mainText ($extra)"
            } else {
                mainText
            }
        }

        /**
         * Formato completo individual con la fecha al inicio:
         * [DD/MM/AA] {Cantidad} {Producto} {Rango} {Nombre} {Codigo} ({Extra})
         */
        fun buildFormattedRecord(
            displayDate: String,
            cantidad: String,
            producto: String,
            rango: String,
            apellidosNombres: String,
            codigoUnidad: String,
            textoExtra: String
        ): String {
            val content = buildRecordContentWithoutDate(
                cantidad = cantidad,
                producto = producto,
                rango = rango,
                apellidosNombres = apellidosNombres,
                codigoUnidad = codigoUnidad,
                textoExtra = textoExtra
            )
            return "[$displayDate] $content"
        }
    }
}
