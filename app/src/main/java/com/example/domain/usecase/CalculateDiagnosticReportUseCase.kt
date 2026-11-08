package com.example.domain.usecase

import com.example.model.DimensionStat
import com.example.model.PsychometricEngine
import com.example.model.QuestionItem
import com.example.model.QuestionReviewItem
import com.example.model.UserAnswerState
import kotlin.math.roundToInt

data class CalculatedDiagnosticSummary(
    val scorePercent: Int,
    val earnedPoints: Double,
    val maxPoints: Double,
    val abilityIndex: Int,
    val logitIndex: Double,
    val scaleBadge: String,
    val scaleDisclaimer: String,
    val cogStats: List<DimensionStat>,
    val topicStats: List<DimensionStat>,
    val reviewItems: List<QuestionReviewItem>,
    val uniqueMistakeTopics: List<String>
)

/**
 * SRP (Single Responsibility Principle):
 * Encapsulates the psychometric aggregation and diagnostic report calculation
 * separately from UI state management in TryoutSessionViewModel.
 */
class CalculateDiagnosticReportUseCase {

    fun execute(
        appMode: String,
        frameworkKey: String,
        scoringMethod: String,
        questions: List<QuestionItem>,
        answers: List<UserAnswerState>
    ): CalculatedDiagnosticSummary {
        var totalMax = 0.0
        var totalGot = 0.0
        val cogMap = linkedMapOf<String, Pair<Double, Double>>()
        val topicMap = linkedMapOf<String, Pair<Double, Double>>()
        val mistakes = mutableListOf<String>()
        val reviewItems = mutableListOf<QuestionReviewItem>()

        questions.forEachIndexed { i, q ->
            val ans = answers.getOrElse(i) { UserAnswerState() }
            val sc = PsychometricEngine.computeItemScore(appMode, scoringMethod, q, ans)
            totalMax += sc.max
            totalGot += sc.got

            val cogKey = q.cognitiveLevel.ifBlank { "Aplikasi" }
            val curCog = cogMap[cogKey] ?: (0.0 to 0.0)
            cogMap[cogKey] = (curCog.first + sc.got) to (curCog.second + sc.max)

            val topKey = q.topic.ifBlank { "Konsep Umum" }
            val curTop = topicMap[topKey] ?: (0.0 to 0.0)
            topicMap[topKey] = (curTop.first + sc.got) to (curTop.second + sc.max)

            if (!sc.fullCorrect) mistakes.add(topKey)

            reviewItems.add(
                QuestionReviewItem(
                    index = i,
                    question = q,
                    userAnswerText = PsychometricEngine.formatUserAnswerText(q, ans),
                    correctAnswerText = PsychometricEngine.formatCorrectAnswerText(q),
                    gotScore = sc.got,
                    maxScore = sc.max,
                    fullCorrect = sc.fullCorrect
                )
            )
        }

        val scorePercent = if (totalMax > 0.0) {
            ((totalGot / totalMax) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val (logitIndex, abilityIndex) = PsychometricEngine.computeEstimatedAbilityIndex(scorePercent)
        val cogStats = cogMap.map { (k, v) -> DimensionStat(k, v.first, v.second) }
        val topicStats = topicMap.map { (k, v) -> DimensionStat(k, v.first, v.second) }
        val (scaleBadge, scaleDisclaimer) = PsychometricEngine.getScaleEquivalent(
            appMode = appMode,
            frameworkKey = frameworkKey,
            scorePercent = scorePercent,
            cogStats = cogStats
        )

        return CalculatedDiagnosticSummary(
            scorePercent = scorePercent,
            earnedPoints = (totalGot * 10.0).roundToInt() / 10.0,
            maxPoints = (totalMax * 10.0).roundToInt() / 10.0,
            abilityIndex = abilityIndex,
            logitIndex = logitIndex,
            scaleBadge = scaleBadge,
            scaleDisclaimer = scaleDisclaimer,
            cogStats = cogStats,
            topicStats = topicStats,
            reviewItems = reviewItems,
            uniqueMistakeTopics = mistakes.distinct().take(4)
        )
    }
}
