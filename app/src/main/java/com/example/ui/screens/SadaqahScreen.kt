package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.QuranCatalog
import com.example.model.MemorialDua
import com.example.ui.theme.AmiriFontFamily

@Composable
fun SadaqahScreen(
  duaCounter: Int,
  onIncrementDuaCounter: () -> Unit,
  onShowFeedback: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  val shareText: (String) -> Unit = { text ->
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
      }
    val chooser = Intent.createChooser(sendIntent, "مشاركة الدعاء")
    context.startActivity(chooser)
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Memorial Dedication Header Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("sadaqah_memorial_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
              Image(
                painter = painterResource(id = R.drawable.img_sadaqah_ornament_1795533463416),
                contentDescription = "زخرفة إسلامية لبطاقة الصدقة الجارية",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        colors =
                          listOf(
                            Color(0x66121413),
                            MaterialTheme.colorScheme.surface,
                          )
                      )
                    )
              )

              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.Center),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "صدقة جارية عن روح المرحوم بإذن الله",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = QuranCatalog.DEDICATED_PERSON_NAME,
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = AmiriFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
              )

              Text(
                text =
                  "نرجو من كل من ينتفع بتلاوات هذا التطبيق أو يستمع لإذاعة القرآن الكريم أن يخصه بدعوة صالحة في ظهر الغيب.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Button(
                  onClick = {
                    onIncrementDuaCounter()
                    onShowFeedback("تقبل الله دعاءك وغفر للمرحوم عبد العظيم رجب زكي")
                  },
                  shape = RoundedCornerShape(50),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  modifier = Modifier.testTag("amen_dua_counter_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "آمين • سجّل دعوة ($duaCounter)",
                    fontWeight = FontWeight.Bold,
                  )
                }

                OutlinedButton(
                  onClick = {
                    val primaryDua = QuranCatalog.memorialDuas.first().textAr
                    shareText(
                      "صدقة جارية عن المرحوم ${QuranCatalog.DEDICATED_PERSON_NAME}\n\n$primaryDua\n\nتطبيق تلاوة"
                    )
                  },
                  shape = RoundedCornerShape(50),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag("share_sadaqah_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("مشاركة")
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
            }
          }
        }
      }

      item {
        Text(
          text = "أدعية مختارة للمتوفى",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      items(QuranCatalog.memorialDuas, key = { it.id }) { dua ->
        MemorialDuaCard(
          dua = dua,
          onCopy = {
            clipboardManager.setText(AnnotatedString(dua.textAr))
            onShowFeedback("تم نسخ الدعاء")
          },
          onShare = {
            shareText(
              "دعاء للمرحوم ${QuranCatalog.DEDICATED_PERSON_NAME}:\n\n${dua.textAr}\n(${dua.sourceAr})"
            )
          },
        )
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun MemorialDuaCard(
  dua: MemorialDua,
  onCopy: () -> Unit,
  onShare: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("dua_card_${dua.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = dua.titleAr,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = dua.sourceAr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = dua.textAr,
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Normal,
          ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onCopy,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_dua_${dua.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "نسخ الدعاء",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onShare,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("share_dua_${dua.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = "مشاركة الدعاء",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}
