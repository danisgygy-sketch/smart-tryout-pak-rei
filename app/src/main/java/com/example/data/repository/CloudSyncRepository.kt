package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.contracts.ActiveSessionRepository
import com.example.domain.contracts.BlueprintRepository
import com.example.domain.contracts.ExamResultRepository
import com.example.domain.contracts.UserProfileRepository
import com.example.model.ActiveExamSession
import com.example.model.ExamResultRecord
import com.example.model.LoginHistoryRecord
import com.example.model.SavedBlueprintRecord
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore
) : UserProfileRepository,
    ActiveSessionRepository,
    ExamResultRepository,
    BlueprintRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // ================= 0. USER PROFILE & LOGIN AUDIT DATABASE =================

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(): Flow<UserProfile?> = observeUserProfile(requireUserId())

    override fun observeLoginHistory(userId: String): Flow<List<LoginHistoryRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("login_logs")
        val path = "users/$userId/login_logs"
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LoginHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLoginHistory(): Flow<List<LoginHistoryRecord>> = observeLoginHistory(requireUserId())

    override suspend fun recordUserLoginAndProfile(
        email: String,
        displayName: String,
        authMethod: String,
        institution: String,
        deviceInfo: String,
        nisn: String,
        gradeClass: String,
        photoBase64: String
    ): Result<String> {
        val uid = requireUserId()
        val normalizedMethod = if (authMethod in listOf("google", "email", "facebook")) authMethod else "google"
        val cleanName = displayName.trim().ifBlank { "Peserta Evaluasi" }.take(140)
        val cleanEmail = email.trim().ifBlank { "pengguna@smarttryout.id" }.take(180)
        val cleanNisn = nisn.trim().take(36)
        val cleanInst = institution.trim().take(180)
        val cleanGrade = gradeClass.trim().take(100)
        val cleanPhoto = photoBase64.trim().take(140000)
        val cleanDevice = deviceInfo.trim().ifBlank { "Android" }.take(180)

        val userDocRef = db.collection("users").document(uid)
        val logDocRef = userDocRef.collection("login_logs").document()

        return try {
            val existingSnap = userDocRef.get().await()
            if (existingSnap.exists()) {
                val prevCount = existingSnap.getLong("loginCount") ?: 1L
                val prevNisn = existingSnap.getString("nisn").orEmpty()
                val prevInst = existingSnap.getString("institution").orEmpty()
                val prevGrade = existingSnap.getString("gradeClass").orEmpty()
                val prevPhoto = existingSnap.getString("photoBase64").orEmpty()
                val prevName = existingSnap.getString("displayName").orEmpty()
                val updatePayload = mapOf(
                    "email" to cleanEmail,
                    "displayName" to if (displayName.isNotBlank()) cleanName else prevName.ifBlank { cleanName },
                    "authMethod" to normalizedMethod,
                    "nisn" to if (cleanNisn.isNotBlank()) cleanNisn else prevNisn,
                    "institution" to if (cleanInst.isNotBlank()) cleanInst else prevInst,
                    "gradeClass" to if (cleanGrade.isNotBlank()) cleanGrade else prevGrade,
                    "photoBase64" to if (cleanPhoto.isNotBlank()) cleanPhoto else prevPhoto,
                    "loginCount" to (prevCount + 1L),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "email" to cleanEmail,
                    "displayName" to cleanName,
                    "authMethod" to normalizedMethod,
                    "nisn" to cleanNisn,
                    "institution" to cleanInst,
                    "gradeClass" to cleanGrade,
                    "photoBase64" to cleanPhoto,
                    "loginCount" to 1L,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(createPayload).await()
            }

            val logPayload = mapOf(
                "userId" to uid,
                "email" to cleanEmail,
                "displayName" to cleanName,
                "authMethod" to normalizedMethod,
                "deviceInfo" to cleanDevice,
                "createdAt" to FieldValue.serverTimestamp()
            )
            logDocRef.set(logPayload).await()

            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfileInfo(
        displayName: String,
        institution: String,
        nisn: String,
        gradeClass: String,
        photoBase64: String
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().ifBlank { "Peserta Evaluasi" }.take(140)
        val cleanInst = institution.trim().take(180)
        val cleanNisn = nisn.trim().take(36)
        val cleanGrade = gradeClass.trim().take(100)
        val cleanPhoto = photoBase64.trim().take(140000)

        return try {
            val snap = userDocRef.get().await()
            if (snap.exists()) {
                val prevPhoto = snap.getString("photoBase64").orEmpty()
                userDocRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "nisn" to cleanNisn,
                        "institution" to cleanInst,
                        "gradeClass" to cleanGrade,
                        "photoBase64" to if (cleanPhoto.isNotBlank()) cleanPhoto else prevPhoto,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    // ================= 1. ACTIVE EXAM SESSIONS =================

    override fun observeActiveSession(userId: String): Flow<ActiveExamSession?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("active_sessions").document("current")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(ActiveExamSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeActiveSession(): Flow<ActiveExamSession?> = observeActiveSession(requireUserId())

    override suspend fun saveActiveSession(session: ActiveExamSession): Result<String> {
        val uid = requireUserId()
        val sessionId = session.id.ifBlank { "current" }
        val docRef = db.collection("users").document(uid)
            .collection("active_sessions").document(sessionId)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "appMode" to session.appMode,
                    "participantName" to session.participantName,
                    "frameworkKey" to session.frameworkKey,
                    "frameworkName" to session.frameworkName,
                    "stage" to session.stage,
                    "subjectName" to session.subjectName,
                    "topicFocus" to session.topicFocus,
                    "difficultyCode" to session.difficultyCode,
                    "scoringMethod" to session.scoringMethod,
                    "lang" to session.lang,
                    "durationMinutes" to session.durationMinutes,
                    "totalTarget" to session.totalTarget,
                    "currentIdx" to session.currentIdx,
                    "timerSeconds" to session.timerSeconds,
                    "questionsPayload" to session.questionsPayload,
                    "answersPayload" to session.answersPayload,
                    "doubtsPayload" to session.doubtsPayload,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "appMode" to session.appMode,
                    "participantName" to session.participantName,
                    "frameworkKey" to session.frameworkKey,
                    "frameworkName" to session.frameworkName,
                    "stage" to session.stage,
                    "subjectName" to session.subjectName,
                    "topicFocus" to session.topicFocus,
                    "difficultyCode" to session.difficultyCode,
                    "scoringMethod" to session.scoringMethod,
                    "lang" to session.lang,
                    "durationMinutes" to session.durationMinutes,
                    "totalTarget" to session.totalTarget,
                    "currentIdx" to session.currentIdx,
                    "timerSeconds" to session.timerSeconds,
                    "questionsPayload" to session.questionsPayload,
                    "answersPayload" to session.answersPayload,
                    "doubtsPayload" to session.doubtsPayload,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun clearActiveSession(sessionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("active_sessions").document(sessionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ================= 2. COMPLETED EXAM RESULTS =================

    override fun observeExamResults(userId: String): Flow<List<ExamResultRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("exam_results")
        val path = "users/$userId/exam_results"
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ExamResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeExamResults(): Flow<List<ExamResultRecord>> = observeExamResults(requireUserId())

    override suspend fun saveExamResult(record: ExamResultRecord): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("exam_results")
        val docRef = if (record.id.isNotBlank()) colRef.document(record.id) else colRef.document()

        val payload = mapOf(
            "userId" to uid,
            "appMode" to record.appMode,
            "participantName" to record.participantName,
            "frameworkKey" to record.frameworkKey,
            "frameworkName" to record.frameworkName,
            "stage" to record.stage,
            "subjectName" to record.subjectName,
            "scoringMethod" to record.scoringMethod,
            "scorePercent" to record.scorePercent,
            "earnedPoints" to record.earnedPoints,
            "maxPoints" to record.maxPoints,
            "abilityIndexScore" to record.abilityIndexScore,
            "logitIndex" to record.logitIndex,
            "scaleBadge" to record.scaleBadge,
            "scaleDisclaimer" to record.scaleDisclaimer,
            "durationTakenMinutes" to record.durationTakenMinutes,
            "pakReiComment" to record.pakReiComment,
            "radarPayload" to record.radarPayload,
            "topicPayload" to record.topicPayload,
            "summariesPayload" to record.summariesPayload,
            "reviewPayload" to record.reviewPayload,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun getExamResultById(ownerUid: String, resultId: String): Result<ExamResultRecord?> {
        val docRef = db.collection("users").document(ownerUid)
            .collection("exam_results").document(resultId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(ExamResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteExamResult(resultId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("exam_results").document(resultId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ================= 3. SAVED BLUEPRINTS =================

    override fun observeSavedBlueprints(userId: String): Flow<List<SavedBlueprintRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("blueprints")
        val path = "users/$userId/blueprints"
        emitAll(
            colRef.whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SavedBlueprintRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedBlueprints(): Flow<List<SavedBlueprintRecord>> = observeSavedBlueprints(requireUserId())

    override suspend fun saveBlueprint(blueprint: SavedBlueprintRecord): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("blueprints")
        val docRef = if (blueprint.id.isNotBlank()) colRef.document(blueprint.id) else colRef.document()

        val payload = mapOf(
            "userId" to uid,
            "title" to blueprint.title,
            "detectedSubject" to blueprint.detectedSubject,
            "frameworkKey" to blueprint.frameworkKey,
            "stage" to blueprint.stage,
            "styleDNA" to blueprint.styleDNA,
            "totalItemsCount" to blueprint.totalItemsCount,
            "itemsPayload" to blueprint.itemsPayload,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteBlueprint(blueprintId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("blueprints").document(blueprintId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
