package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurriculumCatalog
import com.example.model.DimensionStat
import com.example.model.QuestionItem
import com.example.model.QuestionReviewItem
import com.example.ui.components.PakReiMetricTile
import com.example.ui.components.PakReiSectionHeader
import com.example.ui.components.PakReiSegmentedControl
import com.example.ui.components.PakReiStatusBadge
import com.example.ui.components.PakReiSurfaceCard
import com.example.ui.theme.BkAmber
import com.example.ui.theme.BkRose
import com.example.ui.theme.InfoContainerBg
import com.example.ui.theme.InfoContainerBorder
import com.example.ui.theme.InfoContainerText
import com.example.ui.theme.IntlEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PakReiTryoutTheme
import com.example.ui.theme.RadarFillCyan
import com.example.ui.theme.RadarStrokeCyan
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.ZoneMandiriBg
import com.example.ui.theme.ZoneMandiriBorder
import com.example.ui.theme.ZoneMandiriText
import com.example.ui.theme.ZoneScaffoldBg
import com.example.ui.theme.ZoneScaffoldBorder
import com.example.ui.theme.ZoneScaffoldText
import com.example.ui.theme.ZoneZpdBg
import com.example.ui.theme.ZoneZpdBorder
import com.example.ui.theme.ZoneZpdText
import com.example.ui.viewmodel.TryoutUiState
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticResultScreen(
    uiState: TryoutUiState,
    onStartNewSession: () -> Unit
) {
    BackHandler {
        onStartNewSession()
    }

    val fw = CurriculumCatalog.FRAMEWORKS[uiState.frameworkKey]
    val isBk = uiState.appMode == "bk"

    // Explicit breakdown of Correct, Wrong/Partial, and Unanswered items
    val fullCorrectCount = remember(uiState.resultReviewItems) {
        uiState.resultReviewItems.count { it.fullCorrect }
    }
    val unansweredCount = remember(uiState.resultReviewItems) {
        uiState.resultReviewItems.count { it.userAnswerText == "Belum dijawab" }
    }
    val wrongOrPartialCount = remember(uiState.resultReviewItems, fullCorrectCount, unansweredCount) {
        (uiState.resultReviewItems.size - fullCorrectCount - unansweredCount).coerceAtLeast(0)
    }

    var discussionFilterIndex by remember { mutableIntStateOf(0) }
    val filteredReviewItems = remember(uiState.resultReviewItems, discussionFilterIndex) {
        when (discussionFilterIndex) {
            1 -> uiState.resultReviewItems.filter { !it.fullCorrect }
            2 -> uiState.resultReviewItems.filter { it.fullCorrect }
            else -> uiState.resultReviewItems
        }
    }

    val radarAxes = remember(uiState.resultCogStats, uiState.resultTopicStats, uiState.resultScorePercent) {
        val combined = if (uiState.resultCogStats.size >= 3) {
            uiState.resultCogStats
        } else {
            (uiState.resultCogStats + uiState.resultTopicStats).distinctBy { it.label }
        }
        if (combined.size >= 3) {
            combined.take(6)
        } else {
            listOf(
                DimensionStat("Pemahaman", uiState.resultScorePercent.toDouble(), 100.0),
                DimensionStat("Aplikasi", uiState.resultScorePercent.toDouble(), 100.0),
                DimensionStat("Penalaran", uiState.resultScorePercent.toDouble(), 100.0)
            )
        }
    }

    val zoneMandiriTopics = uiState.resultTopicStats.filter { it.percentage >= 80 }
    val zoneZpdTopics = uiState.resultTopicStats.filter { it.percentage in 50..79 }
    val zoneScaffoldTopics = uiState.resultTopicStats.filter { it.percentage < 50 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. RINGKASAN HASIL EVALUASI & BREAKDOWN AKURASI (Benar, Salah, Kosong, Waktu)
        item {
            PakReiSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PakReiStatusBadge(
                            text = if (isBk) "PROFIL PSIKOPEDAGOGIS BK" else "LAPORAN HASIL EVALUASI",
                            containerColor = InfoContainerBg,
                            contentColor = InfoContainerText,
                            borderColor = InfoContainerBorder
                        )

                        PakReiStatusBadge(
                            text = "Tersimpan di Cloud",
                            icon = Icons.Default.CloudDone,
                            containerColor = ZoneMandiriBg,
                            contentColor = ZoneMandiriText,
                            borderColor = ZoneMandiriBorder
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = uiState.participantName.ifBlank { "Peserta Evaluasi" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.subjectName} • ${fw?.name ?: uiState.frameworkKey} (${uiState.stage})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Main Score & Standardized Scale Summary Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${uiState.resultScorePercent}%",
                                        fontFamily = JetBrainsMonoFamily,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 30.sp
                                    )
                                    Text(
                                        text = if (isBk) "INDEKS PROFIL" else "NILAI AKHIR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = uiState.resultScaleBadge.ifBlank { "Evaluasi Terstandar" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                val logitFormatted = String.format(
                                    Locale.US,
                                    "%s%.2f",
                                    if (uiState.resultLogitIndex >= 0) "+" else "",
                                    uiState.resultLogitIndex
                                )
                                Text(
                                    text = if (isBk) {
                                        "Indeks Keseimbangan: ${uiState.resultAbilityIndex} (Skala 200–800)"
                                    } else {
                                        "Indeks Kemampuan: ${uiState.resultAbilityIndex} • Logit $logitFormatted"
                                    },
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Poin Polytomous: ${uiState.resultEarnedPoints} / ${uiState.resultMaxPoints}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.resultScaleDisclaimer.isNotBlank()) {
                        Text(
                            text = uiState.resultScaleDisclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4-Metric Breakdown Row: Benar, Salah/Parsial, Kosong, Waktu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PakReiMetricTile(
                            label = if (isBk) "Terisi" else "Benar",
                            value = "$fullCorrectCount",
                            supportingText = "Butir Tepat",
                            valueColor = IntlEmerald,
                            containerColor = ZoneMandiriBg,
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = if (isBk) "Refleksi" else "Salah / Parsial",
                            value = "$wrongOrPartialCount",
                            supportingText = "Perlu Evaluasi",
                            valueColor = ZoneScaffoldText,
                            containerColor = ZoneScaffoldBg,
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Kosong",
                            value = "$unansweredCount",
                            supportingText = "Tidak Dijawab",
                            valueColor = ZoneZpdText,
                            containerColor = ZoneZpdBg,
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Waktu",
                            value = "${uiState.resultDurationMinutes}m",
                            supportingText = "Durasi Ujian",
                            valueColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. RADAR PEMETAAN DIMENSI & EVALUASI MENTOR PAK REI
        item {
            PakReiSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PakReiSectionHeader(
                        title = "Radar Pemetaan Dimensi & Evaluasi Pak Rei",
                        subtitle = "Visualisasi keseimbangan kemampuan kognitif dan umpan balik personal"
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AbilityRadarCanvas(
                                stats = radarAxes,
                                gridColor = MaterialTheme.colorScheme.outlineVariant,
                                axisColor = MaterialTheme.colorScheme.outline,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                            )
                        }
                    }

                    // Pak Rei Mentor Commentary Box
                    Surface(
                        color = InfoContainerBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InfoContainerBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = RoyalBlue,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Pak Rei",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBk) "Catatan Konselor Pak Rei" else "Evaluasi Mentor Pak Rei",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InfoContainerText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (uiState.isGeneratingAnalysis) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = RoyalBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${uiState.resultPakReiComment}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InfoContainerText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. PENGUASAAN BERDASARKAN LEVEL KOGNITIF & MATERI
        item {
            PakReiSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isBk) "Distribusi Dimensi Psikopedagogis" else "Penguasaan Berdasarkan Level Kognitif",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.resultCogStats.forEach { stat ->
                        MasteryBarRow(stat = stat, barColor = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = if (isBk) "Aspek & Sub-Domain Asesmen" else "Penguasaan Materi / Domain Indikator",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.resultTopicStats.forEach { stat ->
                        val color = when {
                            stat.percentage >= 80 -> IntlEmerald
                            stat.percentage >= 50 -> BkAmber
                            else -> BkRose
                        }
                        MasteryBarRow(stat = stat, barColor = color)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 4. PETA ZONE OF PROXIMAL DEVELOPMENT (ZPD)
        item {
            PakReiSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "ZPD",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBk) {
                                "Peta Kekuatan & Area Pengembangan Diri"
                            } else {
                                "Peta Zone of Proximal Development (ZPD)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    ZpdZoneCard(
                        title = if (isBk) "Kekuatan Dominan (≥80%)" else "Zona Mandiri / Pengayaan (≥80%)",
                        subtitle = if (isBk) {
                            "Aspek yang sudah menjadi modal kuat dalam keseharian belajar."
                        } else {
                            "Materi telah dikuasai dengan matang; siap diberikan tantangan HOTS/Olimpiade."
                        },
                        items = zoneMandiriTopics,
                        bgColor = ZoneMandiriBg,
                        borderColor = ZoneMandiriBorder,
                        textColor = ZoneMandiriText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ZpdZoneCard(
                        title = if (isBk) "Area Berkembang Adaptif (50–79%)" else "Zona Perkembangan Proksimal / ZPD (50–79%)",
                        subtitle = if (isBk) {
                            "Potensi yang sedang bertumbuh dan dapat dioptimalkan dengan latihan rutin."
                        } else {
                            "Konsep dasar sudah ada namun masih rawan terjebak pengecoh; efektif ditingkatkan dengan latihan terarah."
                        },
                        items = zoneZpdTopics,
                        bgColor = ZoneZpdBg,
                        borderColor = ZoneZpdBorder,
                        textColor = ZoneZpdText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ZpdZoneCard(
                        title = if (isBk) "Area Perhatian Konseling (<50%)" else "Zona Intervensi / Scaffolding (<50%)",
                        subtitle = if (isBk) {
                            "Memerlukan refleksi mendalam atau diskusi bersama Guru BK / Orang Tua."
                        } else {
                            "Membutuhkan penjelasan ulang konsep fundamental (scaffolding) langkah demi langkah."
                        },
                        items = zoneScaffoldTopics,
                        bgColor = ZoneScaffoldBg,
                        borderColor = ZoneScaffoldBorder,
                        textColor = ZoneScaffoldText
                    )
                }
            }
        }

        // 5. REKOMENDASI REMEDIASI & BEDAH KONSEP PAK REI
        if (uiState.resultSummaries.isNotEmpty()) {
            item {
                PakReiSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isBk) {
                                "Rekomendasi Pengembangan & Peminatan Pak Rei"
                            } else {
                                "Rekomendasi Remediasi & Bedah Konsep Pak Rei"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        uiState.resultSummaries.forEachIndexed { idx, summary ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${idx + 1}. ${summary.topic}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (idx < uiState.resultSummaries.size - 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        // 6. PEMBAHASAN & ANALISIS BUTIR SOAL (DENGAN FILTER CEPAT)
        item {
            PakReiSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PakReiSectionHeader(
                        title = "Pembahasan & Analisis Butir (${uiState.resultReviewItems.size} Soal)",
                        subtitle = "Ketuk butir soal untuk membandingkan jawaban Anda dengan kunci dan membaca penjelasan lengkap"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isBk) {
                        PakReiSegmentedControl(
                            options = listOf(
                                "Semua (${uiState.resultReviewItems.size})",
                                "Perlu Evaluasi (${wrongOrPartialCount + unansweredCount})",
                                "Benar ($fullCorrectCount)"
                            ),
                            selectedIndex = discussionFilterIndex,
                            onSelect = { discussionFilterIndex = it }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    filteredReviewItems.forEach { reviewItem ->
                        ExpandableReviewItemCard(
                            item = reviewItem,
                            isBk = isBk
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 7. ACTION BUTTON: MULAI EVALUASI BARU
        item {
            Button(
                onClick = onStartNewSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .height(54.dp)
                    .testTag("start_new_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Mulai Evaluasi Baru"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kembali ke Beranda / Mulai Evaluasi Baru",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AbilityRadarCanvas(
    stats: List<DimensionStat>,
    modifier: Modifier = Modifier,
    gridColor: Color = Color(0xFFCBD5E1),
    axisColor: Color = Color(0xFF94A3B8),
    labelColor: Color = Color(0xFF0F172A)
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val count = stats.size
        if (count < 3) return@Canvas

        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = min(size.width, size.height) * 0.34f
        val stepAngle = (2.0 * PI / count.toDouble()).toFloat()

        val levels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        levels.forEach { level ->
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = -PI.toFloat() / 2f + i * stepAngle
                val x = centerX + radius * level * cos(angle)
                val y = centerY + radius * level * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(
                path = ringPath,
                color = gridColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        for (i in 0 until count) {
            val angle = -PI.toFloat() / 2f + i * stepAngle
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)
            drawLine(
                color = axisColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )

            val rawLabel = stats[i].label
            val shortLabel = if (rawLabel.length > 16) rawLabel.take(14) + ".." else rawLabel
            val labelText = "$shortLabel (${stats[i].percentage}%)"
            val measured = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            val labelRadius = radius * 1.22f
            val lx = (centerX + labelRadius * cos(angle) - measured.size.width / 2f)
                .coerceIn(4f, (size.width - measured.size.width - 4f).coerceAtLeast(4f))
            val ly = (centerY + labelRadius * sin(angle) - measured.size.height / 2f)
                .coerceIn(2f, (size.height - measured.size.height - 2f).coerceAtLeast(2f))

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(lx, ly)
            )
        }

        val dataPath = Path()
        val vertices = mutableListOf<Offset>()
        for (i in 0 until count) {
            val angle = -PI.toFloat() / 2f + i * stepAngle
            val ratio = (stats[i].percentage / 100f).coerceIn(0.08f, 1.0f)
            val vx = centerX + radius * ratio * cos(angle)
            val vy = centerY + radius * ratio * sin(angle)
            vertices.add(Offset(vx, vy))
            if (i == 0) dataPath.moveTo(vx, vy) else dataPath.lineTo(vx, vy)
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = RadarFillCyan
        )
        drawPath(
            path = dataPath,
            color = RadarStrokeCyan,
            style = Stroke(width = 2.5.dp.toPx())
        )

        vertices.forEach { pt ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = RadarStrokeCyan,
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun MasteryBarRow(
    stat: DimensionStat,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${stat.percentage}%",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = barColor,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZpdZoneCard(
    title: String,
    subtitle: String,
    items: List<DimensionStat>,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = textColor.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(
                    text = "• Tidak ada topik pada kategori ini.",
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { st ->
                        Surface(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Text(
                                text = "${st.label} (${st.percentage}%)",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableReviewItemCard(
    item: QuestionReviewItem,
    isBk: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val isUnanswered = item.userAnswerText == "Belum dijawab"

    val statusBg = when {
        item.fullCorrect -> ZoneMandiriBg
        item.gotScore > 0.0 -> ZoneZpdBg
        else -> ZoneScaffoldBg
    }
    val statusBorder = when {
        item.fullCorrect -> ZoneMandiriBorder
        item.gotScore > 0.0 -> ZoneZpdBorder
        else -> ZoneScaffoldBorder
    }
    val statusTextCol = when {
        item.fullCorrect -> ZoneMandiriText
        item.gotScore > 0.0 -> ZoneZpdText
        else -> ZoneScaffoldText
    }
    val statusIcon = when {
        item.fullCorrect -> Icons.Default.CheckCircle
        isUnanswered -> Icons.AutoMirrored.Filled.HelpOutline
        else -> Icons.Default.Cancel
    }
    val statusLabel = when {
        isBk -> "Skor ${item.gotScore.toInt()}"
        item.fullCorrect -> "Benar (${item.gotScore}/${item.maxScore})"
        item.gotScore > 0.0 -> "Parsial (${item.gotScore}/${item.maxScore})"
        isUnanswered -> "Kosong (0/${item.maxScore})"
        else -> "Salah (0/${item.maxScore})"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .testTag("review_item_${item.index}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Soal #${item.index + 1} • ${item.question.topic}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.question.cognitiveLevel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                PakReiStatusBadge(
                    text = statusLabel,
                    icon = statusIcon,
                    containerColor = statusBg,
                    contentColor = statusTextCol,
                    borderColor = statusBorder
                )

                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Buka Pembahasan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Question Stem
                    Text(
                        text = item.question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Structured Comparison Boxes: Jawaban Anda vs Kunci Jawaban
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, statusBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "JAWABAN ANDA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusTextCol,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.userAnswerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusTextCol,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            color = ZoneMandiriBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ZoneMandiriBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBk) "INTERPRETASI SKALA" else "KUNCI JAWABAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZoneMandiriText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.correctAnswerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZoneMandiriText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Step-by-Step Explanation Card
                    if (item.question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = InfoContainerBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, InfoContainerBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "PEMBAHASAN PAK REI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InfoContainerText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.question.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InfoContainerText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun DiagnosticResultScreenPreview() {
    PakReiTryoutTheme(darkTheme = false) {
        DiagnosticResultScreen(
            uiState = TryoutUiState(
                participantName = "Nadia Putri",
                subjectName = "Penalaran Umum (PU)",
                resultScorePercent = 82,
                resultScaleBadge = "SNBT UTBK: 685 / 1000 (Sangat Kompetitif)",
                resultAbilityIndex = 685,
                resultLogitIndex = 1.42,
                resultEarnedPoints = 16.4,
                resultMaxPoints = 20.0,
                resultDurationMinutes = 24,
                resultPakReiComment = "Penalaran deduktifmu sudah sangat kuat. Fokuskan latihan berikutnya pada inferensi tabel data majemuk.",
                resultCogStats = listOf(
                    DimensionStat("L1 Pemahaman", 90.0, 100.0),
                    DimensionStat("L2 Aplikasi", 85.0, 100.0),
                    DimensionStat("L3 Penalaran", 72.0, 100.0)
                ),
                resultTopicStats = listOf(
                    DimensionStat("Silogisme Deduktif", 90.0, 100.0),
                    DimensionStat("Analisis Tabel", 68.0, 100.0)
                ),
                resultReviewItems = listOf(
                    QuestionReviewItem(
                        index = 0,
                        question = QuestionItem(
                            topic = "Silogisme Deduktif",
                            cognitiveLevel = "L3 (Penalaran)",
                            text = "Jika semua peserta hadir tepat waktu, maka sesi ujian dimulai pukul 08.00.",
                            explanation = "Gunakan modus tollens (~Q -> ~P)."
                        ),
                        userAnswerText = "B. Peserta yang terlambat menyebabkan ujian tertunda",
                        correctAnswerText = "B. Peserta yang terlambat menyebabkan ujian tertunda",
                        gotScore = 1.0,
                        maxScore = 1.0,
                        fullCorrect = true
                    )
                )
            ),
            onStartNewSession = {}
        )
    }
}
