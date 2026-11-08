package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExamResultRecord
import com.example.model.SavedBlueprintRecord
import com.example.ui.components.PakReiEmptyState
import com.example.ui.components.PakReiMetricTile
import com.example.ui.components.PakReiSectionHeader
import com.example.ui.components.PakReiStatusBadge
import com.example.ui.components.PakReiSurfaceCard
import com.example.ui.theme.BkAmber
import com.example.ui.theme.BkRose
import com.example.ui.theme.InfoContainerBg
import com.example.ui.theme.InfoContainerBorder
import com.example.ui.theme.InfoContainerText
import com.example.ui.theme.IntlEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.ZoneMandiriBg
import com.example.ui.theme.ZoneMandiriBorder
import com.example.ui.theme.ZoneMandiriText
import com.example.ui.theme.ZoneScaffoldBg
import com.example.ui.theme.ZoneScaffoldBorder
import com.example.ui.theme.ZoneScaffoldText
import com.example.ui.viewmodel.CloudDataState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CloudHistoryScreen(
    resultsState: CloudDataState<List<ExamResultRecord>>,
    onOpenResult: (ExamResultRecord) -> Unit,
    onDeleteResult: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PakReiStatusBadge(
                        text = "SINKRONISASI CLOUD AKTIF",
                        icon = Icons.Default.CloudDone,
                        containerColor = ZoneMandiriBg,
                        contentColor = ZoneMandiriText,
                        borderColor = ZoneMandiriBorder
                    )
                }

                PakReiSectionHeader(
                    title = "Riwayat Evaluasi & Statistik Perkembangan",
                    subtitle = "Pantau tren Indeks Kemampuan (Logit), capaian skor, dan buka kembali laporan diagnostik kapan saja"
                )
            }
        }

        when (resultsState) {
            is CloudDataState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            is CloudDataState.Error -> {
                item {
                    Surface(
                        color = ZoneScaffoldBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ZoneScaffoldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                    ) {
                        Text(
                            text = resultsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZoneScaffoldText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is CloudDataState.Success -> {
                val list = resultsState.data
                if (list.isEmpty()) {
                    item {
                        PakReiEmptyState(
                            icon = Icons.Default.History,
                            title = "Belum Ada Riwayat Evaluasi di Cloud",
                            description = "Selesaikan sesi tryout pada tab Beranda untuk menyimpan analisis Radar dan perkembangan skor secara otomatis.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                        )
                    }
                } else {
                    val totalSessions = list.size
                    val avgScore = list.map { it.scorePercent }.average().roundToInt()
                    val peakAbility = list.maxOfOrNull { it.abilityIndexScore } ?: 500L

                    // KPI Metrics + Trend Chart Card
                    item {
                        PakReiSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp),
                            contentPadding = PaddingValues(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PakReiMetricTile(
                                        label = "Total Sesi",
                                        value = "$totalSessions",
                                        supportingText = "Tryout Selesai",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PakReiMetricTile(
                                        label = "Rata-Rata",
                                        value = "$avgScore%",
                                        supportingText = "Akurasi Skor",
                                        valueColor = IntlEmerald,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PakReiMetricTile(
                                        label = "Puncak Indeks",
                                        value = "$peakAbility",
                                        supportingText = "Skala 200–800",
                                        valueColor = ReversePurple,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Tren Skor",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Grafik Tren Capaian (${list.take(8).size} Evaluasi Terakhir)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val recentChronological = list.take(8).reversed()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(124.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    recentChronological.forEachIndexed { idx, rec ->
                                        val pct = rec.scorePercent.toInt().coerceIn(8, 100)
                                        val barCol = when {
                                            pct >= 80 -> IntlEmerald
                                            pct >= 55 -> RoyalBlue
                                            else -> BkAmber
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            Text(
                                                text = "$pct%",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .fillMaxHeight(fraction = (pct / 100f) * 0.72f)
                                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                    .background(barCol)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "#${idx + 1}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Individual History Cards
                    items(list, key = { it.id }) { record ->
                        val formattedDate = record.createdAt?.toDate()?.let { dateFormat.format(it) }
                            ?: "Baru saja disinkronkan"
                        val scoreInt = record.scorePercent.toInt()
                        val badgeColor = when {
                            scoreInt >= 80 -> IntlEmerald
                            scoreInt >= 55 -> RoyalBlue
                            else -> BkRose
                        }

                        PakReiSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .testTag("history_card_${record.id}"),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.subjectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${record.frameworkName} • ${record.stage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Peserta: ${record.participantName} • $formattedDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        color = badgeColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${record.scorePercent}%",
                                                fontFamily = JetBrainsMonoFamily,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            )
                                            Text(
                                                text = "Idx ${record.abilityIndexScore}",
                                                fontFamily = JetBrainsMonoFamily,
                                                color = badgeColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                if (record.scaleBadge.isNotBlank()) {
                                    PakReiStatusBadge(
                                        text = record.scaleBadge,
                                        containerColor = ZoneMandiriBg,
                                        contentColor = ZoneMandiriText,
                                        borderColor = ZoneMandiriBorder
                                    )
                                }

                                if (record.pakReiComment.isNotBlank()) {
                                    Text(
                                        text = "\"${record.pakReiComment}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onOpenResult(record) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("open_history_${record.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Analytics,
                                            contentDescription = "Buka Laporan",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Buka Laporan & Pembahasan",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteResult(record.id) },
                                        modifier = Modifier.testTag("delete_history_${record.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Riwayat",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudBlueprintsScreen(
    blueprintsState: CloudDataState<List<SavedBlueprintRecord>>,
    onUseBlueprint: (SavedBlueprintRecord) -> Unit,
    onDeleteBlueprint: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PakReiStatusBadge(
                    text = "REPOSITORI KISI-KISI CLOUD",
                    containerColor = InfoContainerBg,
                    contentColor = InfoContainerText,
                    borderColor = InfoContainerBorder
                )

                PakReiSectionHeader(
                    title = "Koleksi Blueprint Kisi-Kisi PDF Tersimpan",
                    subtitle = "Gunakan kembali matriks indikator dan Style DNA hasil bedah PDF Anda kapan saja tanpa perlu mengunggah ulang dokumen"
                )
            }
        }

        when (blueprintsState) {
            is CloudDataState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ReversePurple)
                    }
                }
            }

            is CloudDataState.Error -> {
                item {
                    Surface(
                        color = ZoneScaffoldBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ZoneScaffoldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                    ) {
                        Text(
                            text = blueprintsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZoneScaffoldText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is CloudDataState.Success -> {
                val list = blueprintsState.data
                if (list.isEmpty()) {
                    item {
                        PakReiEmptyState(
                            icon = Icons.Default.Description,
                            title = "Belum Ada Blueprint PDF di Cloud",
                            description = "Buka mode Reverse Engineering Kisi-Kisi PDF di tab Beranda, bedah kisi-kisi Anda, lalu tekan 'Simpan Cloud'.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                        )
                    }
                } else {
                    items(list, key = { it.id }) { bp ->
                        PakReiSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .testTag("blueprint_card_${bp.id}"),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bp.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${bp.detectedSubject} • ${bp.stage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    PakReiStatusBadge(
                                        text = "${bp.totalItemsCount} Soal",
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = ReversePurple,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Style DNA: ${bp.styleDNA}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onUseBlueprint(bp) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ReversePurple,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("use_blueprint_${bp.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Gunakan Blueprint",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gunakan Blueprint Ini",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteBlueprint(bp.id) },
                                        modifier = Modifier.testTag("delete_blueprint_${bp.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Blueprint",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
