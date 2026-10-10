package com.example.diariotdn.data

import androidx.room.Embedded
import androidx.room.Relation

data class LoteWithRecords(
    @Embedded val lote: Lote,
    @Relation(
        parentColumn = "id",
        entityColumn = "loteId"
    )
    val records: List<DailyRecord> = emptyList()
) {
    val completedCount: Int
        get() = records.count { it.estado == TaskStatus.COMPLETADO.name }

    val totalCount: Int
        get() = records.size

    val progressText: String
        get() = "$completedCount/$totalCount completados"

    val progressRatio: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}
