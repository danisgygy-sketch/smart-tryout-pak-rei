package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BlueprintRow
import com.example.model.CurriculumCatalog
import com.example.ui.components.PakReiMetricTile
import com.example.ui.components.PakReiSectionHeader
import com.example.ui.components.PakReiStatusBadge
import com.example.ui.components.PakReiSurfaceCard
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.BkRose
import com.example.ui.theme.InfoContainerBg
import com.example.ui.theme.InfoContainerBorder
import com.example.ui.theme.InfoContainerText
import com.example.ui.theme.IntlEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PakReiTryoutTheme
import com.example.ui.theme.ReverseIndigo
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.ZoneMandiriBg
import com.example.ui.theme.ZoneMandiriBorder
import com.example.ui.theme.ZoneMandiriText
import com.example.ui.theme.ZoneScaffoldBg
import com.example.ui.theme.ZoneScaffoldBorder
import com.example.ui.theme.ZoneScaffoldText
import com.example.ui.viewmodel.TryoutStep
import com.example.ui.viewmodel.TryoutUiState

private fun resolveFileName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        } ?: uri.lastPathSegment ?: "dokumen.pdf"
    } catch (_: Exception) {
        "dokumen.pdf"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigAndBlueprintScreen(
    uiState: TryoutUiState,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit = {},
    onOpenAccountProfile: () -> Unit = {},
    onSelectFramework: (String) -> Unit,
    onSelectStage: (String) -> Unit,
    onUpdateSubject: (String) -> Unit,
    onUpdateTopicFocus: (String) -> Unit,
    onUpdateDifficulty: (String) -> Unit,
    onUpdateScoringMethod: (String) -> Unit,
    onUpdateTotalTarget: (Int) -> Unit,
    onUpdateDuration: (Int) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onPdfSelected: (Context, Uri, Boolean, String) -> Unit,
    onRunReverseEngineering: () -> Unit,
    onUpdateBlueprintRow: (Int, BlueprintRow) -> Unit,
    onAddBlueprintRow: () -> Unit,
    onRemoveBlueprintRow: (Int) -> Unit,
    onSaveBlueprintToCloud: () -> Unit,
    onCompileCard: () -> Unit,
    onStartAiExam: () -> Unit,
    onStartDemoExam: () -> Unit
) {
    val context = LocalContext.current
    BackHandler {
        onBack()
    }

    val kisiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = resolveFileName(context, uri)
            onPdfSelected(context, uri, false, name)
        }
    }

    val samplePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = resolveFileName(context, uri)
            onPdfSelected(context, uri, true, name)
        }
    }

    val frameworks = CurriculumCatalog.frameworksForMode(uiState.appMode)
    val currentFw = CurriculumCatalog.FRAMEWORKS[uiState.frameworkKey] ?: frameworks.first()
    val stages = currentFw.stages.keys.toList()
    val subjectSuggestions = currentFw.stages[uiState.stage].orEmpty()

    val modeAccentColor = when (uiState.appMode) {
        "reverse" -> ReversePurple
        "internasional" -> IntlEmerald
        "bk" -> BkRose
        else -> RoyalBlue
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation & Context Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kembali",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (uiState.appMode) {
                            "reverse" -> "Reverse Engineering Kisi-Kisi PDF"
                            "internasional" -> "Standar Asesmen Internasional"
                            "bk" -> "Asesmen BK Psikologis [BETA]"
                            else -> "Konfigurasi Tryout Nasional"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentFw.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = modeAccentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (uiState.step == TryoutStep.ID_CARD_PREVIEW) {
            item {
                DigitalIdentityCardView(
                    uiState = uiState,
                    frameworkName = currentFw.name,
                    frameworkBadge = currentFw.badge,
                    modeAccentColor = modeAccentColor,
                    onStartAiExam = onStartAiExam,
                    onStartDemoExam = onStartDemoExam,
                    onEditConfig = onBack
                )
            }
        } else {
            // STEP 1: IDENTITAS DARI AKUN & KERANGKA KURIKULUM
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PakReiSectionHeader(
                        title = "1. Identitas Akun & Kerangka Kurikulum",
                        subtitle = "Data peserta diambil otomatis dari Info Akun Anda untuk kemudahan sesi & pemeringkatan"
                    )

                    PakReiSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Read-only Account-Bound Student Identity Summary Card
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("participant_name_input")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        StudentAvatar(
                                            displayName = uiState.participantName.ifBlank { "Peserta Evaluasi" },
                                            photoBase64 = uiState.participantPhotoBase64,
                                            size = 46.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = uiState.participantName.ifBlank { "Peserta Evaluasi" },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val metaLine = buildString {
                                                append("NISN: ${uiState.participantNisn.ifBlank { "-" }}")
                                                if (uiState.participantGrade.isNotBlank()) {
                                                    append(" • ${uiState.participantGrade}")
                                                }
                                                if (uiState.participantSchool.isNotBlank()) {
                                                    append(" • ${uiState.participantSchool}")
                                                }
                                            }
                                            Text(
                                                text = metaLine,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = onOpenAccountProfile,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("edit_account_from_config_button")
                                    ) {
                                        Text(
                                            text = "Info Akun",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            // Framework Dropdown
                            var fwExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = fwExpanded,
                                onExpandedChange = { fwExpanded = !fwExpanded }
                            ) {
                                OutlinedTextField(
                                    value = currentFw.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Standar / Kerangka Kurikulum") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fwExpanded)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = fwExpanded,
                                    onDismissRequest = { fwExpanded = false }
                                ) {
                                    frameworks.forEach { fw ->
                                        DropdownMenuItem(
                                            text = { Text(fw.name) },
                                            onClick = {
                                                onSelectFramework(fw.key)
                                                fwExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Framework Description Callout
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = modeAccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = currentFw.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Stage / Phase Dropdown
                            var stageExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = stageExpanded,
                                onExpandedChange = { stageExpanded = !stageExpanded }
                            ) {
                                OutlinedTextField(
                                    value = uiState.stage,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Jenjang / Fase / Kelas") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = stageExpanded,
                                    onDismissRequest = { stageExpanded = false }
                                ) {
                                    stages.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                onSelectStage(st)
                                                stageExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Subject Input + Quick Suggestion Chips
                            OutlinedTextField(
                                value = uiState.subjectName,
                                onValueChange = onUpdateSubject,
                                label = { Text("Mata Pelajaran / Subtes") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("subject_name_input")
                            )

                            if (subjectSuggestions.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Rekomendasi Subtes Kurikulum:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        subjectSuggestions.take(6).forEach { subj ->
                                            FilterChip(
                                                selected = uiState.subjectName == subj,
                                                onClick = { onUpdateSubject(subj) },
                                                label = {
                                                    Text(
                                                        text = subj,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Topic Focus Input
                            OutlinedTextField(
                                value = uiState.topicFocus,
                                onValueChange = onUpdateTopicFocus,
                                label = { Text("Fokus Bab / Topik / CP-KD (Opsional)") },
                                placeholder = { Text("Kosongkan untuk distribusi seimbang sesuai kurikulum") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // STEP 2: PARAMETER & KALIBRASI UJIAN
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PakReiSectionHeader(
                        title = "2. Parameter & Kalibrasi Ujian",
                        subtitle = "Atur tingkat kesulitan kognitif, metode penskoran, jumlah soal, dan waktu"
                    )

                    PakReiSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (uiState.appMode != "bk") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Tingkat Kesulitan Kognitif",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Standar", "HOTS", "Fondasi", "Olimpiade").forEach { diff ->
                                            FilterChip(
                                                selected = uiState.difficultyCode == diff,
                                                onClick = { onUpdateDifficulty(diff) },
                                                label = { Text(diff) }
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Metode Penskoran",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.scoringMethod == "polytomous",
                                            onClick = { onUpdateScoringMethod("polytomous") },
                                            label = { Text("Polytomous (Kredit Parsial)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = uiState.scoringMethod == "strict",
                                            onClick = { onUpdateScoringMethod("strict") },
                                            label = { Text("Dikotomis Ketat (1/0)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Jumlah Butir Soal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(10, 20, 30, 40).forEach { qCount ->
                                            FilterChip(
                                                selected = uiState.totalTarget == qCount,
                                                onClick = { onUpdateTotalTarget(qCount) },
                                                label = { Text("$qCount") }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.durationMinutes.toString(),
                                    onValueChange = { v ->
                                        v.toIntOrNull()?.let { onUpdateDuration(it) }
                                    },
                                    label = { Text("Durasi (Menit)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(136.dp)
                                )
                            }

                            if (uiState.appMode == "internasional" || uiState.appMode == "reverse") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Bahasa Pengantar Soal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            "id" to "Bahasa Indonesia",
                                            "en" to "English",
                                            "bilingual" to "Bilingual"
                                        ).forEach { (code, label) ->
                                            FilterChip(
                                                selected = uiState.lang == code,
                                                onClick = { onUpdateLanguage(code) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.appMode == "bk") {
                                Surface(
                                    color = ZoneScaffoldBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ZoneScaffoldBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "Etika BK",
                                            tint = ZoneScaffoldText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Etika Asesmen BK Psikologis [Non-Klinis]: Mengacu pada standar POP BK Kemendikdasmen untuk pemetaan potensi diri, minat karir, dan kesiapan belajar (tidak ada jawaban benar/salah).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ZoneScaffoldText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STEP 3: REVERSE ENGINEERING PDF WORKBENCH (ONLY FOR REVERSE MODE)
            if (uiState.appMode == "reverse") {
                item {
                    ReverseEngineeringWorkbenchCard(
                        uiState = uiState,
                        onPickKisiPdf = { kisiPicker.launch(arrayOf("application/pdf")) },
                        onPickSamplePdf = { samplePicker.launch(arrayOf("application/pdf")) },
                        onRunReverseEngineering = onRunReverseEngineering,
                        onUpdateRow = onUpdateBlueprintRow,
                        onAddRow = onAddBlueprintRow,
                        onRemoveRow = onRemoveBlueprintRow,
                        onSaveToCloud = onSaveBlueprintToCloud
                    )
                }
            }

            // LIVE SUMMARY STRIP + COMPILE CTA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PakReiSurfaceCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ringkasan Sesi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.subjectName} • ${uiState.stage}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PakReiStatusBadge(
                                    text = "${uiState.totalTarget} Soal",
                                    containerColor = InfoContainerBg,
                                    contentColor = InfoContainerText,
                                    borderColor = InfoContainerBorder
                                )
                                PakReiStatusBadge(
                                    text = "${uiState.durationMinutes} Menit",
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onCompileCard,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = modeAccentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("compile_card_button")
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_compile_card),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReverseEngineeringWorkbenchCard(
    uiState: TryoutUiState,
    onPickKisiPdf: () -> Unit,
    onPickSamplePdf: () -> Unit,
    onRunReverseEngineering: () -> Unit,
    onUpdateRow: (Int, BlueprintRow) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    onSaveToCloud: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PakReiSectionHeader(
            title = "3. Workbench Reverse Engineering Kisi-Kisi PDF",
            subtitle = "Ekstrak indikator dari PDF tabel atau hasil scan menggunakan Android PdfRenderer + Gemini Vision"
        )

        PakReiSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPickKisiPdf() }
                            .testTag("pick_kisi_pdf_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Kisi-Kisi",
                                tint = ReversePurple
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.kisiFileName.ifBlank { "1. Pilih PDF Kisi-Kisi" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPickSamplePdf() }
                            .testTag("pick_sample_pdf_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Upload Contoh Soal",
                                tint = ReverseIndigo
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.sampleFileName.ifBlank { "2. Contoh Soal (Opsional)" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Button(
                    onClick = onRunReverseEngineering,
                    enabled = !uiState.isAnalyzingPdf,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReversePurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_pdf_button")
                ) {
                    if (uiState.isAnalyzingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Membedah Dokumen PDF...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.kisiFileName.isBlank()) {
                                "Bedah PDF / Muat Contoh Blueprint"
                            } else {
                                "Bedah PDF dengan AI Vision"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.isAnalyzingPdf) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = ReversePurple
                    )
                    Text(
                        text = uiState.pdfAnalysisProgressMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val bp = uiState.blueprint
                if (bp != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            PakReiStatusBadge(
                                text = "BLUEPRINT TERDETEKSI",
                                containerColor = ZoneMandiriBg,
                                contentColor = ZoneMandiriText,
                                borderColor = ZoneMandiriBorder
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${bp.items.size} Indikator • Total ${bp.items.sumOf { it.count }} Butir",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onAddRow,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Baris",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Baris", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onSaveToCloud,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ReverseIndigo),
                                modifier = Modifier.testTag("save_blueprint_cloud_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Simpan Cloud",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan Cloud", fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DNA Gaya Soal: ${bp.styleDNA}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    bp.items.forEachIndexed { idx, row ->
                        BlueprintEditableRowCard(
                            index = idx,
                            row = row,
                            onUpdate = { onUpdateRow(idx, it) },
                            onDelete = { onRemoveRow(idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueprintEditableRowCard(
    index: Int,
    row: BlueprintRow,
    onUpdate: (BlueprintRow) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indikator #${index + 1} • ${row.cognitiveLevel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ReverseIndigo,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Indikator",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = row.domain,
                onValueChange = { onUpdate(row.copy(domain = it)) },
                label = { Text("Domain / Materi") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = row.indicator,
                onValueChange = { onUpdate(row.copy(indicator = it)) },
                label = { Text("Rumusan Indikator Soal") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "pilgan" to "Pilgan",
                        "kompleks" to "MCMA",
                        "benarsalah" to "B/S",
                        "isian" to "Isian"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = row.questionType == code,
                            onClick = { onUpdate(row.copy(questionType = code)) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Jml:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = row.count.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let { onUpdate(row.copy(count = it.coerceIn(1, 20))) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(64.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalIdentityCardView(
    uiState: TryoutUiState,
    frameworkName: String,
    frameworkBadge: String,
    modeAccentColor: Color,
    onStartAiExam: () -> Unit,
    onStartDemoExam: () -> Unit,
    onEditConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PakReiSectionHeader(
            title = "Kartu Kendali & Konfirmasi Sesi Ujian",
            subtitle = "Periksa kembali detail ujian dan aturan pengerjaan sebelum memulai"
        )

        // Official Institutional Exam Pass Card (Crisp Surface + Accent Header Strip)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top institutional accent bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(modeAccentColor)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = frameworkBadge.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = frameworkName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.cardIdCode,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudentAvatar(
                            displayName = uiState.participantName.ifBlank { "Peserta Evaluasi" },
                            photoBase64 = uiState.participantPhotoBase64,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "IDENTITAS PESERTA (TERHUBUNG AKUN)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.participantName.ifBlank { "Peserta Evaluasi" },
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            val cardIdentitySub = buildString {
                                append("NISN: ${uiState.participantNisn.ifBlank { "-" }}")
                                if (uiState.participantGrade.isNotBlank()) {
                                    append(" • ${uiState.participantGrade}")
                                }
                                if (uiState.participantSchool.isNotBlank()) {
                                    append(" • ${uiState.participantSchool}")
                                }
                            }
                            Text(
                                text = cardIdentitySub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "MATA PELAJARAN & JENJANG",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.stage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PakReiMetricTile(
                            label = "Jumlah Soal",
                            value = "${uiState.totalTarget}",
                            supportingText = "Butir Terkalibrasi",
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Alokasi Waktu",
                            value = "${uiState.durationMinutes}m",
                            supportingText = "Hitung Mundur CBT",
                            modifier = Modifier.weight(1f)
                        )
                        PakReiMetricTile(
                            label = "Penskoran",
                            value = if (uiState.appMode == "bk") {
                                "Profil"
                            } else if (uiState.scoringMethod == "polytomous") {
                                "Parsial"
                            } else {
                                "Ketat"
                            },
                            supportingText = uiState.difficultyCode,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Exam Rules Checklist
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ketentuan Pengerjaan:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            ExamRuleRow("Progres jawaban tersimpan otomatis ke Cloud Firestore.")
                            ExamRuleRow("Gunakan fitur Tandai Ragu-Ragu untuk meninjau ulang soal sebelum dikumpulkan.")
                            ExamRuleRow("Laporan diagnostik, grafik radar, dan pembahasan akan langsung tersedia setelah selesai.")
                        }
                    }
                }
            }
        }

        // Primary & Secondary Launch Actions
        Button(
            onClick = onStartAiExam,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_ai_exam_button")
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_start_ai_exam),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onStartDemoExam,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_demo_exam_button")
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_start_demo_exam),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onEditConfig,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Ubah Konfigurasi Ujian",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ExamRuleRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = IntlEmerald,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConfigAndBlueprintScreenPreview() {
    PakReiTryoutTheme(darkTheme = false) {
        ConfigAndBlueprintScreen(
            uiState = TryoutUiState(
                participantName = "Nadia Putri",
                subjectName = "Penalaran Umum (PU)",
                totalTarget = 20,
                durationMinutes = 30
            ),
            onBack = {},
            onUpdateName = {},
            onSelectFramework = {},
            onSelectStage = {},
            onUpdateSubject = {},
            onUpdateTopicFocus = {},
            onUpdateDifficulty = {},
            onUpdateScoringMethod = {},
            onUpdateTotalTarget = {},
            onUpdateDuration = {},
            onUpdateLanguage = {},
            onPdfSelected = { _, _, _, _ -> },
            onRunReverseEngineering = {},
            onUpdateBlueprintRow = { _, _ -> },
            onAddBlueprintRow = {},
            onRemoveBlueprintRow = {},
            onSaveBlueprintToCloud = {},
            onCompileCard = {},
            onStartAiExam = {},
            onStartDemoExam = {}
        )
    }
}
