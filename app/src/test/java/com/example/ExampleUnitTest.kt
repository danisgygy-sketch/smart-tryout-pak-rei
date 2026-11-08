package com.example

import com.example.domain.contracts.ItemScoringStrategy
import com.example.domain.scoring.BkPsychometricScaleStrategy
import com.example.domain.scoring.CambridgeScaleStrategy
import com.example.domain.scoring.CompositeItemScorer
import com.example.domain.scoring.IbScaleStrategy
import com.example.domain.scoring.MerdekaKktpScaleStrategy
import com.example.domain.scoring.MultipleChoiceComplexScoringStrategy
import com.example.domain.scoring.PisaScaleStrategy
import com.example.domain.scoring.PsychometricScaleScoringStrategy
import com.example.domain.scoring.PusmendikTkaAkmScaleStrategy
import com.example.domain.scoring.SatScaleStrategy
import com.example.domain.scoring.ScaleEquivalenceResolver
import com.example.domain.scoring.ShortAnswerScoringStrategy
import com.example.domain.scoring.SingleChoiceScoringStrategy
import com.example.domain.scoring.SnbtScaleStrategy
import com.example.domain.scoring.TimssScaleStrategy
import com.example.domain.scoring.TrueFalseCategoryScoringStrategy
import com.example.domain.serialization.TryoutPayloadSerializer
import com.example.domain.usecase.CalculateDiagnosticReportUseCase
import com.example.model.DimensionStat
import com.example.model.ItemScoreResult
import com.example.model.PsychometricEngine
import com.example.model.QuestionItem
import com.example.model.UserAnswerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun lspAndOcp_allItemScoringStrategiesHonorScoreInvariants() {
        val strategies: List<ItemScoringStrategy> = listOf(
            SingleChoiceScoringStrategy(),
            ShortAnswerScoringStrategy(),
            MultipleChoiceComplexScoringStrategy(),
            TrueFalseCategoryScoringStrategy(),
            PsychometricScaleScoringStrategy()
        )

        val testQuestions = listOf(
            QuestionItem(
                type = "pilgan",
                cognitiveLevel = "L3 - Penalaran",
                options = listOf("A", "B", "C", "D"),
                singleAnswer = 2
            ) to UserAnswerState(singleChoice = 2),
            QuestionItem(
                type = "isian",
                cognitiveLevel = "L2 - Aplikasi",
                acceptedAnswers = listOf("90", "90.0")
            ) to UserAnswerState(textAnswer = " 90,0 "),
            QuestionItem(
                type = "kompleks",
                cognitiveLevel = "L3 - Penalaran",
                options = listOf("1", "2", "3", "4"),
                multiAnswer = listOf(0, 2)
            ) to UserAnswerState(multiChoices = listOf(0, 2)),
            QuestionItem(
                type = "benarsalah",
                cognitiveLevel = "L2 - Aplikasi",
                statements = listOf("S1", "S2"),
                boolAnswers = listOf(true, false)
            ) to UserAnswerState(boolChoices = listOf(true, false)),
            QuestionItem(
                type = "likert",
                cognitiveLevel = "Investigative (I)",
                options = listOf("STS", "TS", "N", "S", "SS"),
                optionWeights = listOf(20, 40, 60, 80, 100),
                singleAnswer = 4
            ) to UserAnswerState(singleChoice = 4)
        )

        val compositeScorer = CompositeItemScorer(strategies)

        for ((question, answer) in testQuestions) {
            val weight = PsychometricEngine.getCognitiveWeight("nasional", question.cognitiveLevel)
            val result = compositeScorer.score(
                question = question,
                answer = answer,
                cognitiveWeight = weight,
                isStrict = false
            )
            // LSP invariant: max > 0.0 and 0.0 <= got <= max
            assertTrue("max must be > 0 for ${question.type}", result.max > 0.0)
            assertTrue("got must be >= 0 for ${question.type}", result.got >= 0.0)
            assertTrue("got must be <= max for ${question.type}", result.got <= result.max)
            assertTrue("expected fullCorrect for matching answer on ${question.type}", result.fullCorrect)
            assertFalse(PsychometricEngine.formatUserAnswerText(question, answer).isBlank())
            assertFalse(PsychometricEngine.formatCorrectAnswerText(question).isBlank())
        }
    }

    @Test
    fun ocp_compositeScorerSupportsCustomQuestionTypeWithoutModifyingExistingStrategies() {
        val customMatrixStrategy = object : ItemScoringStrategy {
            override val supportedTypes: Set<String> = setOf("custom_essay")
            override fun computeScore(
                question: QuestionItem,
                answer: UserAnswerState,
                cognitiveWeight: Double,
                isStrict: Boolean
            ): ItemScoreResult {
                val ok = answer.textAnswer.contains("kata_kunci", ignoreCase = true)
                return ItemScoreResult(got = if (ok) 2.0 else 0.0, max = 2.0, fullCorrect = ok)
            }
        }

        val extendedScorer = CompositeItemScorer(
            strategies = listOf(SingleChoiceScoringStrategy(), customMatrixStrategy)
        )
        val q = QuestionItem(type = "custom_essay", text = "Jelaskan konsep dasar!")
        val a = UserAnswerState(textAnswer = "Mengandung KATA_KUNCI utama")
        val res = extendedScorer.score(
            question = q,
            answer = a,
            cognitiveWeight = 1.0,
            isStrict = false
        )

        assertEquals(2.0, res.got, 0.001)
        assertEquals(2.0, res.max, 0.001)
        assertTrue(res.fullCorrect)
    }

    @Test
    fun lspAndOcp_allScaleEquivalenceStrategiesReturnNonBlankBadges() {
        val resolver = ScaleEquivalenceResolver(
            listOf(
                BkPsychometricScaleStrategy(),
                PisaScaleStrategy(),
                TimssScaleStrategy(),
                CambridgeScaleStrategy(),
                IbScaleStrategy(),
                SatScaleStrategy(),
                SnbtScaleStrategy(),
                PusmendikTkaAkmScaleStrategy(),
                MerdekaKktpScaleStrategy()
            )
        )
        val sampleStats = listOf(
            DimensionStat("Investigative (I)", 90.0, 100.0),
            DimensionStat("Social (S)", 80.0, 100.0),
            DimensionStat("Artistic (A)", 75.0, 100.0)
        )

        val frameworks = listOf("pisa", "timss", "cambridge", "ib", "sat", "snbt", "tka", "akm", "merdeka")
        for (fw in frameworks) {
            val (badge, disclaimer) = resolver.resolve("nasional", fw, 82, sampleStats)
            assertTrue("Badge should not be blank for $fw", badge.isNotBlank())
            assertTrue("Disclaimer should not be blank for $fw", disclaimer.isNotBlank())
        }

        val (bkBadge, _) = resolver.resolve("bk", "bk_riasec", 85, sampleStats)
        assertTrue(bkBadge.contains("ISA"))
    }

    @Test
    fun srp_calculateDiagnosticReportUseCaseAndSerializerRoundTrip() {
        val useCase = CalculateDiagnosticReportUseCase()
        val questions = listOf(
            QuestionItem(
                type = "pilgan",
                topic = "Aljabar",
                cognitiveLevel = "L3 - Penalaran",
                text = "2x = 10",
                options = listOf("3", "4", "5", "6"),
                singleAnswer = 2
            ),
            QuestionItem(
                type = "isian",
                topic = "Statistika",
                cognitiveLevel = "L1 - Pemahaman",
                text = "Rata-rata 8 dan 10",
                acceptedAnswers = listOf("9")
            )
        )
        val answers = listOf(
            UserAnswerState(singleChoice = 2), // Correct (1.5 pts)
            UserAnswerState(textAnswer = "8")  // Incorrect (0 / 1.0 pts)
        )

        val summary = useCase.execute(
            appMode = "nasional",
            frameworkKey = "tka",
            scoringMethod = "polytomous",
            questions = questions,
            answers = answers
        )

        assertEquals(60, summary.scorePercent) // 1.5 / 2.5 = 60%
        assertEquals(listOf("Statistika"), summary.uniqueMistakeTopics)
        assertEquals(2, summary.reviewItems.size)

        val serializedQuestions = TryoutPayloadSerializer.serializeQuestions(questions)
        val restoredQuestions = TryoutPayloadSerializer.deserializeQuestions(serializedQuestions)
        assertEquals(2, restoredQuestions.size)
        assertEquals("Aljabar", restoredQuestions[0].topic)
    }

    @Test
    fun unifiedStudentIdentity_matchStageForGradeAndPendingProfileStore() {
        val smpStage = com.example.model.CurriculumCatalog.matchStageForGrade("merdeka", "Kelas 9 SMP/MTs")
        assertTrue(smpStage.contains("SMP"))

        val smaStage = com.example.model.CurriculumCatalog.matchStageForGrade("tka", "Kelas 12 SMA/MA/SMK")
        assertTrue(smaStage.contains("SMA"))

        val bkStage = com.example.model.CurriculumCatalog.matchStageForGrade("bk_riasec", "Kelas 8 SMP/MTs")
        assertTrue(bkStage.contains("SMP"))

        com.example.model.PendingAuthProfileStore.recordPendingAuth(
            method = "email",
            email = "budi@sekolah.id",
            displayName = "Budi Santoso",
            nisn = "0081928374",
            institution = "SMAN 3 Bandung",
            gradeClass = "Kelas 11 SMA/MA/SMK",
            photoBase64 = "base64_avatar_test"
        )
        val (method, email, name) = com.example.model.PendingAuthProfileStore.consumeOrDefault("", "")
        assertEquals("email", method)
        assertEquals("budi@sekolah.id", email)
        assertEquals("Budi Santoso", name)
        assertEquals("0081928374", com.example.model.PendingAuthProfileStore.pendingNisn)
        assertEquals("SMAN 3 Bandung", com.example.model.PendingAuthProfileStore.pendingInstitution)
        assertEquals("Kelas 11 SMA/MA/SMK", com.example.model.PendingAuthProfileStore.pendingGradeClass)
        assertEquals("base64_avatar_test", com.example.model.PendingAuthProfileStore.pendingPhotoBase64)
    }
}
