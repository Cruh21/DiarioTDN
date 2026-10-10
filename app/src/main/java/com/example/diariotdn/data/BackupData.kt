package com.example.diariotdn.data

data class BackupData(
    val version: Int = 1,
    val exportDate: String = "",
    val lotes: List<Lote> = emptyList(),
    val dailyRecords: List<DailyRecord> = emptyList(),
    val categoryOptions: List<CategoryOption> = emptyList()
)
