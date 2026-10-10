package com.example.diariotdn.data

enum class TaskStatus(val displayName: String, val emoji: String) {
    PENDIENTE("Pendiente", "🔴"),
    EN_PROCESO("En Proceso", "🟡"),
    COMPLETADO("Completado", "🟢");

    fun next(): TaskStatus {
        return when (this) {
            PENDIENTE -> EN_PROCESO
            EN_PROCESO -> COMPLETADO
            COMPLETADO -> PENDIENTE
        }
    }

    fun getStatusTag(): String {
        return when (this) {
            COMPLETADO -> "[🟢 COMPLETADO]"
            EN_PROCESO -> "[🟡 EN PROCESO]"
            PENDIENTE -> "[🔴 PENDIENTE]"
        }
    }

    companion object {
        fun fromString(value: String?): TaskStatus {
            if (value == null) return PENDIENTE
            return entries.find {
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true)
            } ?: PENDIENTE
        }
    }
}
