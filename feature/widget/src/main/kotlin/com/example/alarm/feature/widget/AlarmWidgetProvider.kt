package com.example.alarm.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.alarm.data.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AlarmWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val alarms = alarmRepository.getEnabled()
            val nextAlarm = alarms.minByOrNull { it.timeMillis }

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, nextAlarm)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AlarmWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.alarm.REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            nextAlarm: com.example.alarm.data.repository.Alarm?
        ) {
            val layoutId = context.resources.getIdentifier("widget_alarm", "layout", context.packageName)
            val views = RemoteViews(context.packageName, layoutId)

            if (nextAlarm != null) {
                val dateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
                val alarmTime = dateFormat.format(Date(nextAlarm.timeMillis))

                val titleId = context.resources.getIdentifier("widget_alarm_title", "id", context.packageName)
                val timeId = context.resources.getIdentifier("widget_alarm_time", "id", context.packageName)
                val noAlarmId = context.resources.getIdentifier("widget_no_alarm", "id", context.packageName)
                val infoId = context.resources.getIdentifier("widget_alarm_info", "id", context.packageName)

                views.setTextViewText(titleId, nextAlarm.title)
                views.setTextViewText(timeId, alarmTime)
                views.setViewVisibility(noAlarmId, android.view.View.GONE)
                views.setViewVisibility(infoId, android.view.View.VISIBLE)
            } else {
                val noAlarmId = context.resources.getIdentifier("widget_no_alarm", "id", context.packageName)
                val infoId = context.resources.getIdentifier("widget_alarm_info", "id", context.packageName)

                views.setViewVisibility(noAlarmId, android.view.View.VISIBLE)
                views.setViewVisibility(infoId, android.view.View.GONE)
            }

            val intent = Intent(context, Class.forName("com.example.alarm.MainActivity"))
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val containerId = context.resources.getIdentifier("widget_container", "id", context.packageName)
            views.setOnClickPendingIntent(containerId, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestWidgetUpdate(context: Context) {
            val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
