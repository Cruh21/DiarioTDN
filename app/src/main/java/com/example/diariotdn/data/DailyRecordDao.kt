package com.example.diariotdn.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    // Registros del día (incluye opción de filtrar por loteId en memoria)
    @Query("SELECT * FROM daily_records WHERE date = :date ORDER BY timestamp ASC")
    fun getRecordsByDate(date: String): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getRecordsByDateDirect(date: String): List<DailyRecord>

    @Query("SELECT * FROM daily_records ORDER BY date DESC, timestamp ASC")
    fun getAllRecordsFlow(): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records ORDER BY timestamp ASC")
    suspend fun getAllRecordsDirect(): List<DailyRecord>

    @Query("SELECT DISTINCT date FROM daily_records ORDER BY date DESC")
    fun getAllSavedDates(): Flow<List<String>>

    @Query("SELECT * FROM daily_records WHERE loteId = :loteId ORDER BY timestamp ASC")
    fun getRecordsByLoteId(loteId: Int): Flow<List<DailyRecord>>

    // Registros anteriores a hoy para evaluación de Backlog en memoria
    @Query("SELECT * FROM daily_records WHERE date < :todayIsoDate AND loteId IS NULL ORDER BY date DESC, timestamp ASC")
    fun getAllPastRecords(todayIsoDate: String): Flow<List<DailyRecord>>

    // Consultas reactivas para el Dashboard (Solo registros individuales)
    @Query("SELECT COUNT(*) FROM daily_records WHERE date = :date AND loteId IS NULL")
    fun getTodayTotalCount(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_records WHERE date = :date AND estado = 'COMPLETADO' AND loteId IS NULL")
    fun getTodayCompletedCount(date: String): Flow<Int>

    // Producción desglosada por producto para hoy (Completados individualmente)
    @Query("SELECT producto, SUM(CAST(cantidad AS INTEGER)) as total FROM daily_records WHERE date = :todayIsoDate AND estado = 'COMPLETADO' AND loteId IS NULL GROUP BY producto HAVING total > 0 ORDER BY total DESC")
    fun getTodayProductionByProduct(todayIsoDate: String): Flow<List<ProductoCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<DailyRecord>)

    @Update
    suspend fun updateRecord(record: DailyRecord)

    @Delete
    suspend fun deleteRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    // Borrado masivo de registros asociados a un Lote (Manual Cascade)
    @Query("DELETE FROM daily_records WHERE loteId = :loteId")
    suspend fun deleteRecordsByLoteId(loteId: Int)

    @Query("DELETE FROM daily_records WHERE date = :date AND loteId IS NULL")
    suspend fun clearRecordsForDate(date: String)
}
