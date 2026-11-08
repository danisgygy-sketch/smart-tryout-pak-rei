package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PakReiAiLogicRepository
import com.example.domain.contracts.ActiveSessionRepository
import com.example.domain.contracts.BlueprintExtractorService
import com.example.domain.contracts.BlueprintRepository
import com.example.domain.contracts.DiagnosticAnalyzerService
import com.example.domain.contracts.ExamResultRepository
import com.example.domain.contracts.PdfPageRendererService
import com.example.domain.contracts.ProfilePhotoService
import com.example.domain.contracts.QuestionGeneratorService
import com.example.domain.contracts.UserProfileRepository
import com.example.domain.usecase.CalculateDiagnosticReportUseCase
import com.example.model.ActiveExamSession
import com.example.model.BlueprintData
import com.example.model.BlueprintRow
import com.example.model.CurriculumCatalog
import com.example.model.DimensionStat
import com.example.model.ExamResultRecord
import com.example.model.LoginHistoryRecord
import com.example.model.PakReiSummary
import com.example.model.PendingAuthProfileStore
import com.example.model.PsychometricEngine
import com.example.model.QuestionItem
import com.example.model.QuestionReviewItem
import com.example.model.SavedBlueprintRecord
import com.example.model.UserAnswerState
import com.example.model.UserProfile
import com.example.util.AndroidPdfPageRenderer
import com.example.util.ProfilePhotoProcessor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private const val TAG = "TryoutSessionVM"

enum class TryoutStep {
    LANDING,
    REGISTRATION,
    ID_CARD_PREVIEW,
    LOADING_AI,
    EXAM_ROOM,
    DIAGNOSTIC_RESULT
}

sealed interface CloudDataState<out T> {
    data object Loading : CloudDataState<Nothing>
    data class Success<T>(val data: T) : CloudDataState<T>
    data class Error(val message: String) : CloudDataState<Nothing>
}

data class TryoutUiState(
    val step: TryoutStep = TryoutStep.LANDING,
    val appMode: String = "nasional", // nasional | reverse | internasional | bk
    val participantName: String = "",
    val participantNisn: String = "",
    val participantSchool: String = "",
    val participantGrade: String = "",
    val participantPhotoBase64: String = "",
    val frameworkKey: String = "merdeka",
    val stage: String = "Fase F (Kelas 11–12 SMA/MA/SMK)",
    val subjectName: String = "Matematika Wajib",
    val topicFocus: String = "",
    val difficultyCode: String = "Standar",
    val scoringMethod: String = "polytomous",
    val totalTarget: Int = 10,
    val durationMinutes: Int = 30,
    val lang: String = "id",
    val cardIdCode: String = "NAS-4821",

    // AI Model & Profile Modals
    val selectedModel: String = "gemini-3.8-flash",
    val dualPassVerify: Boolean = false,
    val showSettingsModal: Boolean = false,
    val showProfileModal: Boolean = false,

    // Reverse Engineering & PDF state
    val kisiFileName: String = "",
    val sampleFileName: String = "",
    val pdfStartPage: Int = 1,
    val pdfEndPage: Int = 10,
    val isAnalyzingPdf: Boolean = false,
    val pdfAnalysisProgressMsg: String = "",
    val blueprint: BlueprintData? = null,

    // Active Exam Room state
    val questions: List<QuestionItem> = emptyList(),
    val answers: List<UserAnswerState> = emptyList(),
    val doubts: List<Boolean> = emptyList(),
    val currentIdx: Int = 0,
    val timerSeconds: Int = 1800,
    val isFetchingBackgroundBatch: Boolean = false,
    val loadingTitle: String = "Pak Rei Sedang Meracik Soal...",
    val loadingSubtitle: String = "Menyelaraskan indikator kurikulum, menyusun stimulus, dan memverifikasi kunci jawaban.",
    val showFinishConfirmModal: Boolean = false,

    // Diagnostic Result state
    val resultScorePercent: Int = 0,
    val resultEarnedPoints: Double = 0.0,
    val resultMaxPoints: Double = 0.0,
    val resultDurationMinutes: Int = 1,
    val resultAbilityIndex: Int = 500,
    val resultLogitIndex: Double = 0.0,
    val resultScaleBadge: String = "",
    val resultScaleDisclaimer: String = "",
    val resultCogStats: List<DimensionStat> = emptyList(),
    val resultTopicStats: List<DimensionStat> = emptyList(),
    val resultPakReiComment: String = "Menghubungkan ke inti kognitif...",
    val resultSummaries: List<PakReiSummary> = emptyList(),
    val resultReviewItems: List<QuestionReviewItem> = emptyList(),
    val isGeneratingAnalysis: Boolean = false,

    // Alert / Error Dialog
    val alertMessage: String? = null,
    val syncStatusText: String = "Cloud Sync Aktif"
)

class TryoutSessionViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val activeSessionRepository: ActiveSessionRepository,
    private val examResultRepository: ExamResultRepository,
    private val blueprintRepository: BlueprintRepository,
    private val questionGeneratorService: QuestionGeneratorService,
    private val blueprintExtractorService: BlueprintExtractorService,
    private val diagnosticAnalyzerService: DiagnosticAnalyzerService,
    private val pdfPageRendererService: PdfPageRendererService = AndroidPdfPageRenderer(),
    private val profilePhotoService: ProfilePhotoService = ProfilePhotoProcessor(),
    private val calculateDiagnosticReportUseCase: CalculateDiagnosticReportUseCase = CalculateDiagnosticReportUseCase(),
    private val currentUserId: String,
    private val defaultParticipantName: String = "",
    private val defaultEmail: String = ""
) : ViewModel() {

    constructor(
        cloudRepository: CloudSyncRepository,
        aiRepository: PakReiAiLogicRepository,
        currentUserId: String,
        defaultParticipantName: String = "",
        defaultEmail: String = ""
    ) : this(
        userProfileRepository = cloudRepository,
        activeSessionRepository = cloudRepository,
        examResultRepository = cloudRepository,
        blueprintRepository = cloudRepository,
        questionGeneratorService = aiRepository,
        blueprintExtractorService = aiRepository,
        diagnosticAnalyzerService = aiRepository,
        pdfPageRendererService = AndroidPdfPageRenderer(),
        profilePhotoService = ProfilePhotoProcessor(),
        calculateDiagnosticReportUseCase = CalculateDiagnosticReportUseCase(),
        currentUserId = currentUserId,
        defaultParticipantName = defaultParticipantName,
        defaultEmail = defaultEmail
    )

    private val _uiState = MutableStateFlow(
        TryoutUiState(participantName = defaultParticipantName)
    )
    val uiState: StateFlow<TryoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var backgroundFetchJob: Job? = null
    private var kisiBitmapsCache: List<Bitmap> = emptyList()
    private var sampleBitmapsCache: List<Bitmap> = emptyList()

    // Real-time Firestore stream of the user's profile
    val userProfileState: StateFlow<CloudDataState<UserProfile?>> =
        userProfileRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, CloudDataState<UserProfile?>> { profile ->
                if (profile != null) {
                    _uiState.update { state ->
                        state.copy(
                            participantName = profile.displayName.ifBlank { state.participantName },
                            participantNisn = profile.nisn.ifBlank { state.participantNisn },
                            participantSchool = profile.institution.ifBlank { state.participantSchool },
                            participantGrade = profile.gradeClass.ifBlank { state.participantGrade },
                            participantPhotoBase64 = profile.photoBase64.ifBlank { state.participantPhotoBase64 }
                        )
                    }
                }
                CloudDataState.Success(profile)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", rootError)
                emit(CloudDataState.Error(rootError.message ?: "Gagal memuat profil pengguna"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudDataState.Loading
            )

    // Real-time Firestore stream of the user's login audit history
    val loginHistoryState: StateFlow<CloudDataState<List<LoginHistoryRecord>>> =
        userProfileRepository.observeLoginHistory(currentUserId)
            .map<List<LoginHistoryRecord>, CloudDataState<List<LoginHistoryRecord>>> { CloudDataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing login history", rootError)
                emit(CloudDataState.Error(rootError.message ?: "Gagal memuat riwayat login"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudDataState.Loading
            )

    // Real-time Firestore stream of the user's active session
    val activeCloudSession: StateFlow<CloudDataState<ActiveExamSession?>> =
        activeSessionRepository.observeActiveSession(currentUserId)
            .map<ActiveExamSession?, CloudDataState<ActiveExamSession?>> { CloudDataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing active session", rootError)
                emit(CloudDataState.Error(rootError.message ?: "Gagal memuat sesi aktif dari Cloud"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudDataState.Loading
            )

    // Real-time Firestore stream of completed exam results
    val examResultsState: StateFlow<CloudDataState<List<ExamResultRecord>>> =
        examResultRepository.observeExamResults(currentUserId)
            .map<List<ExamResultRecord>, CloudDataState<List<ExamResultRecord>>> { CloudDataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing exam results", rootError)
                emit(CloudDataState.Error(rootError.message ?: "Gagal memuat riwayat hasil ujian"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudDataState.Loading
            )

    // Real-time Firestore stream of saved blueprints
    val savedBlueprintsState: StateFlow<CloudDataState<List<SavedBlueprintRecord>>> =
        blueprintRepository.observeSavedBlueprints(currentUserId)
            .map<List<SavedBlueprintRecord>, CloudDataState<List<SavedBlueprintRecord>>> { CloudDataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing blueprints", rootError)
                emit(CloudDataState.Error(rootError.message ?: "Gagal memuat koleksi blueprint"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudDataState.Loading
            )

    init {
        syncAuthenticatedUserToDatabase()
    }

    private fun syncAuthenticatedUserToDatabase() {
        viewModelScope.launch {
            val (method, resolvedEmail, resolvedName) = PendingAuthProfileStore.consumeOrDefault(
                fallbackEmail = defaultEmail,
                fallbackDisplayName = defaultParticipantName
            )
            val nisn = PendingAuthProfileStore.pendingNisn
            val inst = PendingAuthProfileStore.pendingInstitution
            val grade = PendingAuthProfileStore.pendingGradeClass
            val photo = PendingAuthProfileStore.pendingPhotoBase64
            val deviceModel = "Android ${Build.VERSION.RELEASE ?: ""} (${Build.MODEL ?: "Device"})".trim()

            _uiState.update { state ->
                state.copy(
                    participantName = state.participantName.ifBlank { resolvedName },
                    participantNisn = if (nisn.isNotBlank()) nisn else state.participantNisn,
                    participantSchool = if (inst.isNotBlank()) inst else state.participantSchool,
                    participantGrade = if (grade.isNotBlank()) grade else state.participantGrade,
                    participantPhotoBase64 = if (photo.isNotBlank()) photo else state.participantPhotoBase64
                )
            }

            userProfileRepository.recordUserLoginAndProfile(
                email = resolvedEmail,
                displayName = resolvedName,
                authMethod = method,
                institution = inst,
                deviceInfo = deviceModel,
                nisn = nisn,
                gradeClass = grade,
                photoBase64 = photo
            )
        }
    }

    fun toggleProfileModal(show: Boolean) {
        _uiState.update { it.copy(showProfileModal = show) }
    }

    fun saveProfileInfo(
        displayName: String,
        institution: String,
        nisn: String = "",
        gradeClass: String = "",
        photoBase64: String = ""
    ) {
        val cleanName = displayName.trim().ifBlank { _uiState.value.participantName.ifBlank { "Peserta Evaluasi" } }
        val cleanInst = institution.trim()
        val cleanNisn = nisn.trim()
        val cleanGrade = gradeClass.trim()
        val cleanPhoto = photoBase64.trim()

        _uiState.update { state ->
            state.copy(
                participantName = cleanName,
                participantNisn = cleanNisn,
                participantSchool = cleanInst,
                participantGrade = cleanGrade,
                participantPhotoBase64 = if (cleanPhoto.isNotBlank()) cleanPhoto else state.participantPhotoBase64,
                showProfileModal = false
            )
        }
        viewModelScope.launch {
            userProfileRepository.updateUserProfileInfo(
                displayName = cleanName,
                institution = cleanInst,
                nisn = cleanNisn,
                gradeClass = cleanGrade,
                photoBase64 = cleanPhoto
            )
        }
    }

    fun encodeProfilePhoto(context: Context, uri: Uri, onEncoded: (String) -> Unit) {
        viewModelScope.launch {
            profilePhotoService.encodePhotoUriToBase64(context, uri)
                .onSuccess { base64 -> onEncoded(base64) }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(alertMessage = err.localizedMessage ?: "Gagal memproses foto profil.")
                    }
                }
        }
    }

    // ================= NAVIGATION & MODE SELECTION =================

    fun selectMode(mode: String) {
        val availableFrameworks = CurriculumCatalog.frameworksForMode(mode)
        val defaultFw = when (mode) {
            "reverse" -> CurriculumCatalog.FRAMEWORKS["tka"] ?: availableFrameworks.first()
            else -> availableFrameworks.first()
        }
        val currentGrade = _uiState.value.participantGrade
        val matchedStage = CurriculumCatalog.matchStageForGrade(defaultFw.key, currentGrade)
            .ifBlank { defaultFw.stages.keys.first() }
        val firstSubject = defaultFw.stages[matchedStage]?.firstOrNull().orEmpty()

        _uiState.update { state ->
            state.copy(
                step = TryoutStep.REGISTRATION,
                appMode = mode,
                frameworkKey = defaultFw.key,
                stage = matchedStage,
                subjectName = firstSubject,
                lang = if (mode == "internasional") "en" else "id",
                totalTarget = if (mode == "bk") 10 else 10,
                durationMinutes = if (mode == "bk") 25 else 30,
                blueprint = if (mode == "reverse") state.blueprint else null
            )
        }
    }

    fun backToLanding() {
        timerJob?.cancel()
        backgroundFetchJob?.cancel()
        _uiState.update { it.copy(step = TryoutStep.LANDING, showFinishConfirmModal = false) }
    }

    fun backToRegistrationForm() {
        _uiState.update { it.copy(step = TryoutStep.REGISTRATION) }
    }

    // ================= FORM CONFIG HANDLERS =================

    fun updateParticipantName(name: String) {
        _uiState.update { it.copy(participantName = name) }
    }

    fun selectFramework(fwKey: String) {
        val fw = CurriculumCatalog.FRAMEWORKS[fwKey] ?: return
        val matchedStage = CurriculumCatalog.matchStageForGrade(fwKey, _uiState.value.participantGrade)
            .ifBlank { fw.stages.keys.first() }
        val firstSubject = fw.stages[matchedStage]?.firstOrNull().orEmpty()
        _uiState.update {
            it.copy(
                frameworkKey = fwKey,
                stage = matchedStage,
                subjectName = firstSubject
            )
        }
    }

    fun selectStage(stageName: String) {
        val fw = CurriculumCatalog.FRAMEWORKS[_uiState.value.frameworkKey] ?: return
        val subjects = fw.stages[stageName].orEmpty()
        val nextSubject = if (_uiState.value.subjectName in subjects) {
            _uiState.value.subjectName
        } else {
            subjects.firstOrNull().orEmpty()
        }
        _uiState.update {
            it.copy(stage = stageName, subjectName = nextSubject)
        }
    }

    fun updateSubjectName(subject: String) {
        _uiState.update { it.copy(subjectName = subject) }
    }

    fun updateTopicFocus(focus: String) {
        _uiState.update { it.copy(topicFocus = focus) }
    }

    fun updateDifficulty(diff: String) {
        _uiState.update { it.copy(difficultyCode = diff) }
    }

    fun updateScoringMethod(method: String) {
        _uiState.update { it.copy(scoringMethod = method) }
    }

    fun updateTotalTarget(count: Int) {
        _uiState.update { it.copy(totalTarget = count.coerceIn(4, 40)) }
    }

    fun updateDurationMinutes(mins: Int) {
        _uiState.update { it.copy(durationMinutes = mins.coerceIn(5, 240)) }
    }

    fun updateLanguage(lang: String) {
        _uiState.update { it.copy(lang = lang) }
    }

    fun updatePdfPageRange(startPage: Int, endPage: Int) {
        val s = max(1, startPage)
        val e = max(s, endPage)
        _uiState.update { it.copy(pdfStartPage = s, pdfEndPage = e) }
    }

    fun toggleSettingsModal(show: Boolean) {
        _uiState.update { it.copy(showSettingsModal = show) }
    }

    fun saveAiModelSettings(modelName: String, dualPass: Boolean) {
        _uiState.update {
            it.copy(
                selectedModel = modelName,
                dualPassVerify = dualPass,
                showSettingsModal = false
            )
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    // ================= PDF EXTRACTION & BLUEPRINT WORKBENCH =================

    fun onPdfSelected(context: Context, uri: Uri, isSample: Boolean, displayName: String) {
        viewModelScope.launch {
            _uiState.update {
                if (isSample) it.copy(sampleFileName = displayName)
                else it.copy(kisiFileName = displayName)
            }
            val bitmaps = pdfPageRendererService.renderPages(
                context = context,
                uri = uri,
                startPage = _uiState.value.pdfStartPage,
                maxPages = if (isSample) 2 else 4
            )
            if (isSample) {
                sampleBitmapsCache = bitmaps
            } else {
                kisiBitmapsCache = bitmaps
            }
        }
    }

    fun runReverseEngineeringPdf() {
        val state = _uiState.value
        if (kisiBitmapsCache.isEmpty()) {
            // Load rich sample blueprint if no PDF uploaded yet
            val sampleBp = blueprintExtractorService.getSampleBlueprint(state.frameworkKey, state.subjectName)
            _uiState.update {
                it.copy(
                    blueprint = sampleBp,
                    alertMessage = "Menampilkan Blueprint Kisi-Kisi contoh siap pakai. Anda juga dapat memilih file PDF dari penyimpanan perangkat untuk dibedah dengan AI Vision."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzingPdf = true,
                    pdfAnalysisProgressMsg = "AI Vision Pak Rei sedang membedah matriks indikator & DNA gaya soal..."
                )
            }
            val result = blueprintExtractorService.reverseEngineerPdfBlueprint(
                modelName = state.selectedModel,
                frameworkKey = state.frameworkKey,
                stage = state.stage,
                targetQCount = state.totalTarget,
                kisiBitmaps = kisiBitmapsCache,
                sampleBitmaps = sampleBitmapsCache
            )
            result.fold(
                onSuccess = { bp ->
                    _uiState.update {
                        it.copy(
                            isAnalyzingPdf = false,
                            pdfAnalysisProgressMsg = "",
                            blueprint = bp,
                            subjectName = if (it.subjectName.isBlank()) bp.detectedSubject else it.subjectName
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAnalyzingPdf = false,
                            pdfAnalysisProgressMsg = "",
                            alertMessage = "Gagal membedah PDF Kisi-Kisi: ${err.localizedMessage ?: "Periksa koneksi internet Anda."}"
                        )
                    }
                }
            )
        }
    }

    fun updateBlueprintRow(index: Int, updated: BlueprintRow) {
        val currentBp = _uiState.value.blueprint ?: return
        val newList = currentBp.items.toMutableList()
        if (index in newList.indices) {
            newList[index] = updated
            val totalCount = newList.sumOf { max(1, it.count) }
            _uiState.update {
                it.copy(
                    blueprint = currentBp.copy(items = newList),
                    totalTarget = totalCount
                )
            }
        }
    }

    fun addBlueprintRow() {
        val state = _uiState.value
        val fw = CurriculumCatalog.FRAMEWORKS[state.frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["merdeka"]!!
        val currentBp = state.blueprint ?: blueprintExtractorService.getSampleBlueprint(state.frameworkKey, state.subjectName)
        val newList = currentBp.items + BlueprintRow(
            domain = "Topik Baru",
            indicator = "Disajikan kasus kontekstual, peserta didik dapat menganalisis...",
            cognitiveLevel = fw.cogLevels.first(),
            questionType = "pilgan",
            count = 1
        )
        _uiState.update {
            it.copy(
                blueprint = currentBp.copy(items = newList),
                totalTarget = newList.sumOf { r -> max(1, r.count) }
            )
        }
    }

    fun removeBlueprintRow(index: Int) {
        val currentBp = _uiState.value.blueprint ?: return
        val newList = currentBp.items.toMutableList()
        if (index in newList.indices && newList.size > 1) {
            newList.removeAt(index)
            _uiState.update {
                it.copy(
                    blueprint = currentBp.copy(items = newList),
                    totalTarget = newList.sumOf { r -> max(1, r.count) }
                )
            }
        }
    }

    fun saveCurrentBlueprintToCloud() {
        val state = _uiState.value
        val bp = state.blueprint ?: return
        viewModelScope.launch {
            val totalCount = bp.items.sumOf { max(1, it.count) }
            val record = SavedBlueprintRecord(
                userId = currentUserId,
                title = "Blueprint ${state.subjectName.ifBlank { bp.detectedSubject }}",
                detectedSubject = state.subjectName.ifBlank { bp.detectedSubject },
                frameworkKey = state.frameworkKey,
                stage = state.stage,
                styleDNA = bp.styleDNA,
                totalItemsCount = totalCount.toLong(),
                itemsPayload = PsychometricEngine.serializeBlueprintItems(bp.items)
            )
            val res = blueprintRepository.saveBlueprint(record)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(alertMessage = "Blueprint Kisi-Kisi berhasil disimpan ke Cloud Firestore!")
                }
            } else {
                _uiState.update {
                    it.copy(alertMessage = "Gagal menyimpan Blueprint ke Cloud: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
                }
            }
        }
    }

    fun applySavedBlueprint(record: SavedBlueprintRecord) {
        val rows = PsychometricEngine.deserializeBlueprintItems(record.itemsPayload)
        if (rows.isEmpty()) return
        val bp = BlueprintData(
            detectedSubject = record.detectedSubject,
            styleDNA = record.styleDNA,
            items = rows
        )
        _uiState.update {
            it.copy(
                step = TryoutStep.REGISTRATION,
                appMode = "reverse",
                frameworkKey = record.frameworkKey,
                stage = record.stage,
                subjectName = record.detectedSubject,
                totalTarget = rows.sumOf { r -> max(1, r.count) },
                blueprint = bp
            )
        }
    }

    fun deleteSavedBlueprint(blueprintId: String) {
        viewModelScope.launch {
            blueprintRepository.deleteBlueprint(blueprintId)
        }
    }

    // ================= ID CARD COMPILATION & EXAM INITIATION =================

    fun compileIdentityCard() {
        val state = _uiState.value
        val resolvedName = state.participantName
            .ifBlank { defaultParticipantName }
            .ifBlank { "Peserta Evaluasi" }

        var bp = state.blueprint
        if (state.appMode == "reverse" && bp == null) {
            bp = blueprintExtractorService.getSampleBlueprint(state.frameworkKey, state.subjectName)
        }
        val finalTarget = if (state.appMode == "reverse" && bp != null && bp.items.isNotEmpty()) {
            bp.items.sumOf { max(1, it.count) }
        } else {
            state.totalTarget
        }

        val prefix = when (state.appMode) {
            "reverse" -> "REV"
            "internasional" -> "INT"
            "bk" -> "BK"
            else -> "NAS"
        }
        val randomNum = Random.nextInt(1000, 9999)

        _uiState.update {
            it.copy(
                step = TryoutStep.ID_CARD_PREVIEW,
                participantName = resolvedName,
                blueprint = bp,
                totalTarget = finalTarget,
                cardIdCode = "$prefix-$randomNum"
            )
        }
    }

    fun startAiExamProcess() {
        val state = _uiState.value
        val fw = CurriculumCatalog.FRAMEWORKS[state.frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["merdeka"]!!

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    step = TryoutStep.LOADING_AI,
                    loadingTitle = "Pak Rei Sedang Meracik Soal...",
                    loadingSubtitle = "Menyusun instrumen ${state.subjectName} (${fw.name}) dengan ${state.selectedModel}..."
                )
            }

            val firstBatchSize = min(10, state.totalTarget)
            val batchResult = questionGeneratorService.generateQuestionBatch(
                modelName = state.selectedModel,
                dualPassVerify = state.dualPassVerify,
                appMode = state.appMode,
                frameworkKey = state.frameworkKey,
                stage = state.stage,
                subjectName = state.subjectName,
                topicFocus = state.topicFocus,
                difficultyCode = state.difficultyCode,
                lang = state.lang,
                blueprint = state.blueprint,
                startIdx = 0,
                amountNeeded = firstBatchSize
            )

            batchResult.fold(
                onSuccess = { firstBatch ->
                    val initialQuestions = firstBatch.take(firstBatchSize)
                    val target = state.totalTarget
                    val initialAnswers = List(target) { UserAnswerState() }
                    val initialDoubts = List(target) { false }
                    val initialSeconds = state.durationMinutes * 60

                    _uiState.update {
                        it.copy(
                            step = TryoutStep.EXAM_ROOM,
                            questions = initialQuestions,
                            answers = initialAnswers,
                            doubts = initialDoubts,
                            currentIdx = 0,
                            timerSeconds = initialSeconds
                        )
                    }
                    syncActiveSessionToCloud()
                    startExamTimer()
                    if (initialQuestions.size < target) {
                        triggerBackgroundBatchFetch()
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            step = TryoutStep.ID_CARD_PREVIEW,
                            alertMessage = "Gagal meracik soal via Firebase AI Logic: ${err.localizedMessage ?: "Periksa koneksi Anda atau gunakan tombol Simulasi Cepat."}"
                        )
                    }
                }
            )
        }
    }

    fun startDemoExamProcess() {
        val state = _uiState.value
        val demoList = questionGeneratorService.getDemoQuestions(state.appMode)
        val target = demoList.size
        val initialSeconds = state.durationMinutes * 60

        _uiState.update {
            it.copy(
                step = TryoutStep.EXAM_ROOM,
                participantName = state.participantName.ifBlank { "Peserta Simulasi" },
                subjectName = state.subjectName.ifBlank { "Simulasi Terpadu" },
                totalTarget = target,
                questions = demoList,
                answers = List(target) { UserAnswerState() },
                doubts = List(target) { false },
                currentIdx = 0,
                timerSeconds = initialSeconds
            )
        }
        syncActiveSessionToCloud()
        startExamTimer()
    }

    fun resumeCloudSession(session: ActiveExamSession) {
        val restoredQuestions = PsychometricEngine.deserializeQuestions(session.questionsPayload)
        if (restoredQuestions.isEmpty()) {
            discardCloudSession()
            return
        }
        val target = max(restoredQuestions.size, session.totalTarget.toInt())
        val restoredAnswers = PsychometricEngine.deserializeAnswers(session.answersPayload, target)
        val restoredDoubts = PsychometricEngine.deserializeDoubts(session.doubtsPayload, target)
        val restoredIdx = session.currentIdx.toInt().coerceIn(0, restoredQuestions.size - 1)
        val restoredSeconds = max(60, session.timerSeconds.toInt())

        _uiState.update {
            it.copy(
                step = TryoutStep.EXAM_ROOM,
                appMode = session.appMode,
                participantName = session.participantName,
                frameworkKey = session.frameworkKey,
                stage = session.stage,
                subjectName = session.subjectName,
                topicFocus = session.topicFocus,
                difficultyCode = session.difficultyCode,
                scoringMethod = session.scoringMethod,
                lang = session.lang,
                durationMinutes = session.durationMinutes.toInt(),
                totalTarget = target,
                questions = restoredQuestions,
                answers = restoredAnswers,
                doubts = restoredDoubts,
                currentIdx = restoredIdx,
                timerSeconds = restoredSeconds
            )
        }
        startExamTimer()
        if (restoredQuestions.size < target) {
            triggerBackgroundBatchFetch()
        }
    }

    fun discardCloudSession() {
        viewModelScope.launch {
            activeSessionRepository.clearActiveSession("current")
        }
    }

    private fun triggerBackgroundBatchFetch() {
        if (backgroundFetchJob?.isActive == true) return
        backgroundFetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetchingBackgroundBatch = true) }
            var attempts = 0
            while (_uiState.value.questions.size < _uiState.value.totalTarget &&
                attempts < 4 &&
                _uiState.value.step == TryoutStep.EXAM_ROOM
            ) {
                val curState = _uiState.value
                val needed = min(10, curState.totalTarget - curState.questions.size)
                val res = questionGeneratorService.generateQuestionBatch(
                    modelName = curState.selectedModel,
                    dualPassVerify = curState.dualPassVerify,
                    appMode = curState.appMode,
                    frameworkKey = curState.frameworkKey,
                    stage = curState.stage,
                    subjectName = curState.subjectName,
                    topicFocus = curState.topicFocus,
                    difficultyCode = curState.difficultyCode,
                    lang = curState.lang,
                    blueprint = curState.blueprint,
                    startIdx = curState.questions.size,
                    amountNeeded = needed
                )
                val newBatch = res.getOrNull()
                if (!newBatch.isNullOrEmpty()) {
                    _uiState.update { st ->
                        val merged = (st.questions + newBatch).take(st.totalTarget)
                        st.copy(questions = merged)
                    }
                    syncActiveSessionToCloud()
                } else {
                    break
                }
                attempts++
            }
            _uiState.update { st ->
                val adjustedTarget = if (st.questions.isNotEmpty() && st.questions.size < st.totalTarget) {
                    st.questions.size
                } else {
                    st.totalTarget
                }
                st.copy(isFetchingBackgroundBatch = false, totalTarget = adjustedTarget)
            }
        }
    }

    private fun startExamTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var tickCount = 0
            while (_uiState.value.step == TryoutStep.EXAM_ROOM && _uiState.value.timerSeconds > 0) {
                delay(1000L)
                tickCount++
                _uiState.update { it.copy(timerSeconds = max(0, it.timerSeconds - 1)) }
                if (tickCount % 15 == 0) {
                    syncActiveSessionToCloud()
                }
                if (_uiState.value.timerSeconds <= 0) {
                    confirmFinishExam()
                    break
                }
            }
        }
    }

    private fun syncActiveSessionToCloud() {
        val st = _uiState.value
        if (st.step != TryoutStep.EXAM_ROOM || st.questions.isEmpty()) return
        val fw = CurriculumCatalog.FRAMEWORKS[st.frameworkKey]
        val session = ActiveExamSession(
            id = "current",
            userId = currentUserId,
            appMode = st.appMode,
            participantName = st.participantName.ifBlank { "Peserta" },
            frameworkKey = st.frameworkKey,
            frameworkName = fw?.name ?: st.frameworkKey,
            stage = st.stage,
            subjectName = st.subjectName.ifBlank { "Mata Uji" },
            topicFocus = st.topicFocus,
            difficultyCode = st.difficultyCode,
            scoringMethod = st.scoringMethod,
            lang = st.lang,
            durationMinutes = st.durationMinutes.toLong(),
            totalTarget = st.totalTarget.toLong(),
            currentIdx = st.currentIdx.toLong(),
            timerSeconds = st.timerSeconds.toLong(),
            questionsPayload = PsychometricEngine.serializeQuestions(st.questions),
            answersPayload = PsychometricEngine.serializeAnswers(st.answers),
            doubtsPayload = PsychometricEngine.serializeDoubts(st.doubts)
        )
        viewModelScope.launch {
            val res = activeSessionRepository.saveActiveSession(session)
            _uiState.update {
                it.copy(syncStatusText = if (res.isSuccess) "Tersinkron ke Cloud" else "Mode Offline Sementara")
            }
        }
    }

    // ================= EXAM ROOM QUESTION INTERACTIONS =================

    fun jumpToQuestion(index: Int) {
        val st = _uiState.value
        if (index in st.questions.indices) {
            _uiState.update { it.copy(currentIdx = index) }
            syncActiveSessionToCloud()
        }
    }

    fun navigatePrevNext(delta: Int) {
        val st = _uiState.value
        if (delta > 0 && st.currentIdx == st.totalTarget - 1) {
            requestFinishExam()
            return
        }
        val nextIdx = (st.currentIdx + delta).coerceIn(0, max(0, st.questions.size - 1))
        _uiState.update { it.copy(currentIdx = nextIdx) }
        syncActiveSessionToCloud()
        if (nextIdx >= st.questions.size - 3 && st.questions.size < st.totalTarget) {
            triggerBackgroundBatchFetch()
        }
    }

    fun toggleCurrentDoubt() {
        val st = _uiState.value
        val idx = st.currentIdx
        if (idx !in st.doubts.indices) return
        val updated = st.doubts.toMutableList()
        updated[idx] = !updated[idx]
        _uiState.update { it.copy(doubts = updated) }
        syncActiveSessionToCloud()
    }

    fun selectSingleOption(optionIndex: Int) {
        val st = _uiState.value
        val idx = st.currentIdx
        if (idx !in st.answers.indices) return
        val updated = st.answers.toMutableList()
        updated[idx] = updated[idx].copy(singleChoice = optionIndex)
        _uiState.update { it.copy(answers = updated) }
        syncActiveSessionToCloud()
    }

    fun toggleKompleksOption(optionIndex: Int) {
        val st = _uiState.value
        val idx = st.currentIdx
        if (idx !in st.answers.indices) return
        val updated = st.answers.toMutableList()
        val currentSet = updated[idx].multiChoices.toMutableList()
        if (optionIndex in currentSet) currentSet.remove(optionIndex) else currentSet.add(optionIndex)
        updated[idx] = updated[idx].copy(multiChoices = currentSet.sorted())
        _uiState.update { it.copy(answers = updated) }
        syncActiveSessionToCloud()
    }

    fun selectBenarSalahStatement(statementIndex: Int, value: Boolean) {
        val st = _uiState.value
        val idx = st.currentIdx
        val q = st.questions.getOrNull(idx) ?: return
        if (idx !in st.answers.indices) return
        val updated = st.answers.toMutableList()
        val currentBools = MutableList(q.statements.size) { i ->
            updated[idx].boolChoices.getOrNull(i)
        }
        if (statementIndex in currentBools.indices) {
            currentBools[statementIndex] = value
        }
        updated[idx] = updated[idx].copy(boolChoices = currentBools)
        _uiState.update { it.copy(answers = updated) }
        syncActiveSessionToCloud()
    }

    fun updateIsianText(textValue: String) {
        val st = _uiState.value
        val idx = st.currentIdx
        if (idx !in st.answers.indices) return
        val updated = st.answers.toMutableList()
        updated[idx] = updated[idx].copy(textAnswer = textValue)
        _uiState.update { it.copy(answers = updated) }
    }

    fun requestFinishExam() {
        _uiState.update { it.copy(showFinishConfirmModal = true) }
    }

    fun dismissFinishModal() {
        _uiState.update { it.copy(showFinishConfirmModal = false) }
    }

    // ================= FINISH & DIAGNOSTIC REPORT GENERATION =================

    fun confirmFinishExam() {
        timerJob?.cancel()
        backgroundFetchJob?.cancel()

        val st = _uiState.value
        val fw = CurriculumCatalog.FRAMEWORKS[st.frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["merdeka"]!!
        val elapsedMinutes = max(1, ((st.durationMinutes * 60) - st.timerSeconds) / 60)

        // Delegate psychometric calculation to single-responsibility use case (SRP)
        val summary = calculateDiagnosticReportUseCase.execute(
            appMode = st.appMode,
            frameworkKey = st.frameworkKey,
            scoringMethod = st.scoringMethod,
            questions = st.questions,
            answers = st.answers
        )

        _uiState.update {
            it.copy(
                step = TryoutStep.DIAGNOSTIC_RESULT,
                showFinishConfirmModal = false,
                resultScorePercent = summary.scorePercent,
                resultEarnedPoints = summary.earnedPoints,
                resultMaxPoints = summary.maxPoints,
                resultDurationMinutes = elapsedMinutes,
                resultAbilityIndex = summary.abilityIndex,
                resultLogitIndex = summary.logitIndex,
                resultScaleBadge = summary.scaleBadge,
                resultScaleDisclaimer = summary.scaleDisclaimer,
                resultCogStats = summary.cogStats,
                resultTopicStats = summary.topicStats,
                resultReviewItems = summary.reviewItems,
                resultPakReiComment = "Pak Rei sedang menganalisis pola respon kognitifmu...",
                resultSummaries = emptyList(),
                isGeneratingAnalysis = true
            )
        }

        viewModelScope.launch {
            activeSessionRepository.clearActiveSession("current")

            val (comment, summaries) = diagnosticAnalyzerService.generatePakReiAnalysis(
                modelName = st.selectedModel,
                appMode = st.appMode,
                participantName = st.participantName.ifBlank { "Peserta" },
                frameworkName = fw.name,
                stage = st.stage,
                subjectName = st.subjectName,
                scorePercent = summary.scorePercent,
                mistakesList = summary.uniqueMistakeTopics,
                cogStats = summary.cogStats
            )

            _uiState.update {
                it.copy(
                    resultPakReiComment = comment,
                    resultSummaries = summaries,
                    isGeneratingAnalysis = false
                )
            }

            // Persist completed result record to Cloud Firestore
            val record = ExamResultRecord(
                userId = currentUserId,
                appMode = st.appMode,
                participantName = st.participantName.ifBlank { "Peserta" },
                frameworkKey = st.frameworkKey,
                frameworkName = fw.name,
                stage = st.stage,
                subjectName = st.subjectName.ifBlank { "Mata Uji" },
                scoringMethod = st.scoringMethod,
                scorePercent = summary.scorePercent.toLong(),
                earnedPoints = summary.earnedPoints,
                maxPoints = summary.maxPoints,
                abilityIndexScore = summary.abilityIndex.toLong(),
                logitIndex = summary.logitIndex,
                scaleBadge = summary.scaleBadge,
                scaleDisclaimer = summary.scaleDisclaimer,
                durationTakenMinutes = elapsedMinutes.toLong(),
                pakReiComment = comment,
                radarPayload = PsychometricEngine.serializeDimensionStats(summary.cogStats),
                topicPayload = PsychometricEngine.serializeDimensionStats(summary.topicStats),
                summariesPayload = PsychometricEngine.serializeSummaries(summaries),
                reviewPayload = PsychometricEngine.serializeReviewItems(summary.reviewItems)
            )
            examResultRepository.saveExamResult(record)
        }
    }

    fun openSavedResultDetail(record: ExamResultRecord) {
        val cogStats = PsychometricEngine.deserializeDimensionStats(record.radarPayload)
        val topicStats = PsychometricEngine.deserializeDimensionStats(record.topicPayload)
        val summaries = PsychometricEngine.deserializeSummaries(record.summariesPayload)
        val reviews = PsychometricEngine.deserializeReviewItems(record.reviewPayload)

        _uiState.update {
            it.copy(
                step = TryoutStep.DIAGNOSTIC_RESULT,
                appMode = record.appMode,
                participantName = record.participantName,
                frameworkKey = record.frameworkKey,
                stage = record.stage,
                subjectName = record.subjectName,
                scoringMethod = record.scoringMethod,
                resultScorePercent = record.scorePercent.toInt(),
                resultEarnedPoints = record.earnedPoints,
                resultMaxPoints = record.maxPoints,
                resultDurationMinutes = record.durationTakenMinutes.toInt(),
                resultAbilityIndex = record.abilityIndexScore.toInt(),
                resultLogitIndex = record.logitIndex,
                resultScaleBadge = record.scaleBadge,
                resultScaleDisclaimer = record.scaleDisclaimer,
                resultCogStats = cogStats,
                resultTopicStats = topicStats,
                resultPakReiComment = record.pakReiComment,
                resultSummaries = summaries,
                resultReviewItems = reviews,
                isGeneratingAnalysis = false
            )
        }
    }

    fun deleteHistoryRecord(resultId: String) {
        viewModelScope.launch {
            examResultRepository.deleteExamResult(resultId)
        }
    }
}
