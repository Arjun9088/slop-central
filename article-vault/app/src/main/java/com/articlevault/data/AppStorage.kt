package com.articlevault.data

import android.content.Context
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ROOT_DIR = "ArticleVault"
        private const val DB_NAME = "article_vault.db"
        private const val ARTICLES_DIR = "articles"
    }

    val storageRoot: File = File(Environment.getExternalStorageDirectory(), ROOT_DIR)
    val dbFile: File = File(storageRoot, DB_NAME)
    val articlesDir: File = File(storageRoot, ARTICLES_DIR)

    private val oldDbFile: File = File(context.getExternalFilesDir(null), DB_NAME)
    private val oldArticlesDir: File = File(context.filesDir, ARTICLES_DIR)

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val check = context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            check == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasExistingData(): Boolean {
        return dbFile.exists() && dbFile.length() > 0
    }

    fun hasOldLocationData(): Boolean {
        val hasOldDb = oldDbFile.exists() && oldDbFile.length() > 0
        val hasOldArticles = oldArticlesDir.exists() && (oldArticlesDir.listFiles()?.isNotEmpty() == true)
        return hasOldDb || hasOldArticles
    }

    fun initializeStorage(): Boolean {
        return try {
            if (!storageRoot.exists()) storageRoot.mkdirs()
            if (!articlesDir.exists()) articlesDir.mkdirs()
            storageRoot.exists() && articlesDir.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun migrateFromOldLocation(): Boolean {
        return try {
            initializeStorage()

            if (oldDbFile.exists() && !dbFile.exists()) {
                oldDbFile.copyTo(dbFile, overwrite = false)
                val wal = File(oldDbFile.parent, "$DB_NAME-wal")
                val shm = File(oldDbFile.parent, "$DB_NAME-shm")
                if (wal.exists()) wal.copyTo(File(storageRoot, "$DB_NAME-wal"), overwrite = false)
                if (shm.exists()) shm.copyTo(File(storageRoot, "$DB_NAME-shm"), overwrite = false)
            }

            if (oldArticlesDir.exists()) {
                oldArticlesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "html") {
                        val target = File(articlesDir, file.name)
                        if (!target.exists()) {
                            file.copyTo(target, overwrite = false)
                        }
                    }
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
