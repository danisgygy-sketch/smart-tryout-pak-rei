package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PakReiAiLogicRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.AiModelSettingsDialog
import com.example.ui.screens.CloudBlueprintsScreen
import com.example.ui.screens.CloudHistoryScreen
import com.example.ui.screens.ConfigAndBlueprintScreen
import com.example.ui.screens.DiagnosticResultScreen
import com.example.ui.screens.ExamRoomScreen
import com.example.ui.screens.LoadingAiScreen
import com.example.ui.screens.ModeLandingScreen
import com.example.ui.screens.UserProfileAndLoginModal
import com.example.ui.theme.SmartTryoutPakReiTheme
import com.example.ui.viewmodel.CloudDataState
import com.example.ui.viewmodel.TryoutSessionViewModel
import com.example.ui.viewmodel.TryoutStep
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)
        setContent {
            SmartTryoutPakReiTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedTryoutShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedTryoutShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: TryoutSessionViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val db = FirebaseFirestore.getInstance(
                    app.getString(R.string.firestore_database_id)
                )
                TryoutSessionViewModel(
                    cloudRepository = CloudSyncRepository(db),
                    aiRepository = PakReiAiLogicRepository(),
                    currentUserId = user.uid,
                    defaultParticipantName = user.displayName ?: "",
                    defaultEmail = user.email ?: ""
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val loginHistoryState by viewModel.loginHistoryState.collectAsStateWithLifecycle()
    val activeSessionState by viewModel.activeCloudSession.collectAsStateWithLifecycle()
    val examResultsState by viewModel.examResultsState.collectAsStateWithLifecycle()
    val savedBlueprintsState by viewModel.savedBlueprintsState.collectAsStateWithLifecycle()

    // Navigation destination: 0 = Evaluasi, 1 = Riwayat, 2 = Blueprint
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val isInExamRoom = selectedTab == 0 && uiState.step == TryoutStep.EXAM_ROOM

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!isInExamRoom) {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Pak Rei Tryout",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Evaluasi & Diagnostik Akademik",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        viewModel.toggleProfileModal(true)
                                    },
                                    modifier = Modifier.testTag("top_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profil Pengguna"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.toggleSettingsModal(true)
                                    },
                                    modifier = Modifier.testTag("top_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Pengaturan AI"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        signOut(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Keluar Akun"
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
            bottomBar = {
                if (!isInExamRoom && !isExpandedScreen) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.School else Icons.Outlined.School,
                                        contentDescription = "Evaluasi"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Evaluasi",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_evaluasi")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Default.History else Icons.Outlined.History,
                                        contentDescription = "Riwayat"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Riwayat",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_riwayat_cloud")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Default.Description else Icons.Outlined.Description,
                                        contentDescription = "Blueprint"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Blueprint",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_blueprint_cloud")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                if (!isInExamRoom && isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.School else Icons.Outlined.School,
                                    contentDescription = "Evaluasi"
                                )
                            },
                            label = { Text("Evaluasi") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_evaluasi")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Default.History else Icons.Outlined.History,
                                    contentDescription = "Riwayat"
                                )
                            },
                            label = { Text("Riwayat") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_riwayat_cloud")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Default.Description else Icons.Outlined.Description,
                                    contentDescription = "Blueprint"
                                )
                            },
                            label = { Text("Blueprint") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_blueprint_cloud")
                        )
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = selectedTab,
                        animationSpec = tween(durationMillis = 180),
                        label = "MainTabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                Crossfade(
                                    targetState = uiState.step,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "TryoutStepTransition"
                                ) { currentStep ->
                                    when (currentStep) {
                                        TryoutStep.LANDING -> {
                                            ModeLandingScreen(
                                                uiState = uiState,
                                                userProfileState = userProfileState,
                                                loginHistoryState = loginHistoryState,
                                                activeSessionState = activeSessionState,
                                                examResultsState = examResultsState,
                                                onSelectMode = viewModel::selectMode,
                                                onQuickStartSubject = { mode, fwKey, subject ->
                                                    viewModel.selectMode(mode)
                                                    viewModel.selectFramework(fwKey)
                                                    viewModel.updateSubjectName(subject)
                                                },
                                                onResumeSession = viewModel::resumeCloudSession,
                                                onDiscardSession = viewModel::discardCloudSession,
                                                onOpenRecentResult = { record ->
                                                    viewModel.openSavedResultDetail(record)
                                                },
                                                onNavigateToHistory = { selectedTab = 1 },
                                                onOpenProfile = { viewModel.toggleProfileModal(true) },
                                                onDismissProfile = { viewModel.toggleProfileModal(false) },
                                                onOpenSettings = { viewModel.toggleSettingsModal(true) },
                                                onSaveSettings = viewModel::saveAiModelSettings,
                                                onDismissSettings = { viewModel.toggleSettingsModal(false) }
                                            )
                                        }

                                        TryoutStep.REGISTRATION, TryoutStep.ID_CARD_PREVIEW -> {
                                            ConfigAndBlueprintScreen(
                                                uiState = uiState,
                                                onBack = {
                                                    if (uiState.step == TryoutStep.ID_CARD_PREVIEW) {
                                                        viewModel.backToRegistrationForm()
                                                    } else {
                                                        viewModel.backToLanding()
                                                    }
                                                },
                                                onUpdateName = viewModel::updateParticipantName,
                                                onOpenAccountProfile = { viewModel.toggleProfileModal(true) },
                                                onSelectFramework = viewModel::selectFramework,
                                                onSelectStage = viewModel::selectStage,
                                                onUpdateSubject = viewModel::updateSubjectName,
                                                onUpdateTopicFocus = viewModel::updateTopicFocus,
                                                onUpdateDifficulty = viewModel::updateDifficulty,
                                                onUpdateScoringMethod = viewModel::updateScoringMethod,
                                                onUpdateTotalTarget = viewModel::updateTotalTarget,
                                                onUpdateDuration = viewModel::updateDurationMinutes,
                                                onUpdateLanguage = viewModel::updateLanguage,
                                                onPdfSelected = viewModel::onPdfSelected,
                                                onRunReverseEngineering = viewModel::runReverseEngineeringPdf,
                                                onUpdateBlueprintRow = viewModel::updateBlueprintRow,
                                                onAddBlueprintRow = viewModel::addBlueprintRow,
                                                onRemoveBlueprintRow = viewModel::removeBlueprintRow,
                                                onSaveBlueprintToCloud = viewModel::saveCurrentBlueprintToCloud,
                                                onCompileCard = viewModel::compileIdentityCard,
                                                onStartAiExam = viewModel::startAiExamProcess,
                                                onStartDemoExam = viewModel::startDemoExamProcess
                                            )
                                        }

                                        TryoutStep.LOADING_AI -> {
                                            LoadingAiScreen(
                                                title = uiState.loadingTitle,
                                                subtitle = uiState.loadingSubtitle
                                            )
                                        }

                                        TryoutStep.EXAM_ROOM -> {
                                            ExamRoomScreen(
                                                uiState = uiState,
                                                onJumpToQuestion = viewModel::jumpToQuestion,
                                                onNavigatePrevNext = viewModel::navigatePrevNext,
                                                onToggleDoubt = viewModel::toggleCurrentDoubt,
                                                onSelectSingleOption = viewModel::selectSingleOption,
                                                onToggleKompleksOption = viewModel::toggleKompleksOption,
                                                onSelectBenarSalah = viewModel::selectBenarSalahStatement,
                                                onUpdateIsianText = viewModel::updateIsianText,
                                                onRequestFinish = viewModel::requestFinishExam,
                                                onConfirmFinish = viewModel::confirmFinishExam,
                                                onDismissFinishModal = viewModel::dismissFinishModal
                                            )
                                        }

                                        TryoutStep.DIAGNOSTIC_RESULT -> {
                                            DiagnosticResultScreen(
                                                uiState = uiState,
                                                onStartNewSession = viewModel::backToLanding
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                CloudHistoryScreen(
                                    resultsState = examResultsState,
                                    onOpenResult = { record ->
                                        viewModel.openSavedResultDetail(record)
                                        selectedTab = 0
                                    },
                                    onDeleteResult = viewModel::deleteHistoryRecord
                                )
                            }

                            2 -> {
                                CloudBlueprintsScreen(
                                    blueprintsState = savedBlueprintsState,
                                    onUseBlueprint = { bpRecord ->
                                        viewModel.applySavedBlueprint(bpRecord)
                                        selectedTab = 0
                                    },
                                    onDeleteBlueprint = viewModel::deleteSavedBlueprint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Global Info Akun & Identitas Peserta Modal (Accessible from any screen)
    val currentProfile = (userProfileState as? CloudDataState.Success)?.data
    if (uiState.showProfileModal) {
        UserProfileAndLoginModal(
            profile = currentProfile,
            fallbackName = uiState.participantName.ifBlank { user.displayName ?: "Peserta" },
            fallbackNisn = uiState.participantNisn,
            fallbackSchool = uiState.participantSchool,
            fallbackGrade = uiState.participantGrade,
            fallbackPhotoBase64 = uiState.participantPhotoBase64,
            loginHistoryState = loginHistoryState,
            onEncodePhoto = viewModel::encodeProfilePhoto,
            onSave = { name, inst, nisn, grade, photo ->
                viewModel.saveProfileInfo(
                    displayName = name,
                    institution = inst,
                    nisn = nisn,
                    gradeClass = grade,
                    photoBase64 = photo
                )
            },
            onDismiss = { viewModel.toggleProfileModal(false) }
        )
    }

    // Global AI Model Settings Modal
    if (uiState.showSettingsModal) {
        AiModelSettingsDialog(
            currentModel = uiState.selectedModel,
            currentDualPass = uiState.dualPassVerify,
            onSave = viewModel::saveAiModelSettings,
            onDismiss = { viewModel.toggleSettingsModal(false) }
        )
    }

    // Global Alert / Notification Dialog
    if (uiState.alertMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAlert,
            title = {
                Text(
                    text = "Informasi Sistem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = uiState.alertMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::dismissAlert,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dismiss_alert_button")
                ) {
                    Text(
                        text = "Mengerti",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}
