package com.monetai.sdk.utils

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import java.util.*

/**
 * DateTime helper class for ISO 8601 parsing and formatting
 * Uses ThreeTenABP library for Android API level 21+ compatibility
 */
object DateTimeHelper {
    
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    
    /**
     * Format Date to ISO 8601 string with local timezone
     * @param date Date to format
     * @return ISO 8601 formatted string
     */
    fun formatToISO8601(date: Date): String {
        val instant = Instant.ofEpochMilli(date.time)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        return isoFormatter.format(zonedDateTime)
    }
    
    /**
     * Parse ISO 8601 string to Date
     * @param iso8601String ISO 8601 formatted string
     * @return Date object or null if parsing fails
     */
    fun parseISO8601(iso8601String: String?): Date? {
        if (iso8601String.isNullOrBlank()) return null
        
        return try {
            val instant = Instant.parse(iso8601String)
            Date(instant.toEpochMilli())
        } catch (e: DateTimeParseException) {
            // Fallback for different ISO formats
            try {
                val instant = Instant.parse(iso8601String + "Z")
                Date(instant.toEpochMilli())
            } catch (e2: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
} 