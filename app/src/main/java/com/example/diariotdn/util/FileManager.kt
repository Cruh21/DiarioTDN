package com.example.diariotdn.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TipoReporte(val folderName: String) {
    DIARIO("Reportes Diarios"),
    SEMANAL("Reportes Semanales"),
    BACKUP("Backups")
}

data class ArchivoReporte(
    val id: Long,
    val nombre: String,
    val uri: Uri,
    val fechaModificacion: Long
)

object FileManager {

    private const val PREFS_NAME = "diario_tdn_saf_prefs"
    private const val KEY_BASE_FOLDER_URI = "key_base_folder_uri"

    /**
     * Persiste la URI del directorio base seleccionado por el usuario mediante SAF (Storage Access Framework)
     */
    fun guardarUriDirectorioBase(context: Context, uriString: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_FOLDER_URI, uriString).apply()
    }

    /**
     * Elimina el permiso y desvincula la URI del directorio base guardado
     */
    fun desvincularUriDirectorioBase(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_BASE_FOLDER_URI, null)
        if (!uriString.isNullOrEmpty()) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(Uri.parse(uriString), flags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        prefs.edit().remove(KEY_BASE_FOLDER_URI).apply()
    }

    /**
     * Obtiene la URI del directorio base validando que los permisos de lectura/escritura sigan vigentes
     */
    fun obtenerUriDirectorioBase(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_BASE_FOLDER_URI, null) ?: return null

        val hasPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uriString && it.isWritePermission
        }
        return if (hasPermission) uriString else null
    }

    /**
     * Retorna el directorio base /Documents/Diario TDN y crea las subcarpetas necesarias
     */
    fun getReportFolder(tipo: TipoReporte): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val baseDir = File(documentsDir, "Diario TDN")
        val targetDir = File(baseDir, tipo.folderName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        return targetDir
    }

    /**
     * Guarda permanentemente el Bitmap en el almacenamiento de Android usando MediaStore
     */
    fun guardarImagen(
        context: Context,
        bitmap: Bitmap,
        tipo: TipoReporte,
        nombreArchivo: String
    ): Uri? {
        val cleanFileName = if (nombreArchivo.endsWith(".png", ignoreCase = true)) {
            nombreArchivo
        } else {
            "$nombreArchivo.png"
        }.replace("/", "-").replace("\\", "-").replace(":", "-")

        val relativePath = Environment.DIRECTORY_DOCUMENTS + "/Diario TDN/" + tipo.folderName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al guardar en MediaStore: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                null
            }
        } else {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = File(documentsDir, "Diario TDN/${tipo.folderName}")
                if (!targetDir.exists()) targetDir.mkdirs()

                val targetFile = File(targetDir, cleanFileName)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, targetFile)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al guardar imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                null
            }
        }
    }

    /**
     * Guarda el Bitmap en la carpeta caché privada de la app para compartir de forma temporal
     */
    fun guardarEnCacheParaCompartir(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val tempFile = File(reportsDir, "reporte_temporal_compartir.png")

            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar imagen temporal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Consulta y retorna la lista de archivos de reportes/backups guardados usando DocumentFile (SAF)
     * creando automáticamente la subcarpeta si no existe, con fallback a escaneo directo en disco
     */
    fun obtenerReportesGuardados(context: Context, tipo: TipoReporte): List<ArchivoReporte> {
        val uriBaseString = obtenerUriDirectorioBase(context)
        val lista = mutableListOf<ArchivoReporte>()

        if (!uriBaseString.isNullOrEmpty()) {
            try {
                val treeUri = Uri.parse(uriBaseString)
                val carpetaRaiz = DocumentFile.fromTreeUri(context, treeUri)
                val nombreSubcarpeta = when (tipo) {
                    TipoReporte.DIARIO -> "Reportes Diarios"
                    TipoReporte.SEMANAL -> "Reportes Semanales"
                    TipoReporte.BACKUP -> "Backups"
                }

                var subCarpeta = carpetaRaiz?.findFile(nombreSubcarpeta)
                if (subCarpeta == null && carpetaRaiz != null && carpetaRaiz.exists()) {
                    subCarpeta = carpetaRaiz.createDirectory(nombreSubcarpeta)
                }

                subCarpeta?.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val name = file.name ?: ""
                        if (name.endsWith(".png", ignoreCase = true) || name.endsWith(".json", ignoreCase = true)) {
                            lista.add(
                                ArchivoReporte(
                                    id = file.uri.hashCode().toLong(),
                                    nombre = name,
                                    uri = file.uri,
                                    fechaModificacion = file.lastModified()
                                )
                            )
                        }
                    }
                }
                if (lista.isNotEmpty()) {
                    return lista.sortedByDescending { it.fechaModificacion }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: Escaneo físico directo con java.io.File
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val baseDir = File(documentsDir, "Diario TDN")
            val targetDir = File(baseDir, tipo.folderName)

            if (targetDir.exists() && targetDir.isDirectory) {
                targetDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.equals("png", ignoreCase = true) || file.extension.equals("json", ignoreCase = true))) {
                        val authority = "${context.packageName}.fileprovider"
                        val contentUri = FileProvider.getUriForFile(context, authority, file)
                        lista.add(
                            ArchivoReporte(
                                id = file.hashCode().toLong(),
                                nombre = file.name,
                                uri = contentUri,
                                fechaModificacion = file.lastModified()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return lista.sortedByDescending { it.fechaModificacion }
    }

    /**
     * Elimina un archivo mediante SAF DocumentFile (100% Infalible si la carpeta está vinculada)
     * con fallback a java.io.File y MediaStore
     */
    fun eliminarReporteFisico(context: Context, tipo: TipoReporte, nombreArchivo: String): Boolean {
        val uriBaseString = obtenerUriDirectorioBase(context)

        // Intento 1: SAF DocumentFile (Infalible para carpetas vinculadas)
        if (!uriBaseString.isNullOrEmpty()) {
            try {
                val treeUri = Uri.parse(uriBaseString)
                val carpetaRaiz = DocumentFile.fromTreeUri(context, treeUri)
                val nombreSubcarpeta = when (tipo) {
                    TipoReporte.DIARIO -> "Reportes Diarios"
                    TipoReporte.SEMANAL -> "Reportes Semanales"
                    TipoReporte.BACKUP -> "Backups"
                }

                var subCarpeta = carpetaRaiz?.findFile(nombreSubcarpeta)
                if (subCarpeta == null && carpetaRaiz != null && carpetaRaiz.exists()) {
                    subCarpeta = carpetaRaiz.createDirectory(nombreSubcarpeta)
                }

                val archivo = subCarpeta?.findFile(nombreArchivo)

                if (archivo != null && archivo.exists()) {
                    val deleted = archivo.delete()
                    if (deleted) return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Intento 2: Borrado directo con java.io.File
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val baseDir = File(documentsDir, "Diario TDN")
        val targetDir = File(baseDir, tipo.folderName)
        val archivo = File(targetDir, nombreArchivo)

        if (archivo.exists() && archivo.delete()) {
            return true
        }

        // Intento 3: Borrado mediante MediaStore
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(nombreArchivo, "%Diario TDN%")

        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val deleteUri = ContentUris.withAppendedId(uri, id)
                    val deletedRows = context.contentResolver.delete(deleteUri, null, null)
                    return deletedRows > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Elimina un archivo de reporte guardado en la base de datos de MediaStore / disco
     */
    fun eliminarReporte(context: Context, uri: Uri): Boolean {
        return try {
            val deletedRows = context.contentResolver.delete(uri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Abre un archivo PNG con una aplicación del sistema (Viewer)
     */
    fun abrirArchivo(context: Context, uri: Uri) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(viewIntent, "Abrir Reporte")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al abrir el archivo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Comparte un archivo PNG directamente a otras aplicaciones
     */
    fun compartirArchivo(
        context: Context,
        uri: Uri,
        tituloAction: String = "Reporte de Producción",
        textoPersonalizado: String? = null
    ) {
        try {
            val textoCompartir = if (!textoPersonalizado.isNullOrBlank()) {
                textoPersonalizado
            } else {
                val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale("es", "ES"))
                val fechaFormateada = sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                "Reporte $fechaFormateada"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, tituloAction)
                putExtra(Intent.EXTRA_TEXT, textoCompartir)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir $tituloAction")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Genera el nombre de archivo formateado con guiones y día de la semana para Reporte Diario
     * Ejemplo: "Lunes 24-02-2025.png"
     */
    fun buildDailyFileName(isoDate: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInput.parse(isoDate) ?: Date()

            val dayOfWeekName = SimpleDateFormat("EEEE", Locale("es", "ES")).format(date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val sdfFormattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(date)

            "$dayOfWeekName $sdfFormattedDate.png"
        } catch (e: Exception) {
            "Reporte_Diario_${isoDate.replace("-", "_")}.png"
        }
    }

    /**
     * Genera el nombre de archivo formateado con guiones para Reporte Semanal
     * Ejemplo: "Semana 24-02 al 02-03-2025.png"
     */
    fun buildWeeklyFileName(startIso: String, endIso: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDate = sdfInput.parse(startIso) ?: Date()
            val endDate = sdfInput.parse(endIso) ?: Date()

            val sdfStartDisplay = SimpleDateFormat("dd-MM", Locale.US).format(startDate)
            val sdfEndDisplay = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(endDate)

            "Semana $sdfStartDisplay al $sdfEndDisplay.png"
        } catch (e: Exception) {
            "Reporte_Semanal_${startIso}_al_${endIso}.png"
        }
    }

    /**
     * Genera el nombre estandarizado para Copias de Seguridad (Backups JSON)
     * Formato: "Backup [Full|Dia] [DíaDeSemana] [dd-MM-yyyy].json"
     * Ejemplo Full: "Backup Full Sábado 10-10-2026.json"
     * Ejemplo Día: "Backup Dia Sábado 10-10-2026.json"
     */
    fun buildBackupFileName(isFullBackup: Boolean, isoDate: String = DateUtils.getTodayIsoDate()): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInput.parse(isoDate) ?: Date()

            val dayOfWeekName = SimpleDateFormat("EEEE", Locale("es", "ES")).format(date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val sdfFormattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(date)
            val tipoTag = if (isFullBackup) "Full" else "Dia"

            "Backup $tipoTag $dayOfWeekName $sdfFormattedDate.json"
        } catch (e: Exception) {
            val tipoTag = if (isFullBackup) "Full" else "Dia"
            "Backup_${tipoTag}_${isoDate.replace("-", "_")}.json"
        }
    }

    /**
     * Exporta un string JSON directamente a Documents/Diario TDN/Backups/ usando MediaStore (API 29+)
     */
    fun guardarBackupJson(
        context: Context,
        jsonString: String,
        isFullBackup: Boolean,
        isoDate: String = DateUtils.getTodayIsoDate()
    ): Uri? {
        val fileName = buildBackupFileName(isFullBackup, isoDate)
        val relativePath = Environment.DIRECTORY_DOCUMENTS + "/Diario TDN/" + TipoReporte.BACKUP.folderName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonString.toByteArray(Charsets.UTF_8))
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al guardar backup en MediaStore: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                null
            }
        } else {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = File(documentsDir, "Diario TDN/${TipoReporte.BACKUP.folderName}")
                if (!targetDir.exists()) targetDir.mkdirs()

                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }

                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, targetFile)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al guardar backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                null
            }
        }
    }

    /**
     * Lanza el selector de aplicaciones para visualizar o compartir la Uri directamente
     */
    fun abrirOCompartirImagen(context: Context, contentUri: Uri, tituloAction: String) {
        compartirArchivo(context, contentUri, tituloAction)
    }
}
