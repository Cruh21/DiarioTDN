package com.example.diariotdn.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {

    @Query("SELECT * FROM lotes ORDER BY timestamp DESC")
    fun getAllLotes(): Flow<List<Lote>>

    @Transaction
    @Query("SELECT * FROM lotes ORDER BY timestamp DESC")
    fun getAllLotesWithRecords(): Flow<List<LoteWithRecords>>

    @Transaction
    @Query("SELECT * FROM lotes WHERE id = :loteId")
    fun getLoteWithRecordsById(loteId: Int): Flow<LoteWithRecords?>

    @Query("SELECT * FROM lotes WHERE id = :loteId")
    suspend fun getLoteByIdDirect(loteId: Int): Lote?

    @Query("SELECT * FROM lotes")
    suspend fun getAllLotesDirect(): List<Lote>

    @Query("SELECT COUNT(*) FROM lotes WHERE estadoGeneral != 'COMPLETADO'")
    fun getActiveLotesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLote(lote: Lote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLotes(lotes: List<Lote>)

    @Update
    suspend fun updateLote(lote: Lote)

    @Delete
    suspend fun deleteLote(lote: Lote)

    @Query("DELETE FROM lotes WHERE id = :id")
    suspend fun deleteLoteById(id: Int)
}
