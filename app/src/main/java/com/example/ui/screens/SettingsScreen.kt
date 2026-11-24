package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.data.UserPreferencesState
import com.example.model.DailyWirdGoal
import com.example.model.FontScaleOption
import com.example.model.SleepTimerOption
import com.example.notifications.DailyWirdReminderScheduler
import com.example.ui.theme.AmiriFontFamily

private data class WirdTimePreset(val labelAr: String, val hour: Int, val minute: Int)

private val WIRD_TIME_PRESETS =
  listOf(
    WirdTimePreset("الفجر", 5, 0),
    WirdTimePreset("الضحى", 8, 30),
    WirdTimePreset("العصر", 16, 0),
    WirdTimePreset("المغرب", 18, 30),
    WirdTimePreset("قبل النوم", 22, 0),
  )

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  preferences: UserPreferencesState,
  activeSleepTimer: SleepTimerOption,
  onBackClick: () -> Unit,
  onToggleDarkMode: (Boolean) -> Unit,
  onSelectFontScale: (FontScaleOption) -> Unit,
  onToggleAutoPlayNext: (Boolean) -> Unit,
  onSelectSleepTimer: (SleepTimerOption) -> Unit,
  onToggleWirdReminder: (Boolean) -> Unit,
  onSelectWirdTime: (Int, Int) -> Unit,
  onSelectWirdGoal: (DailyWirdGoal) -> Unit,
  onSendTestWirdNotification: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  var showTimePicker by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 760.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onBackClick,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.testTag("settings_previous_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "السابق",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("السابق")
        }

        Text(
          text = "إعدادات التطبيق",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // Theme Toggle Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (preferences.isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (preferences.isDarkMode) "الوضع الداكن" else "الوضع الفاتح",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "التبديل بين المظهر الداكن المريح والمظهر النهاري الفاتح",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Switch(
            checked = !preferences.isDarkMode,
            onCheckedChange = { lightEnabled -> onToggleDarkMode(!lightEnabled) },
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.testTag("light_mode_switch"),
          )
        }
      }

      // Font Size Scaling Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.FormatSize,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "حجم الخط",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "التحكم في حجم النصوص وأسماء السور والأدعية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FontScaleOption.entries.forEach { option ->
              FilterChip(
                selected = preferences.fontScaleOption == option,
                onClick = { onSelectFontScale(option) },
                label = { Text(option.labelAr) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                  ),
                modifier = Modifier.testTag("font_scale_${option.name}"),
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = AmiriFontFamily),
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "معاينة فورية لحجم الخط في القوائم وأسماء السور.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      // Playback Preferences Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "الانتقال التلقائي للسورة التالية",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "تشغيل السورة التالية تلقائياً عند انتهاء التلاوة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = preferences.autoPlayNext,
              onCheckedChange = onToggleAutoPlayNext,
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                  checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
              modifier = Modifier.testTag("settings_autoplay_switch"),
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "مؤقت النوم",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SleepTimerOption.entries.forEach { option ->
                FilterChip(
                  selected = activeSleepTimer == option,
                  onClick = { onSelectSleepTimer(option) },
                  label = { Text(option.labelAr) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                  modifier = Modifier.testTag("settings_sleep_${option.name}"),
                )
              }
            }
          }
        }
      }

      // Daily Quran Portion Reminder Card
      Card(
        modifier = Modifier.fillMaxWidth().testTag("wird_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "تذكير الورد اليومي",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "تنبيه يومي يذكّرك بقراءة أو سماع وردك من القرآن الكريم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = preferences.wirdReminderEnabled,
              onCheckedChange = onToggleWirdReminder,
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                  checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
              modifier = Modifier.testTag("wird_reminder_switch"),
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "مقدار الورد اليومي",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              DailyWirdGoal.entries.forEach { goal ->
                FilterChip(
                  selected = preferences.wirdGoal == goal,
                  onClick = { onSelectWirdGoal(goal) },
                  label = { Text(goal.labelAr) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                  modifier = Modifier.testTag("wird_goal_${goal.name}"),
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Column {
                    Text(
                      text = "وقت التنبيه اليومي",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text =
                        DailyWirdReminderScheduler.formatArabicTime(
                          preferences.wirdReminderHour,
                          preferences.wirdReminderMinute,
                        ),
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                OutlinedButton(
                  onClick = { showTimePicker = true },
                  shape = RoundedCornerShape(50),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("wird_change_time_button"),
                ) {
                  Text("تغيير الوقت")
                }
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                WIRD_TIME_PRESETS.forEach { preset ->
                  val isSelected =
                    preferences.wirdReminderHour == preset.hour &&
                      preferences.wirdReminderMinute == preset.minute
                  FilterChip(
                    selected = isSelected,
                    onClick = { onSelectWirdTime(preset.hour, preset.minute) },
                    label = {
                      Text(
                        "${preset.labelAr} (${DailyWirdReminderScheduler.formatArabicTime(preset.hour, preset.minute)})"
                      )
                    },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                      ),
                    modifier =
                      Modifier.testTag("wird_preset_${preset.hour}_${preset.minute}"),
                  )
                }
              }
            }
          }

          OutlinedButton(
            onClick = onSendTestWirdNotification,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth().testTag("wird_test_notification_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("إرسال إشعار تجريبي للورد اليومي")
          }
        }
      }

      // Dedication Footer
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "تلاوة",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "صدقة جارية عن روح المرحوم ${QuranCatalog.DEDICATED_PERSON_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = preferences.wirdReminderHour,
        initialMinute = preferences.wirdReminderMinute,
        is24Hour = false,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = {
        Text(
          text = "تحديد وقت الورد اليومي",
          style = MaterialTheme.typography.titleLarge,
        )
      },
      text = {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(state = timePickerState)
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            onSelectWirdTime(timePickerState.hour, timePickerState.minute)
            showTimePicker = false
          },
          modifier = Modifier.testTag("wird_time_picker_confirm"),
        ) {
          Text("حفظ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text("إلغاء") }
      },
    )
  }
}
