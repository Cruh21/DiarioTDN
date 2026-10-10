package com.example.diariotdn.util

import android.content.Context
import android.net.Uri
import com.example.diariotdn.data.AppDatabase
import com.example.diariotdn.data.BackupData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportAllData(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lotes = database.loteDao().getAllLotesDirect()
            val records = database.dailyRecordDao().getAllRecordsDirect()
            val options = database.categoryOptionDao().getAllCategoryOptionsDirect()

            val backup = BackupData(
                version = 1,
                exportDate = DateUtils.getTodayIsoDate(),
                lotes = lotes,
                dailyRecords = records,
                categoryOptions = options
            )

            val jsonString = gson.toJson(backup)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo de salida"))

            Result.success(records.size + lotes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportDateData(uri: Uri, targetIsoDate: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val records = database.dailyRecordDao().getRecordsByDateDirect(targetIsoDate)
            val loteIds = records.mapNotNull { it.loteId }.distinct()
            val lotes = database.loteDao().getAllLotesDirect().filter { it.id in loteIds }

            val backup = BackupData(
                version = 1,
                exportDate = targetIsoDate,
                lotes = lotes,
                dailyRecords = records,
                categoryOptions = emptyList()
            )

            val jsonString = gson.toJson(backup)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo de salida"))

            Result.success(records.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("No se pudo leer el archivo seleccionado"))

            val backupData = gson.fromJson(jsonString, BackupData::class.java)
                ?: return@withContext Result.failure(Exception("El archivo JSON no tiene un formato válido"))

            var importedRecordsCount = 0
            var importedLotesCount = 0

            if (backupData.lotes.isNotEmpty()) {
                database.loteDao().insertLotes(backupData.lotes)
                importedLotesCount = backupData.lotes.size
            }

            if (backupData.dailyRecords.isNotEmpty()) {
                database.dailyRecordDao().insertRecords(backupData.dailyRecords)
                importedRecordsCount = backupData.dailyRecords.size
            }

            if (backupData.categoryOptions.isNotEmpty()) {
                database.categoryOptionDao().insertOptions(backupData.categoryOptions)
            }

            Result.success("Importación exitosa: $importedLotesCount Lote(s) y $importedRecordsCount Registro(s) restaurados.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
