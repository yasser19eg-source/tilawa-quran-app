package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyWirdReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val pendingResult = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val prefsRepo = UserPreferencesRepository(appContext)
        val prefs = prefsRepo.preferencesFlow.first()
        if (prefs.wirdReminderEnabled) {
          if (!prefs.isTodayWirdCompleted()) {
            DailyWirdReminderScheduler.showDailyWirdNotification(
              context = appContext,
              goal = prefs.wirdGoal,
            )
          }
          DailyWirdReminderScheduler.scheduleDailyReminder(
            context = appContext,
            hour = prefs.wirdReminderHour,
            minute = prefs.wirdReminderMinute,
          )
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
