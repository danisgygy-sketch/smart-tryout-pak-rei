package com.example.domain.contracts

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.ActiveExamSession
import com.example.model.BlueprintData
import com.example.model.DimensionStat
import com.example.model.ExamResultRecord
import com.example.model.ItemScoreResult
import com.example.model.LoginHistoryRecord
import com.example.model.PakReiSummary
import com.example.model.QuestionItem
import com.example.model.SavedBlueprintRecord
import com.example.model.UserAnswerState
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * ISP (Interface Segregation Principle) & DIP (Dependency Inversion Principle):
 * Segregated data access and domain service contracts so consumers only depend on
 * the narrow operations they actually require.
 */

interface UserProfileRepository {
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    fun observeLoginHistory(userId: String): Flow<List<LoginHistoryRecord>>
    suspend fun recordUserLoginAndProfile(
        email: String,
        displayName: String,
        authMethod: String,
        institution: String = "",
        deviceInfo: String = "Android",
        nisn: String = "",
        gradeClass: String = "",
        photoBase64: String = ""
    ): Result<String>
    suspend fun updateUserProfileInfo(
        displayName: String,
        institution: String,
        nisn: String = "",
        gradeClass: String = "",
        photoBase64: String = ""
    ): Result<Unit>
}

interface ProfilePhotoService {
    suspend fun encodePhotoUriToBase64(context: Context, uri: Uri): Result<String>
}

interface ActiveSessionRepository {
    fun observeActiveSession(userId: String): Flow<ActiveExamSession?>
    suspend fun saveActiveSession(session: ActiveExamSession): Result<String>
    suspend fun clearActiveSession(sessionId: String = "current"): Result<Unit>
}

interface ExamResultRepository {
    fun observeExamResults(userId: String): Flow<List<ExamResultRecord>>
    suspend fun saveExamResult(record: ExamResultRecord): Result<String>
    suspend fun getExamResultById(ownerUid: String, resultId: String): Result<ExamResultRecord?>
    suspend fun deleteExamResult(resultId: String): Result<Unit>
}

interface BlueprintRepository {
    fun observeSavedBlueprints(userId: String): Flow<List<SavedBlueprintRecord>>
    suspend fun saveBlueprint(blueprint: SavedBlueprintRecord): Result<String>
    suspend fun deleteBlueprint(blueprintId: String): Result<Unit>
}

interface QuestionGeneratorService {
    suspend fun generateQuestionBatch(
        modelName: String,
        dualPassVerify: Boolean,
        appMode: String,
        frameworkKey: String,
        stage: String,
        subjectName: String,
        topicFocus: String,
        difficultyCode: String,
        lang: String,
        blueprint: BlueprintData?,
        startIdx: Int,
        amountNeeded: Int
    ): Result<List<QuestionItem>>

    fun getDemoQuestions(appMode: String): List<QuestionItem>
}

interface BlueprintExtractorService {
    suspend fun reverseEngineerPdfBlueprint(
        modelName: String,
        frameworkKey: String,
        stage: String,
        targetQCount: Int,
        kisiBitmaps: List<Bitmap>,
        sampleBitmaps: List<Bitmap>
    ): Result<BlueprintData>

    fun getSampleBlueprint(frameworkKey: String, subjectName: String): BlueprintData
}

interface DiagnosticAnalyzerService {
    suspend fun generatePakReiAnalysis(
        modelName: String,
        appMode: String,
        participantName: String,
        frameworkName: String,
        stage: String,
        subjectName: String,
        scorePercent: Int,
        mistakesList: List<String>,
        cogStats: List<DimensionStat>
    ): Pair<String, List<PakReiSummary>>
}

interface PdfPageRendererService {
    suspend fun renderPages(
        context: Context,
        uri: Uri,
        startPage: Int,
        maxPages: Int
    ): List<Bitmap>
}

/**
 * OCP (Open/Closed Principle) & LSP (Liskov Substitution Principle) contracts
 * for extensible psychometric item scoring and curriculum scale equivalence.
 */
interface ItemScoringStrategy {
    val supportedTypes: Set<String>
    fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult
}

interface ScaleEquivalenceStrategy {
    fun supports(appMode: String, frameworkKey: String): Boolean
    fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String>
}
