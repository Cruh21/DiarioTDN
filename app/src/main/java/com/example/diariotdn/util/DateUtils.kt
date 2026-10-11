package com.example.diariotdn.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    /**
     * Retorna la fecha actual en formato ISO "YYYY-MM-DD" para consultas en la BD.
     */
    fun getTodayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Convierte una fecha en formato "YYYY-MM-DD" a "DD/MM/AA" para mostrar en el registro.
     * Ejemplo: "2026-09-24" -> "24/09/26"
     */
    fun isoToDisplayDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size == 3) {
            val yearFull = parts[0]
            val month = parts[1]
            val day = parts[2]
            val yearTwoDigits = if (yearFull.length == 4) yearFull.substring(2) else yearFull
            return "$day/$month/$yearTwoDigits"
        }
        return isoDate
    }

    /**
     * Convierte los milisegundos seleccionados de un DatePicker (UTC) a formato ISO "YYYY-MM-DD".
     */
    fun millisToIsoDate(millis: Long?): String {
        if (millis == null) return getTodayIsoDate()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = millis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    /**
     * Retorna la fecha de ayer en formato ISO "YYYY-MM-DD".
     */
    fun getYesterdayIsoDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(calendar.time)
    }

    /**
     * Retorna un Pair<String, String> con la fecha de inicio (Lunes) y fin (Domingo)
     * de la semana actual en formato ISO "YYYY-MM-DD".
     */
    fun getCurrentWeekRangeIso(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startIso = sdf.format(calendar.time)

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endIso = sdf.format(calendar.time)

        return Pair(startIso, endIso)
    }
}
