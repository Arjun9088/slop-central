package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.db.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val migration1to2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN modifiedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker.db"
        )
            .addMigrations(migration1to2)
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
}
