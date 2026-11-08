package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveExamSession
import com.example.model.CurriculumCatalog
import com.example.model.ExamResultRecord
import com.example.model.LoginHistoryRecord
import com.example.model.UserProfile
import com.example.ui.components.PakReiMetricTile
import com.example.ui.components.PakReiSectionHeader
import com.example.ui.components.PakReiStatusBadge
import com.example.ui.components.PakReiSurfaceCard
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.BkRose
import com.example.ui.theme.IntlEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.CloudDataState
import com.example.ui.viewmodel.TryoutUiState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private data class QuickSubjectPreset(
    val label: String,
    val mode: String,
    val frameworkKey: String,
    val badge: String
)

private val QUICK_SUBJECT_PRESETS = listOf(
    QuickSubjectPreset("Matematika Wajib", "nasional", "merdeka", "Merdeka"),
    QuickSubjectPreset("Penalaran Umum", "nasional", "snbt", "SNBT"),
    QuickSubjectPreset("Literasi Membaca", "nasional", "akm", "AKM"),
    QuickSubjectPreset("Scientific Literacy", "internasional", "pisa", "PISA"),
    QuickSubjectPreset("Minat Karir RIASEC", "bk", "bk_riasec", "BK")
)

@Composable
fun ModeLandingScreen(
    uiState: TryoutUiState,
    userProfileState: CloudDataState<UserProfile?>,
    loginHistoryState: CloudDataState<List<LoginHistoryRecord>>,
    activeSessionState: CloudDataState<ActiveExamSession?>,
    examResultsState: CloudDataState<List<ExamResultRecord>> = CloudDataState.Loading,
    onSelectMode: (String) -> Unit,
    onQuickStartSubject: (String, String, String) -> Unit = { mode, _, _ -> onSelectMode(mode) },
    onResumeSession: (ActiveExamSession) -> Unit,
    onDiscardSession: () -> Unit,
    onOpenRecentResult: (ExamResultRecord) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onSaveProfile: (String, String) -> Unit = { _, _ -> },
    onDismissProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSaveSettings: (String, Boolean) -> Unit = { _, _ -> },
    onDismissSettings: () -> Unit = {}
) {
    val profile = (userProfileState as? CloudDataState.Success)?.data
    val displayName = profile?.displayName?.ifBlank { uiState.participantName }
        ?: uiState.participantName.ifBlank { "Peserta" }
    val resolvedNisn = profile?.nisn?.ifBlank { uiState.participantNisn } ?: uiState.participantNisn
    val resolvedSchool = profile?.institution?.ifBlank { uiState.participantSchool } ?: uiState.participantSchool
    val resolvedGrade = profile?.gradeClass?.ifBlank { uiState.participantGrade } ?: uiState.participantGrade
    val resolvedPhoto = profile?.photoBase64?.ifBlank { uiState.participantPhotoBase64 } ?: uiState.participantPhotoBase64

    val authMethodLabel = when (profile?.authMethod) {
        "email" -> "Email"
        "facebook" -> "Facebook"
        else -> "Google"
    }
    val completedResults = (examResultsState as? CloudDataState.Success)?.data.orEmpty()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.forLanguageTag("id-ID")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Clean Greeting & Unified Student Identity Bar
        item {
            PakReiSurfaceCard(
                onClick = onOpenProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("profile_summary_row"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        StudentAvatar(
                            displayName = displayName,
                            photoBase64 = resolvedPhoto,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Halo, $displayName",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subInfo = buildString {
                                if (resolvedGrade.isNotBlank()) {
                                    append(resolvedGrade)
                                    append(" • ")
                                }
                                if (resolvedSchool.isNotBlank()) {
                                    append(resolvedSchool)
                                    append(" • ")
                                }
                                if (resolvedNisn.isNotBlank()) {
                                    append("NISN $resolvedNisn")
                                } else {
                                    append("Akun $authMethodLabel")
                                }
                            }
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Info Akun",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. Active Cloud Session Banner (Only shown when an unfinished session exists)
        if (activeSessionState is CloudDataState.Success && activeSessionState.data != null) {
            val activeSession = activeSessionState.data
            val progressRatio = (
                (activeSession.currentIdx + 1).toFloat() /
                    max(1L, activeSession.totalTarget).toFloat()
                ).coerceIn(0.05f, 1f)
            val remainingMins = max(1L, activeSession.timerSeconds / 60L)

            item {
                PakReiSurfaceCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sesi Ujian Aktif Tersimpan",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        PakReiStatusBadge(
                            text = "Sisa ~${remainingMins}m",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Schedule
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${activeSession.subjectName} • ${activeSession.frameworkName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progres Pengerjaan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Soal #${activeSession.currentIdx + 1} dari ${activeSession.totalTarget}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onResumeSession(activeSession) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("resume_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lanjutkan Ujian",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        OutlinedButton(
                            onClick = onDiscardSession,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("discard_session_button")
                        ) {
                            Text(
                                text = "Hapus Sesi",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Subject Selector Strip (Mata Pelajaran Cepat)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                PakReiSectionHeader(
                    title = "Mulai Cepat Mata Pelajaran",
                    subtitle = "Pilih subtes populer untuk langsung masuk ke konfigurasi ujian"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_SUBJECT_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = {
                                onQuickStartSubject(preset.mode, preset.frameworkKey, preset.label)
                            },
                            label = {
                                Text(
                                    text = "${preset.label} (${preset.badge})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }

        // 4. Primary Tryout Package / Mode Selection
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                PakReiSectionHeader(
                    title = "Pilih Kategori & Paket Evaluasi",
                    subtitle = "Setiap kategori disesuaikan dengan taksonomi kognitif dan standar penilaian resmi"
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ModeListItem(
                            categoryBadge = "NASIONAL",
                            title = "Kurikulum Nasional & Seleksi PTN",
                            subtitle = "Kurikulum Merdeka (Fase A–F), TKA Pusmendik, AKM, UTBK-SNBT, K-13",
                            metaInfo = "10–40 Soal • Polytomous & IRT • SD/SMP/SMA",
                            icon = Icons.Default.AccountBalance,
                            iconColor = RoyalBlue,
                            iconBg = RoyalBlue.copy(alpha = 0.10f),
                            testTag = "mode_card_nasional",
                            onClick = { onSelectMode("nasional") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ModeListItem(
                            categoryBadge = "AI VISION PDF",
                            title = "Reverse Engineering Kisi-Kisi PDF",
                            subtitle = "Unggah dokumen kisi-kisi atau contoh soal PDF untuk diracik menjadi paket ujian",
                            metaInfo = "Ekstraksi Matriks Indikator • Blueprint Cloud",
                            icon = Icons.Default.Biotech,
                            iconColor = ReversePurple,
                            iconBg = ReversePurple.copy(alpha = 0.10f),
                            testTag = "mode_card_reverse",
                            onClick = { onSelectMode("reverse") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ModeListItem(
                            categoryBadge = "GLOBAL",
                            title = "Standar Asesmen Internasional",
                            subtitle = "OECD PISA, IEA TIMSS, Cambridge IGCSE/AS-A Level, IB Diploma, Digital SAT",
                            metaInfo = "ID / EN / Bilingual • Penyetaraan Band & PUM",
                            icon = Icons.Default.Public,
                            iconColor = IntlEmerald,
                            iconBg = IntlEmerald.copy(alpha = 0.10f),
                            testTag = "mode_card_internasional",
                            onClick = { onSelectMode("internasional") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ModeListItem(
                            categoryBadge = "NON-KLINIS",
                            title = "Bimbingan & Konseling (BK)",
                            subtitle = "Peminatan Karir Holland RIASEC, Gaya Belajar VARK, Regulasi Diri & KSE",
                            metaInfo = "Skala Likert & SJT • Peta Profil Siswa",
                            icon = Icons.Default.Favorite,
                            iconColor = BkRose,
                            iconBg = BkRose.copy(alpha = 0.10f),
                            testTag = "mode_card_bk",
                            onClick = { onSelectMode("bk") }
                        )
                    }
                }
            }
        }

        // 5. Learning Progress & Recent Tryout History Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                PakReiSectionHeader(
                    title = "Ringkasan Belajar & Riwayat Terbaru",
                    subtitle = if (completedResults.isEmpty()) {
                        "Rekam jejak hasil evaluasi Anda akan tampil di sini"
                    } else {
                        "Berdasarkan ${completedResults.size} sesi evaluasi yang telah diselesaikan"
                    },
                    trailingActionText = if (completedResults.isNotEmpty()) "Lihat Semua" else null,
                    onTrailingClick = if (completedResults.isNotEmpty()) onNavigateToHistory else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (completedResults.isEmpty()) {
                    PakReiSurfaceCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "Belum ada riwayat pengerjaan tryout",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih salah satu mata pelajaran atau kategori evaluasi di atas untuk memulai simulasi. Hasil nilai, pemetaan Zone of Proximal Development (ZPD), serta pembahasan tiap nomor akan tersimpan otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val avgScore = completedResults.map { it.scorePercent }.average().roundToInt()
                    val bestIndex = completedResults.maxOfOrNull { it.abilityIndexScore } ?: 500L

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PakReiMetricTile(
                            label = "Sesi Selesai",
                            value = "${completedResults.size}",
                            supportingText = "Paket Ujian",
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Rata-Rata Nilai",
                            value = "$avgScore%",
                            supportingText = "Skor Polytomous",
                            valueColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Indeks Tertinggi",
                            value = "$bestIndex",
                            supportingText = "Skala 200–800",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            completedResults.take(2).forEachIndexed { idx, rec ->
                                val dateStr = rec.createdAt?.toDate()?.let { dateFormat.format(it) }
                                    ?: "Tersimpan di Cloud"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenRecentResult(rec) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.subjectName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${rec.frameworkName} • $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${rec.scorePercent}%",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                if (idx == 0 && completedResults.size > 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Note: UserProfileAndLoginModal and AiModelSettingsDialog are rendered at the
    // AuthenticatedTryoutShell level in MainActivity so they work across all screens.
}

@Composable
private fun ModeListItem(
    categoryBadge: String,
    title: String,
    subtitle: String,
    metaInfo: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconBg,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = categoryBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = metaInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileAndLoginModal(
    profile: UserProfile?,
    fallbackName: String,
    fallbackNisn: String = "",
    fallbackSchool: String = "",
    fallbackGrade: String = "",
    fallbackPhotoBase64: String = "",
    loginHistoryState: CloudDataState<List<LoginHistoryRecord>>,
    onEncodePhoto: (Context, Uri, (String) -> Unit) -> Unit = { _, _, _ -> },
    onSave: (displayName: String, institution: String, nisn: String, gradeClass: String, photoBase64: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember(profile, fallbackName) {
        mutableStateOf(profile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var nisnInput by remember(profile, fallbackNisn) {
        mutableStateOf(profile?.nisn?.ifBlank { fallbackNisn } ?: fallbackNisn)
    }
    var instInput by remember(profile, fallbackSchool) {
        mutableStateOf(profile?.institution?.ifBlank { fallbackSchool } ?: fallbackSchool)
    }
    var gradeInput by remember(profile, fallbackGrade) {
        mutableStateOf(
            profile?.gradeClass?.ifBlank { fallbackGrade }?.ifBlank {
                CurriculumCatalog.STUDENT_GRADE_OPTIONS.first()
            } ?: fallbackGrade.ifBlank { CurriculumCatalog.STUDENT_GRADE_OPTIONS.first() }
        )
    }
    var photoBase64Input by remember(profile, fallbackPhotoBase64) {
        mutableStateOf(profile?.photoBase64?.ifBlank { fallbackPhotoBase64 } ?: fallbackPhotoBase64)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onEncodePhoto(context, uri) { encoded ->
                photoBase64Input = encoded
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.forLanguageTag("id-ID")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Info Akun & Identitas Peserta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Digunakan otomatis pada seluruh ujian, asesmen BK, kisi-kisi PDF, dan peringkat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar + Photo Picker Row
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            StudentAvatar(
                                displayName = nameInput.ifBlank { "Peserta" },
                                photoBase64 = photoBase64Input,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Foto Profil Peserta",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (photoBase64Input.isNotBlank()) "Foto profil tersimpan" else "Belum ada foto profil",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("profile_photo_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Ubah Foto",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ubah Foto",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Lengkap Peserta") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_edit")
                )

                OutlinedTextField(
                    value = nisnInput,
                    onValueChange = { nisnInput = it.filter { ch -> ch.isDigit() || ch == '-' }.take(24) },
                    label = { Text("NISN / Nomor Induk Siswa") },
                    placeholder = { Text("Contoh: 0078192831") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_nisn_edit")
                )

                OutlinedTextField(
                    value = instInput,
                    onValueChange = { instInput = it },
                    label = { Text("Asal Sekolah / Instansi") },
                    placeholder = { Text("Contoh: SMAN 1 Jakarta") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_inst_edit")
                )

                var gradeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = gradeExpanded,
                    onExpandedChange = { gradeExpanded = !gradeExpanded }
                ) {
                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kelas / Tingkat Saat Ini") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("profile_grade_edit")
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        CurriculumCatalog.STUDENT_GRADE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    gradeInput = option
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (profile != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Email: ${profile.email}\nMetode: ${profile.authMethod.uppercase()} • Total Sesi Login: ${profile.loginCount}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                val logs = (loginHistoryState as? CloudDataState.Success)?.data.orEmpty().take(3)
                if (logs.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "3 Aktivitas Login Terakhir:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    logs.forEach { log ->
                        val timeStr = log.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Baru saja"
                        Text(
                            text = "• $timeStr — ${log.authMethod.uppercase()} (${log.deviceInfo})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(nameInput, instInput, nisnInput, gradeInput, photoBase64Input)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AiModelSettingsDialog(
    currentModel: String,
    currentDualPass: Boolean,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
    var dualPass by remember(currentDualPass) { mutableStateOf(currentDualPass) }

    val models = listOf(
        Triple("gemini-3.8-flash", "Gemini 3.8 Flash", "Seimbang antara kecepatan & akurasi kurikuler (Rekomendasi)"),
        Triple("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", "Respon tercepat untuk simulasi kilat"),
        Triple("gemini-4-argon", "Gemini 4 Argon", "Penalaran mendalam untuk soal HOTS & Olimpiade")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Konfigurasi Mesin Soal AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih Model Pemrosesan:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                models.forEach { (id, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedModel = id }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selectedModel == id,
                            onClick = { selectedModel = id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dualPass = !dualPass }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dualPass,
                        onCheckedChange = { dualPass = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Dual-Pass Key Verification",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Audit ulang kunci jawaban matematika & sains sebelum soal ditampilkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedModel, dualPass) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
