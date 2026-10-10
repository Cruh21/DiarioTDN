package com.example.diariotdn.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType(val displayName: String) {
    CANTIDAD("Cantidad"),
    PRODUCTO("Producto"),
    RANGO("Rango"),
    CODIGO("Código")
}

@Entity(tableName = "category_options")
data class CategoryOption(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String, // Usa CategoryType.name ("CANTIDAD", "PRODUCTO", "RANGO", "CODIGO")
    val value: String
)
