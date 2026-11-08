package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ActiveExamSession
import com.example.model.ExamResultRecord
import com.example.model.SavedBlueprintRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.UUID

@Config(sdk = [36])
class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun recordAndObserveUserLoginAndProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = CloudSyncRepository(firestore)

        val res = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordUserLoginAndProfile(
                email = ALICE_EMAIL,
                displayName = "Siti Aminah",
                authMethod = "email",
                institution = "SMAN 1 Jakarta",
                deviceInfo = "Android 16",
                nisn = "0078192831",
                gradeClass = "Kelas 12 SMA/MA/SMK",
                photoBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            )
        }
        assertTrue("Expected recordUserLoginAndProfile to succeed", res.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { it != null && it.displayName == "Siti Aminah" }
        }
        assertNotNull(profile)
        assertEquals("email", profile?.authMethod)
        assertEquals("0078192831", profile?.nisn)
        assertEquals("SMAN 1 Jakarta", profile?.institution)
        assertEquals("Kelas 12 SMA/MA/SMK", profile?.gradeClass)

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateUserProfileInfo(
                displayName = "Siti Aminah Putri",
                institution = "SMAN 8 Jakarta",
                nisn = "0078192899",
                gradeClass = "Kelas 12 SMA/MA/SMK",
                photoBase64 = ""
            )
        }
        assertTrue("Expected updateUserProfileInfo to succeed", updateRes.isSuccess)

        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { it != null && it.displayName == "Siti Aminah Putri" }
        }
        assertEquals("0078192899", updatedProfile?.nisn)
        assertEquals("SMAN 8 Jakarta", updatedProfile?.institution)

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLoginHistory(aliceUid).first { it.isNotEmpty() }
        }
        assertTrue(logs.any { it.email == ALICE_EMAIL && it.authMethod == "email" })
    }

    @Test
    fun saveAndObserveActiveSession_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = CloudSyncRepository(firestore)

        val session = ActiveExamSession(
            id = "current",
            userId = aliceUid,
            appMode = "nasional",
            participantName = "Siti Aminah",
            frameworkKey = "tka",
            frameworkName = "TKA Resmi (Perka BSKAP 2025/2026)",
            stage = "SMA/MA/SMK (Kelas 12 - Wajib & Pilihan)",
            subjectName = "Matematika (Wajib)",
            topicFocus = "Aljabar",
            difficultyCode = "Standar",
            scoringMethod = "polytomous",
            lang = "id",
            durationMinutes = 45L,
            totalTarget = 20L,
            currentIdx = 3L,
            timerSeconds = 2200L,
            questionsPayload = "[]",
            answersPayload = "[]",
            doubtsPayload = "[]"
        )

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveActiveSession(session) }
        assertTrue("Expected saveActiveSession to succeed", saveRes.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeActiveSession(aliceUid).first { it != null && it.currentIdx == 3L }
        }
        assertNotNull(observed)
        assertEquals("Matematika (Wajib)", observed?.subjectName)
    }

    @Test
    fun saveAndObserveExamResult_authenticatedOwner_emitsResult() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = CloudSyncRepository(firestore)
        val dynamicId = "res_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val record = ExamResultRecord(
            id = dynamicId,
            userId = aliceUid,
            appMode = "nasional",
            participantName = "Siti Aminah",
            frameworkKey = "tka",
            frameworkName = "TKA Resmi (Perka BSKAP 2025/2026)",
            stage = "SMA/MA/SMK (Kelas 12 - Wajib & Pilihan)",
            subjectName = "Fisika",
            scoringMethod = "polytomous",
            scorePercent = 88L,
            earnedPoints = 17.6,
            maxPoints = 20.0,
            abilityIndexScore = 699L,
            logitIndex = 1.99,
            scaleBadge = "Estimasi Kategori Pusmendik: Mahir",
            scaleDisclaimer = "Simulasi",
            durationTakenMinutes = 35L,
            pakReiComment = "Penguasaan mekanika sangat kuat."
        )

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveExamResult(record) }
        assertTrue(saveRes.isSuccess)

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeExamResults(aliceUid).first { list -> list.any { it.id == dynamicId } }
        }
        assertTrue(emittedList.any { it.id == dynamicId && it.scorePercent == 88L })
    }

    @Test
    fun saveAndObserveBlueprint_authenticatedOwner_emitsBlueprint() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = CloudSyncRepository(firestore)
        val dynamicId = "bp_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val bp = SavedBlueprintRecord(
            id = dynamicId,
            userId = aliceUid,
            title = "Kisi-Kisi TKA Biologi",
            detectedSubject = "Biologi",
            frameworkKey = "tka",
            stage = "SMA/MA/SMK (Kelas 12 - Wajib & Pilihan)",
            styleDNA = "Soal berbasis eksperimen ilmiah.",
            totalItemsCount = 20L,
            itemsPayload = "[{\"domain\":\"Genetika\",\"indicator\":\"Menganalisis persilangan\",\"cognitiveLevel\":\"L3 - Penalaran\",\"questionType\":\"pilgan\",\"count\":20}]"
        )

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveBlueprint(bp) }
        assertTrue(saveRes.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSavedBlueprints(aliceUid).first { list -> list.any { it.id == dynamicId } }
        }
        assertTrue(emitted.any { it.id == dynamicId && it.detectedSubject == "Biologi" })
    }

    @Test
    fun getExamResultById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        val dynamicId = "res_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val record = ExamResultRecord(
            id = dynamicId,
            userId = aliceUid,
            appMode = "nasional",
            participantName = "Alice",
            frameworkKey = "tka",
            frameworkName = "TKA Resmi",
            stage = "SMA",
            subjectName = "Kimia",
            scoringMethod = "polytomous",
            scorePercent = 90L,
            earnedPoints = 18.0,
            maxPoints = 20.0,
            abilityIndexScore = 720L,
            logitIndex = 2.2,
            scaleBadge = "Mahir",
            scaleDisclaimer = "Simulasi",
            durationTakenMinutes = 25L,
            pakReiComment = "Bagus"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveExamResult(record).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        val crossRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getExamResultById(aliceUid, dynamicId) }
        assertTrue("Expected cross-user access to fail", crossRes.isFailure)
        val fsEx = crossRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeExamResults_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = CloudSyncRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeExamResults(aliceUid).first()
            }
            fail("Expected unauthenticated observeExamResults to throw")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@pakrei.test"
        const val BOB_EMAIL = "bob@pakrei.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
