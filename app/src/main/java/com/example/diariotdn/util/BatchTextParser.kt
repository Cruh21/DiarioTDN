package com.example.diariotdn.util

import java.util.UUID

data class DraftRecord(
    val idTemp: String = UUID.randomUUID().toString(),
    var rango: String = "",
    var nombre: String = "",
    var codigoExtra: String = ""
)

object BatchTextParser {

    /**
     * Procesa texto sin formato (ej. copiado de WhatsApp) aplicando blindaje contra caracteres invisibles,
     * eliminación estricta de numeración y extracción por Regex de Rango, Nombre y Código.
     */
    fun procesarTextoLote(rawText: String): List<DraftRecord> {
        if (rawText.isBlank()) return emptyList()

        // 1. LIMPIEZA DE FANTASMAS (Caracteres invisibles de WhatsApp: LTR, RTL, espacios no rompibles)
        var cleanRawText = rawText
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u00A0", " ")

        // PRE-PROCESAMIENTO:
        // A) Reemplaza "Promoción " o "PROMOCIÓN " por "P-"
        cleanRawText = cleanRawText.replace("(?i)promoci[óo]n\\s*".toRegex(), "P-")

        // B) Deshace líneas huérfanas: junta saltos de línea seguidos de un código (ej. \nP-100 -> " P-100")
        cleanRawText = cleanRawText.replace("\n\\s*P-".toRegex(), " P-")

        // 2. FILTRADO ESTRICTO DE LÍNEAS VACÍAS O "Talla:"
        val lines = cleanRawText.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                        !line.contains("Talla:", ignoreCase = true) &&
                        !line.contains("MAS EL", ignoreCase = true)
            }

        val draftList = mutableListOf<DraftRecord>()

        // Regex para eliminación estricta de numeración inicial (ej. "1)", "01-", "1. ", "25-")
        val leadingNumberRegex = Regex("^0*\\d+[).-]\\s*")

        // Regex para extracción de código P-XX (ej. "P-90", "P- 100")
        val codeRegex = "P-\\s*\\d+".toRegex(RegexOption.IGNORE_CASE)

        fun formatTitleCase(input: String): String {
            if (input.isBlank()) return ""
            return input.split("\\s+".toRegex()).joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { char -> char.uppercase() }
            }
        }

        for (line in lines) {
            // 3. DESTRUCCIÓN DE NUMERACIÓN INICIAL
            var lineText = line.replace(leadingNumberRegex, "").trim()

            // B) EXTRACCIÓN DE CÓDIGO
            var extractedCode = ""
            val codeMatch = codeRegex.find(lineText)
            if (codeMatch != null) {
                extractedCode = codeMatch.value.replace("\\s+".toRegex(), "").uppercase()
                lineText = lineText.replace(codeMatch.value, "").trim()
            }

            // 4. LÓGICA DE RANGO Y NOMBRE CON LISTA MAESTRA DE PREFIJOS
            var extractedRango = ""
            var extractedNombre = ""

            if (lineText.isNotBlank()) {
                val parts = lineText.split(Regex("\\s+"), 2)
                val posibleRango = parts[0].trim().uppercase()

                // Lista maestra de inicios de jerarquía/rango
                val prefijosValidos = listOf("S/", "S1", "S2", "S3", "SM", "CAP", "PTTE", "TTE", "CNEL")

                if (prefijosValidos.any { posibleRango.startsWith(it) }) {
                    extractedRango = posibleRango
                    extractedNombre = formatTitleCase(parts.getOrNull(1)?.trim() ?: "")
                } else {
                    extractedRango = ""
                    extractedNombre = formatTitleCase(lineText.trim())
                }
            }

            // Solo agregamos si el ítem tiene un nombre o un código válido
            if (extractedNombre.isNotBlank() || extractedCode.isNotBlank()) {
                draftList.add(
                    DraftRecord(
                        rango = extractedRango,
                        nombre = extractedNombre,
                        codigoExtra = extractedCode
                    )
                )
            }
        }

        return draftList
    }
}
