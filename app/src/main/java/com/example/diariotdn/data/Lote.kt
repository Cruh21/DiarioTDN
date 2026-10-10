package com.example.diariotdn.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lotes")
data class Lote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreLote: String, // Ej: "Lote Maurera"
    val productoLote: String = "", // Ej: "Brazalete"
    val fechaCreacion: String, // ISO YYYY-MM-DD
    val estadoGeneral: String = TaskStatus.EN_PROCESO.name,
    val timestamp: Long = System.currentTimeMillis()
)
