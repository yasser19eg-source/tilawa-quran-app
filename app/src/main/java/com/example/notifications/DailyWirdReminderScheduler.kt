package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.DailyWirdGoal
import java.util.Calendar
import java.util.Locale

object DailyWirdReminderScheduler {

  const val DAILY_WIRD_CHANNEL_ID = "daily_wird_reminder_channel"
  const val DAILY_WIRD_NOTIFICATION_ID = 4102
  private const val DAILY_WIRD_ALARM_REQUEST_CODE = 4103

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = manager.getNotificationChannel(DAILY_WIRD_CHANNEL_ID)
      if (existing != null) return

      val channel =
        NotificationChannel(
            DAILY_WIRD_CHANNEL_ID,
            context.getString(R.string.daily_wird_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
          )
          .apply {
            description = "تنبيه يومي للمواظبة على ورد القرآن الكريم في تطبيق تلاوة"
          }
      manager.createNotificationChannel(channel)
    }
  }

  fun computeNextTriggerTimeMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val cal =
      Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, safeHour)
        set(Calendar.MINUTE, safeMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= nowMillis) {
          add(Calendar.DAY_OF_MONTH, 1)
        }
      }
    return cal.timeInMillis
  }

  fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val appContext = context.applicationContext
    ensureNotificationChannel(appContext)
    val alarmManager =
      appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildAlarmPendingIntent(appContext)
    val triggerAtMillis = computeNextTriggerTimeMillis(hour, minute)
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  }

  fun cancelDailyReminder(context: Context) {
    val appContext = context.applicationContext
    val alarmManager =
      appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildAlarmPendingIntent(appContext)
    alarmManager.cancel(pendingIntent)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showDailyWirdNotification(
    context: Context,
    goal: DailyWirdGoal,
    isTest: Boolean = false,
  ): Boolean {
    val appContext = context.applicationContext
    if (!hasNotificationPermission(appContext)) {
      return false
    }
    ensureNotificationChannel(appContext)

    val openAppIntent =
      Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val contentPendingIntent =
      PendingIntent.getActivity(
        appContext,
        DAILY_WIRD_NOTIFICATION_ID,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title =
      if (isTest) "معاينة تذكير الورد اليومي • تلاوة"
      else "وردك اليومي من القرآن الكريم • تلاوة"
    val message =
      "حان موعد وردك اليومي (${goal.labelAr}). لا تنس نصيبك اليوم من كتاب الله، جعل الله تلاوتك نوراً وبركة."

    val notification =
      NotificationCompat.Builder(appContext, DAILY_WIRD_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_quran)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .build()

    NotificationManagerCompat.from(appContext).notify(DAILY_WIRD_NOTIFICATION_ID, notification)
    return true
  }

  fun formatArabicTime(hour: Int, minute: Int): String {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val period = if (safeHour < 12) "ص" else "م"
    val displayHour =
      when (val h12 = safeHour % 12) {
        0 -> 12
        else -> h12
      }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, safeMinute, period)
  }

  private fun buildAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DailyWirdReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      DAILY_WIRD_ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}

class DailyWirdReminderManager(private val context: Context) {
  private val appContext = context.applicationContext

  fun schedule(hour: Int, minute: Int) {
    DailyWirdReminderScheduler.scheduleDailyReminder(appContext, hour, minute)
  }

  fun cancel() {
    DailyWirdReminderScheduler.cancelDailyReminder(appContext)
  }

  fun hasNotificationPermission(): Boolean =
    DailyWirdReminderScheduler.hasNotificationPermission(appContext)

  fun showTestNotification(goal: DailyWirdGoal): Boolean =
    DailyWirdReminderScheduler.showDailyWirdNotification(
      context = appContext,
      goal = goal,
      isTest = true,
    )
}
