package com.example.diariotdn.util

import java.text.Normalizer

/**
 * Normaliza una cadena de texto eliminando tildes/diacríticos y convirtiendo a minúsculas
 * Ejemplo: "Velásquez" -> "velasquez"
 */
fun String.normalizeForSearch(): String {
    if (this.isEmpty()) return ""
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
}
