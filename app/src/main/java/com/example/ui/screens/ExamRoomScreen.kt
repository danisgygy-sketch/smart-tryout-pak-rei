package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurriculumCatalog
import com.example.model.PsychometricEngine
import com.example.model.QuestionItem
import com.example.model.UserAnswerState
import com.example.ui.components.PakReiStatusBadge
import com.example.ui.components.PakReiSurfaceCard
import com.example.ui.theme.BkAmber
import com.example.ui.theme.InfoContainerBg
import com.example.ui.theme.InfoContainerBorder
import com.example.ui.theme.InfoContainerText
import com.example.ui.theme.IntlEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PakReiTryoutTheme
import com.example.ui.theme.ReversePurple
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

private enum class QuestionNavStatus {
    NOT_VISITED,
    VISITED_UNANSWERED,
    ANSWERED,
    FLAGGED_UNANSWERED,
    ANSWERED_AND_FLAGGED
}

@Composable
fun LoadingAiScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PakReiSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Meracik Soal AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamRoomScreen(
    uiState: TryoutUiState,
    onJumpToQuestion: (Int) -> Unit,
    onNavigatePrevNext: (Int) -> Unit,
    onToggleDoubt: () -> Unit,
    onSelectSingleOption: (Int) -> Unit,
    onToggleKompleksOption: (Int) -> Unit,
    onSelectBenarSalah: (Int, Boolean) -> Unit,
    onUpdateIsianText: (String) -> Unit,
    onRequestFinish: () -> Unit,
    onConfirmFinish: () -> Unit,
    onDismissFinishModal: () -> Unit
) {
    BackHandler {
        onRequestFinish()
    }

    var showNavigatorGrid by remember { mutableStateOf(false) }

    val currentIdx = uiState.currentIdx
    val currentQuestion = uiState.questions.getOrNull(currentIdx)
    val currentAnswer = uiState.answers.getOrElse(currentIdx) { UserAnswerState() }
    val currentDoubt = uiState.doubts.getOrElse(currentIdx) { false }
    val fw = CurriculumCatalog.FRAMEWORKS[uiState.frameworkKey]

    // Track visited question indices for the 5-state CBT Question Navigator
    var visitedIndices by remember(uiState.totalTarget) { mutableStateOf(setOf(0)) }
    LaunchedEffect(currentIdx) {
        visitedIndices = visitedIndices + currentIdx
    }

    val minutes = uiState.timerSeconds / 60
    val seconds = uiState.timerSeconds % 60
    val formattedTimer = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    val answeredCount = uiState.questions.indices.count { i ->
        uiState.answers.getOrNull(i)?.isAnswered(uiState.questions.getOrNull(i)) == true
    }
    val progressFraction = if (uiState.totalTarget > 0) {
        (answeredCount.toFloat() / uiState.totalTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Calm, non-panic timer styling that shifts only when time is genuinely low
    val isCriticalTime = uiState.timerSeconds < 120
    val isWarningTime = uiState.timerSeconds in 120..299
    val timerBgColor = when {
        isCriticalTime -> ZoneScaffoldBg
        isWarningTime -> ZoneZpdBg
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val timerBorderColor = when {
        isCriticalTime -> ZoneScaffoldBorder
        isWarningTime -> ZoneZpdBorder
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val timerTextColor = when {
        isCriticalTime -> ZoneScaffoldText
        isWarningTime -> ZoneZpdText
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. CALM CBT TOP BAR
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.subjectName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Soal ${currentIdx + 1} dari ${uiState.totalTarget} • ${fw?.badge ?: uiState.frameworkKey.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Non-panic countdown timer badge
                        Surface(
                            color = timerBgColor,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, timerBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sisa Waktu Ujian",
                                    tint = timerTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = formattedTimer,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = timerTextColor,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Finish Exam Action (Outlined / Tonal so it doesn't cause panic but stays accessible)
                        OutlinedButton(
                            onClick = onRequestFinish,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("finish_exam_top_button")
                        ) {
                            Text(
                                text = "Akhiri",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Linear Progress Indicator showing answered completion
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )

                // Sub-strip: Cloud Sync Status + Expandable 5-State Question Navigator Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Status Sinkronisasi",
                            tint = IntlEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isFetchingBackgroundBatch) {
                                "Menyiapkan kloter soal berikutnya (${uiState.questions.size}/${uiState.totalTarget})..."
                            } else {
                                "Terjawab $answeredCount/${uiState.totalTarget} • ${uiState.syncStatusText}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showNavigatorGrid = !showNavigatorGrid }
                            .testTag("toggle_question_map_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Buka Peta Soal",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Daftar Soal (${currentIdx + 1}/${uiState.totalTarget})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = if (showNavigatorGrid) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 5-State Question Navigator Drawer / Panel
                AnimatedVisibility(visible = showNavigatorGrid) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until uiState.totalTarget) {
                                val isLoaded = i < uiState.questions.size
                                val qItem = uiState.questions.getOrNull(i)
                                val isAnswered = isLoaded && uiState.answers.getOrNull(i)?.isAnswered(qItem) == true
                                val isDoubt = isLoaded && uiState.doubts.getOrNull(i) == true
                                val isVisited = i in visitedIndices
                                val isActive = i == currentIdx

                                val navStatus = when {
                                    isAnswered && isDoubt -> QuestionNavStatus.ANSWERED_AND_FLAGGED
                                    isDoubt -> QuestionNavStatus.FLAGGED_UNANSWERED
                                    isAnswered -> QuestionNavStatus.ANSWERED
                                    isVisited -> QuestionNavStatus.VISITED_UNANSWERED
                                    else -> QuestionNavStatus.NOT_VISITED
                                }

                                QuestionNavCell(
                                    index = i,
                                    isLoaded = isLoaded,
                                    isActive = isActive,
                                    status = navStatus,
                                    onClick = { onJumpToQuestion(i) }
                                )
                            }
                        }

                        // 5-State Legend (Never relies on color alone)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NavigatorLegendItem(
                                badgeText = "✓",
                                bgColor = ZoneMandiriBg,
                                borderColor = ZoneMandiriBorder,
                                textColor = ZoneMandiriText,
                                label = "Sudah Dijawab"
                            )
                            NavigatorLegendItem(
                                badgeText = "⚑",
                                bgColor = ZoneZpdBg,
                                borderColor = ZoneZpdBorder,
                                textColor = ZoneZpdText,
                                label = "Ditandai Tinjauan"
                            )
                            NavigatorLegendItem(
                                badgeText = "✓⚑",
                                bgColor = InfoContainerBg,
                                borderColor = BkAmber,
                                textColor = InfoContainerText,
                                label = "Terjawab & Ditinjau"
                            )
                            NavigatorLegendItem(
                                badgeText = "○",
                                bgColor = MaterialTheme.colorScheme.surface,
                                borderColor = MaterialTheme.colorScheme.outline,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                label = "Belum Dijawab"
                            )
                            NavigatorLegendItem(
                                badgeText = "·",
                                bgColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "Belum Dikunjungi"
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        // 2. MAIN READING & RESPONSE WORKSPACE
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (currentQuestion == null) {
                item {
                    PakReiSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menyiapkan butir soal...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                item {
                    PakReiSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Metadata Badges Row: Item Number, Format Type, Cognitive Level, Weight
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PakReiStatusBadge(
                                    text = "BUTIR #${currentIdx + 1}",
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )

                                val typeBadgeText = when (currentQuestion.type) {
                                    "kompleks" -> "PGK MCMA (Lebih dari 1 Jawaban)"
                                    "benarsalah" -> "PGK Kategori (Benar / Salah)"
                                    "isian" -> "Isian Singkat"
                                    "likert" -> "Refleksi Diri (Likert)"
                                    "sjt" -> "Situational Judgment (SJT)"
                                    else -> "Pilihan Ganda Tunggal"
                                }
                                PakReiStatusBadge(
                                    text = typeBadgeText,
                                    containerColor = InfoContainerBg,
                                    contentColor = InfoContainerText,
                                    borderColor = InfoContainerBorder
                                )

                                PakReiStatusBadge(
                                    text = currentQuestion.cognitiveLevel,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = ReversePurple,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                )

                                if (uiState.appMode != "bk") {
                                    val weight = PsychometricEngine.getCognitiveWeight(
                                        uiState.appMode,
                                        currentQuestion.cognitiveLevel
                                    )
                                    PakReiStatusBadge(
                                        text = "Bobot ${weight}x",
                                        containerColor = ZoneMandiriBg,
                                        contentColor = ZoneMandiriText,
                                        borderColor = ZoneMandiriBorder
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Domain & Indicator Context Strip
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "Domain: ${currentQuestion.topic}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentQuestion.indicator.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Indikator: ${currentQuestion.indicator}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Stimulus Passage / Data Table
                            if (currentQuestion.stimulus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                StimulusCard(stimulusText = currentQuestion.stimulus)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // High-readability Question Stem (16sp / 26sp line-height from bodyLarge)
                            Text(
                                text = currentQuestion.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Interactive Answer Area
                            QuestionInteractiveArea(
                                question = currentQuestion,
                                answerState = currentAnswer,
                                onSelectSingle = onSelectSingleOption,
                                onToggleKompleks = onToggleKompleksOption,
                                onSelectBenarSalah = onSelectBenarSalah,
                                onUpdateIsian = onUpdateIsianText
                            )
                        }
                    }
                }
            }
        }

        // 3. BOTTOM ERGONOMIC CBT ACTION BAR
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onNavigatePrevNext(-1) },
                        enabled = currentIdx > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("prev_question_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Soal Sebelumnya",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sebelumnya",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Mark for Review / Doubt Button with explicit state label + icon
                    Surface(
                        onClick = onToggleDoubt,
                        color = if (currentDoubt) ZoneZpdBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (currentDoubt) 1.5.dp else 1.dp,
                            color = if (currentDoubt) BkAmber else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("doubt_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Tandai Ragu-ragu",
                                tint = if (currentDoubt) ZoneZpdText else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentDoubt) "Ditinjau" else "Ragu",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (currentDoubt) ZoneZpdText else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isLastQuestion = currentIdx >= uiState.totalTarget - 1
                    Button(
                        onClick = { onNavigatePrevNext(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastQuestion) IntlEmerald else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("next_question_button")
                    ) {
                        Text(
                            text = if (isLastQuestion) "Selesai" else "Selanjutnya",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastQuestion) {
                                Icons.Default.Check
                            } else {
                                Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = "Selanjutnya",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // 4. FINISH EXAM CONFIRMATION MODAL
    if (uiState.showFinishConfirmModal) {
        val doubtCount = uiState.doubts.count { it }
        val emptyCount = (uiState.totalTarget - answeredCount).coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = onDismissFinishModal,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Konfirmasi Pengumpulan Ujian",
                    tint = BkAmber,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Kumpulkan & Analisis Hasil?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Periksa ringkasan status pengerjaan Anda sebelum hasil dievaluasi:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCountBadge(
                            label = "Terjawab",
                            count = answeredCount,
                            bgColor = ZoneMandiriBg,
                            borderColor = ZoneMandiriBorder,
                            textColor = ZoneMandiriText,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCountBadge(
                            label = "Ditinjau",
                            count = doubtCount,
                            bgColor = ZoneZpdBg,
                            borderColor = ZoneZpdBorder,
                            textColor = ZoneZpdText,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCountBadge(
                            label = "Kosong",
                            count = emptyCount,
                            bgColor = ZoneScaffoldBg,
                            borderColor = ZoneScaffoldBorder,
                            textColor = ZoneScaffoldText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (emptyCount > 0 || doubtCount > 0) {
                        Surface(
                            color = ZoneZpdBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ZoneZpdBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = buildString {
                                    if (emptyCount > 0) {
                                        append("Masih terdapat $emptyCount soal yang belum dijawab. ")
                                    }
                                    if (doubtCount > 0) {
                                        append("Terdapat $doubtCount soal yang masih ditandai ragu-ragu.")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ZoneZpdText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = IntlEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_finish_exam_button")
                ) {
                    Text(
                        text = "Ya, Kumpulkan Sekarang",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissFinishModal) {
                    Text("Kembali ke Soal")
                }
            }
        )
    }
}

@Composable
private fun QuestionNavCell(
    index: Int,
    isLoaded: Boolean,
    isActive: Boolean,
    status: QuestionNavStatus,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor, badgeSymbol) = when (status) {
        QuestionNavStatus.ANSWERED_AND_FLAGGED -> Quadruple(
            InfoContainerBg,
            BkAmber,
            InfoContainerText,
            "✓⚑"
        )
        QuestionNavStatus.FLAGGED_UNANSWERED -> Quadruple(
            ZoneZpdBg,
            ZoneZpdBorder,
            ZoneZpdText,
            "⚑"
        )
        QuestionNavStatus.ANSWERED -> Quadruple(
            ZoneMandiriBg,
            ZoneMandiriBorder,
            ZoneMandiriText,
            "✓"
        )
        QuestionNavStatus.VISITED_UNANSWERED -> Quadruple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface,
            "○"
        )
        QuestionNavStatus.NOT_VISITED -> Quadruple(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            ""
        )
    }

    val effectiveBg = if (isActive) MaterialTheme.colorScheme.primary else bgColor
    val effectiveText = if (isActive) MaterialTheme.colorScheme.onPrimary else textColor
    val effectiveBorder = if (isActive) MaterialTheme.colorScheme.primary else borderColor

    Surface(
        onClick = onClick,
        enabled = isLoaded,
        color = effectiveBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, effectiveBorder),
        modifier = Modifier
            .size(44.dp)
            .semantics {
                contentDescription = "Soal nomor ${index + 1}, status ${status.name}"
                role = Role.Button
            }
            .testTag("nav_item_$index")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${index + 1}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = effectiveText
            )
            if (badgeSymbol.isNotEmpty()) {
                Text(
                    text = badgeSymbol,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = effectiveText
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun NavigatorLegendItem(
    badgeText: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCountBadge(
    label: String,
    count: Int,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontFamily = JetBrainsMonoFamily,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StimulusCard(stimulusText: String) {
    val lines = stimulusText.lines()
    val paragraphs = mutableListOf<String>()
    val tableRows = mutableListOf<List<String>>()

    lines.forEach { rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
            val cells = trimmed.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
            val isDivider = cells.all { cell -> cell.all { ch -> ch == '-' || ch == ':' || ch == ' ' } }
            if (!isDivider) {
                tableRows.add(cells)
            }
        } else if (trimmed.isNotBlank()) {
            paragraphs.add(trimmed)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left structural reading accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "WACANA / STIMULUS KONTEKSTUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                paragraphs.forEachIndexed { idx, para ->
                    Text(
                        text = para,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (idx < paragraphs.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (tableRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            tableRows.forEachIndexed { rowIdx, rowCells ->
                                val isHeader = rowIdx == 0
                                Row(
                                    modifier = Modifier.background(
                                        if (isHeader) {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        } else if (rowIdx % 2 == 1) {
                                            MaterialTheme.colorScheme.surface
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                    )
                                ) {
                                    rowCells.forEach { cellText ->
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 110.dp, max = 220.dp)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = cellText,
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                if (rowIdx < tableRows.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
private fun QuestionInteractiveArea(
    question: QuestionItem,
    answerState: UserAnswerState,
    onSelectSingle: (Int) -> Unit,
    onToggleKompleks: (Int) -> Unit,
    onSelectBenarSalah: (Int, Boolean) -> Unit,
    onUpdateIsian: (String) -> Unit
) {
    val letters = listOf("A", "B", "C", "D", "E", "F")

    when (question.type) {
        "pilgan", "likert", "sjt" -> {
            val isPsychometric = question.type == "likert" || question.type == "sjt"
            if (isPsychometric) {
                Surface(
                    color = ZoneScaffoldBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ZoneScaffoldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = if (question.type == "likert") {
                            "Pilih satu tingkat kesesuaian yang paling menggambarkan dirimu secara jujur:"
                        } else {
                            "Pilih satu tindakan yang paling mungkin kamu lakukan pada situasi tersebut:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ZoneScaffoldText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { idx, optionText ->
                    val isSelected = answerState.singleChoice == idx
                    Surface(
                        onClick = { onSelectSingle(idx) },
                        color = if (isSelected) {
                            InfoContainerBg
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                RoyalBlue
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_single_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isSelected) {
                                    RoyalBlue
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = letters.getOrElse(idx) { "${idx + 1}" },
                                        fontFamily = JetBrainsMonoFamily,
                                        color = if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    InfoContainerText
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSingle(idx) }
                            )
                        }
                    }
                }
            }
        }

        "kompleks" -> {
            Surface(
                color = InfoContainerBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, InfoContainerBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Petunjuk PGK MCMA: Anda dapat memilih LEBIH DARI SATU jawaban yang benar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InfoContainerText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { idx, optionText ->
                    val isChecked = idx in answerState.multiChoices
                    Surface(
                        onClick = { onToggleKompleks(idx) },
                        color = if (isChecked) {
                            InfoContainerBg
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isChecked) 2.dp else 1.dp,
                            color = if (isChecked) {
                                RoyalBlue
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_kompleks_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleKompleks(idx) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${letters.getOrElse(idx) { "${idx + 1}" }}. $optionText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isChecked) {
                                    InfoContainerText
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        "benarsalah" -> {
            Surface(
                color = ZoneMandiriBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ZoneMandiriBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Petunjuk PGK Kategori: Tentukan Benar atau Salah untuk setiap pernyataan di bawah ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZoneMandiriText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.statements.forEachIndexed { stmtIdx, statementText ->
                    val currentSelection = answerState.boolChoices.getOrNull(stmtIdx)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${stmtIdx + 1}. $statementText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isTrueSelected = currentSelection == true
                                val isFalseSelected = currentSelection == false

                                OutlinedButton(
                                    onClick = { onSelectBenarSalah(stmtIdx, true) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isTrueSelected) {
                                            ZoneMandiriBg
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        contentColor = if (isTrueSelected) {
                                            ZoneMandiriText
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = if (isTrueSelected) 2.dp else 1.dp,
                                        color = if (isTrueSelected) {
                                            IntlEmerald
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("stmt_${stmtIdx}_true")
                                ) {
                                    Text(
                                        text = if (isTrueSelected) "✓ BENAR" else "BENAR",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onSelectBenarSalah(stmtIdx, false) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isFalseSelected) {
                                            ZoneScaffoldBg
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        contentColor = if (isFalseSelected) {
                                            ZoneScaffoldText
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = if (isFalseSelected) 2.dp else 1.dp,
                                        color = if (isFalseSelected) {
                                            ZoneScaffoldText
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("stmt_${stmtIdx}_false")
                                ) {
                                    Text(
                                        text = if (isFalseSelected) "✕ SALAH" else "SALAH",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        "isian" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ketik Jawaban Singkat / Numerik:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = answerState.textAnswer,
                    onValueChange = onUpdateIsian,
                    placeholder = { Text("Contoh: 24 atau 3.5") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("isian_text_input")
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExamRoomScreenPreview() {
    PakReiTryoutTheme(darkTheme = false) {
        ExamRoomScreen(
            uiState = TryoutUiState(
                participantName = "Nadia Putri",
                subjectName = "Penalaran Umum (PU)",
                totalTarget = 10,
                timerSeconds = 1450,
                questions = listOf(
                    QuestionItem(
                        type = "pilgan",
                        topic = "Silogisme & Logika Deduktif",
                        cognitiveLevel = "L3 (Penalaran)",
                        indicator = "Menarik simpulan valid dari dua premis bersyarat",
                        stimulus = "Semua peserta SNBT yang disiplin berlatih tryout mengalami peningkatan skor kemampuan kuantitatif.\nSebagian siswa kelas XII-A mendapatkan skor kuantitatif di atas 700.",
                        text = "Manakah simpulan yang PALING TEPAT berdasarkan informasi di atas?",
                        options = listOf(
                            "Semua siswa kelas XII-A disiplin berlatih tryout.",
                            "Peserta yang tidak mengalami peningkatan skor kuantitatif pasti tidak disiplin berlatih tryout.",
                            "Sebagian siswa kelas XII-A tidak mengikuti SNBT.",
                            "Tidak ada siswa yang mendapat skor di bawah 700.",
                            "Semua peserta tryout adalah siswa kelas XII-A."
                        ),
                        singleAnswer = 1
                    )
                ),
                answers = listOf(UserAnswerState(singleChoice = 1)),
                doubts = listOf(false)
            ),
            onJumpToQuestion = {},
            onNavigatePrevNext = {},
            onToggleDoubt = {},
            onSelectSingleOption = {},
            onToggleKompleksOption = {},
            onSelectBenarSalah = { _, _ -> },
            onUpdateIsianText = {},
            onRequestFinish = {},
            onConfirmFinish = {},
            onDismissFinishModal = {}
        )
    }
}
