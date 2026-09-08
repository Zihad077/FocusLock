package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.AppRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter

object DataExportHelper {
    suspend fun exportDataAsCsv(context: Context, repository: AppRepository) {
        val usageData = repository.getAllUsage().first()
        val focusSessions = repository.allFocusSessions.first()
        
        val file = File(context.cacheDir, "focuslock_export.csv")
        FileWriter(file).use { writer ->
            writer.append("Type,Package/Mode,Date/Start,DurationMinutes\n")
            
            // Export Usage
            usageData.forEach { usage ->
                writer.append("Usage,${usage.packageName},${usage.dateString},${usage.usedMinutes}\n")
            }
            
            // Export Sessions
            focusSessions.forEach { session ->
                writer.append("FocusSession,${session.mode},${session.startTime},${session.durationMinutes}\n")
            }
        }
        
        shareFile(context, file, "text/csv")
    }

    suspend fun exportDataAsJson(context: Context, repository: AppRepository) {
        val usageData = repository.getAllUsage().first()
        val file = File(context.cacheDir, "focuslock_export.json")
        FileWriter(file).use { writer ->
            writer.append("[\n")
            usageData.forEachIndexed { index, usage ->
                writer.append("  {\"package\": \"${usage.packageName}\", \"date\": \"${usage.dateString}\", \"minutes\": ${usage.usedMinutes}}")
                if (index < usageData.size - 1) writer.append(",\n") else writer.append("\n")
            }
            writer.append("]\n")
        }
        
        shareFile(context, file, "application/json")
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Data"))
    }
}
