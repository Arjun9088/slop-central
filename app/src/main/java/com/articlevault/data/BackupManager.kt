package com.articlevault.data

import android.content.Context
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BackupManager"
        private const val DB_NAME = "article_vault.db"
        private const val DB_WAL = "article_vault.db-wal"
        private const val DB_SHM = "article_vault.db-shm"
    }

    private val dbFile = File(context.getExternalFilesDir(null), DB_NAME)
    private val articlesDir = File(context.filesDir, "articles")

    data class BackupResult(val success: Boolean, val path: String? = null, val error: String? = null)

    suspend fun export(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val zipName = "ArticleVault_backup_$date.zip"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val zipFile = File(downloadsDir, zipName)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // Add database files
                addFileToZip(zos, dbFile, "article_vault.db")
                val wal = File(dbFile.parent, DB_WAL)
                if (wal.exists()) addFileToZip(zos, wal, DB_WAL)
                val shm = File(dbFile.parent, DB_SHM)
                if (shm.exists()) addFileToZip(zos, shm, DB_SHM)

                // Add HTML files
                if (articlesDir.exists()) {
                    articlesDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.extension == "html") {
                            addFileToZip(zos, file, "articles/${file.name}")
                        }
                    }
                }
            }

            Log.d(TAG, "Backup exported to ${zipFile.absolutePath} (${zipFile.length()} bytes)")
            BackupResult(true, zipFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            BackupResult(false, error = "Export failed: ${e.message}")
        }
    }

    suspend fun import(zipFile: File): BackupResult = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.filesDir, "restore_temp")
            tempDir.mkdirs()

            // Extract ZIP
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            zis.copyTo(bos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Replace database
            val restoredDb = File(tempDir, DB_NAME)
            if (!restoredDb.exists()) {
                tempDir.deleteRecursively()
                return@withContext BackupResult(false, error = "No database found in backup.")
            }

            // Close any open database connections by restarting the app
            // Copy database files
            val dbDir = dbFile.parentFile!!
            restoredDb.copyTo(dbFile, true)
            File(tempDir, DB_WAL).let { if (it.exists()) it.copyTo(File(dbDir, DB_WAL), true) }
            File(tempDir, DB_SHM).let { if (it.exists()) it.copyTo(File(dbDir, DB_SHM), true) }

            // Restore HTML files
            val restoredArticles = File(tempDir, "articles")
            if (restoredArticles.exists()) {
                articlesDir.deleteRecursively()
                articlesDir.mkdirs()
                restoredArticles.copyRecursively(articlesDir, true)
            }

            // Cleanup
            tempDir.deleteRecursively()

            Log.d(TAG, "Backup restored from ${zipFile.absolutePath}")
            BackupResult(true)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            BackupResult(false, error = "Import failed: ${e.message}")
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }
}
