package com.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.expensetracker.R
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class QuickEntryWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var expenseDao: ExpenseDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, QuickEntryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.quick_entry_widget)

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val totalToday = try {
            runBlocking(Dispatchers.IO) {
                expenseDao.getTotalInRange(today, today) ?: 0.0
            }
        } catch (_: Exception) {
            0.0
        }
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        views.setTextViewText(R.id.today_total, "Today: ${formatter.format(totalToday)}")

        setCategoryPendingIntent(context, views, R.id.btn_food, "Food and drink", 0)
        setCategoryPendingIntent(context, views, R.id.btn_transport, "Transportation", 1)
        setCategoryPendingIntent(context, views, R.id.btn_shopping, "Shopping", 2)
        setCategoryPendingIntent(context, views, R.id.btn_other, "Other", 3)

        val appIntent = PendingIntent.getActivity(
            context, 100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.app_icon, appIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setCategoryPendingIntent(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        category: String,
        requestCode: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_CATEGORY, category)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    companion object {
        const val EXTRA_WIDGET_CATEGORY = "com.expensetracker.widget.CATEGORY"

        fun updateWidgets(context: Context) {
            val intent = Intent(context, QuickEntryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
