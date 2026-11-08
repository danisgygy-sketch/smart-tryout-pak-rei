package com.example.domain.scoring

import com.example.domain.contracts.ItemScoringStrategy
import com.example.domain.contracts.ScaleEquivalenceStrategy
import com.example.model.DimensionStat
import com.example.model.ItemScoreResult
import com.example.model.QuestionItem
import com.example.model.UserAnswerState
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * OCP (Open/Closed Principle) & LSP (Liskov Substitution Principle):
 * Each question type and curriculum scale equivalence rule is encapsulated in its own
 * strategy implementation. New question types or curriculum scales can be added by
 * registering a new strategy without modifying existing strategy classes.
 */

class SingleChoiceScoringStrategy : ItemScoringStrategy {
    override val supportedTypes: Set<String> = setOf("pilgan")

    override fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val w = if (isStrict) 1.0 else max(0.1, cognitiveWeight)
        val ok = answer.singleChoice != null && answer.singleChoice == question.singleAnswer
        return ItemScoreResult(got = if (ok) w else 0.0, max = w, fullCorrect = ok)
    }
}

class ShortAnswerScoringStrategy : ItemScoringStrategy {
    override val supportedTypes: Set<String> = setOf("isian")

    private fun normalize(s: String): String {
        return s.trim().lowercase().replace(Regex("\\s+"), " ").replace(',', '.')
    }

    override fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val w = if (isStrict) 1.0 else max(0.1, cognitiveWeight)
        val userNorm = normalize(answer.textAnswer)
        val ok = userNorm.isNotEmpty() && question.acceptedAnswers.any { normalize(it) == userNorm }
        return ItemScoreResult(got = if (ok) w else 0.0, max = w, fullCorrect = ok)
    }
}

class MultipleChoiceComplexScoringStrategy : ItemScoringStrategy {
    override val supportedTypes: Set<String> = setOf("kompleks")

    override fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val keySet = question.multiAnswer.toSet()
        val userSet = answer.multiChoices.toSet()
        val exact = userSet.size == keySet.size && userSet.all { it in keySet }

        if (isStrict) {
            return ItemScoreResult(got = if (exact) 1.0 else 0.0, max = 1.0, fullCorrect = exact)
        }

        val w = max(0.1, cognitiveWeight)
        var rightChosen = 0
        var wrongChosen = 0
        userSet.forEach { v ->
            if (v in keySet) rightChosen++ else wrongChosen++
        }
        val rawFraction = max(0.0, (rightChosen - wrongChosen).toDouble() / max(1, keySet.size).toDouble())
        val gotPts = ((rawFraction * w) * 100.0).roundToInt() / 100.0
        return ItemScoreResult(got = gotPts, max = w, fullCorrect = exact)
    }
}

class TrueFalseCategoryScoringStrategy : ItemScoringStrategy {
    override val supportedTypes: Set<String> = setOf("benarsalah")

    override fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val n = max(1, question.statements.size)
        var rightCount = 0
        for (j in 0 until n) {
            val userBool = answer.boolChoices.getOrNull(j)
            val keyBool = question.boolAnswers.getOrNull(j)
            if (userBool != null && userBool == keyBool) rightCount++
        }
        val allOk = rightCount == n

        if (isStrict) {
            return ItemScoreResult(got = if (allOk) 1.0 else 0.0, max = 1.0, fullCorrect = allOk)
        }

        val w = max(0.1, cognitiveWeight)
        val maxPts = ((n * 0.5 * w) * 100.0).roundToInt() / 100.0
        val gotPts = (((rightCount.toDouble() / n.toDouble()) * maxPts) * 100.0).roundToInt() / 100.0
        return ItemScoreResult(got = gotPts, max = maxPts, fullCorrect = allOk)
    }
}

class PsychometricScaleScoringStrategy : ItemScoringStrategy {
    override val supportedTypes: Set<String> = setOf("likert", "sjt")

    override fun computeScore(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val weights = if (question.optionWeights.isNotEmpty()) {
            question.optionWeights
        } else {
            listOf(20, 40, 60, 80, 100)
        }
        val choice = answer.singleChoice
        val rawVal = if (choice != null && choice in weights.indices) {
            weights[choice].toDouble().coerceIn(0.0, 100.0)
        } else {
            0.0
        }
        return ItemScoreResult(got = rawVal, max = 100.0, fullCorrect = rawVal >= 75.0)
    }
}

class CompositeItemScorer(
    strategies: List<ItemScoringStrategy> = listOf(
        SingleChoiceScoringStrategy(),
        ShortAnswerScoringStrategy(),
        MultipleChoiceComplexScoringStrategy(),
        TrueFalseCategoryScoringStrategy(),
        PsychometricScaleScoringStrategy()
    )
) {
    private val strategyMap: MutableMap<String, ItemScoringStrategy> = mutableMapOf()

    init {
        strategies.forEach { registerStrategy(it) }
    }

    fun registerStrategy(strategy: ItemScoringStrategy) {
        strategy.supportedTypes.forEach { type ->
            strategyMap[type.lowercase()] = strategy
        }
    }

    fun score(
        question: QuestionItem,
        answer: UserAnswerState,
        cognitiveWeight: Double,
        isStrict: Boolean
    ): ItemScoreResult {
        val strategy = strategyMap[question.type.lowercase()]
            ?: return ItemScoreResult(got = 0.0, max = 1.0, fullCorrect = false)
        return strategy.computeScore(
            question = question,
            answer = answer,
            cognitiveWeight = cognitiveWeight,
            isStrict = isStrict
        )
    }
}

// ================= SCALE EQUIVALENCE STRATEGIES (OCP & LSP) =================

class BkPsychometricScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean =
        appMode == "bk" || frameworkKey.startsWith("bk_")

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val sorted = cogStats.sortedByDescending { it.percentage }
        if (frameworkKey == "bk_riasec" && sorted.size >= 3) {
            val code = sorted.take(3).joinToString("") { stat ->
                Regex("\\(([A-Z])\\)").find(stat.label)?.groupValues?.getOrNull(1)
                    ?: stat.label.firstOrNull()?.uppercase() ?: ""
            }
            return "Profil RIASEC Dominan: $code (${sorted[0].label})" to
                "Pemetaan minat non-klinis berdasarkan respons mandiri (bukan hasil tes psikologi klinis berlisensi)."
        }
        if (sorted.isNotEmpty()) {
            return "Kecenderungan Utama: ${sorted[0].label} (${sorted[0].percentage}%)" to
                "Pemetaan diagnostik non-kognitif untuk bahan refleksi bersama Guru BK."
        }
        return "Indeks Psikopedagogis: $scorePercent%" to "Instrumen refleksi diri non-klinis."
    }
}

class PisaScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "pisa"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val lvl = when {
            scorePercent >= 90 -> "Level 6"
            scorePercent >= 78 -> "Level 5"
            scorePercent >= 65 -> "Level 4"
            scorePercent >= 50 -> "Level 3"
            scorePercent >= 35 -> "Level 2 (Baseline)"
            else -> "Level 1"
        }
        return "Estimasi Setara PISA: ~$lvl" to
            "*Estimasi simulasi dari persentase capaian berbobot. Konversi resmi OECD PISA menggunakan Plausible Values (IRT 2PL/GPC) pada skala rata-rata 500."
    }
}

class TimssScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "timss"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val bm = when {
            scorePercent >= 88 -> "Advanced (~625)"
            scorePercent >= 74 -> "High (~550)"
            scorePercent >= 55 -> "Intermediate (~475)"
            else -> "Low (~400)"
        }
        return "Estimasi Setara TIMSS: $bm" to
            "*Estimasi simulasi. International Benchmarks resmi IEA TIMSS dikalibrasi melalui IRT 3PL terhadap populasi global."
    }
}

class CambridgeScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "cambridge"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val g = when {
            scorePercent >= 90 -> "A*"
            scorePercent >= 80 -> "A"
            scorePercent >= 70 -> "B"
            scorePercent >= 60 -> "C"
            scorePercent >= 50 -> "D"
            else -> "E / U"
        }
        return "Estimasi Setara Grade: ~$g (Simulasi PUM $scorePercent)" to
            "*Estimasi simulasi setara Percentage Uniform Mark (PUM). Grade resmi Cambridge ditentukan melalui Grade Thresholds spesifik tiap seri ujian."
    }
}

class IbScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "ib"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val b = when {
            scorePercent >= 85 -> 7
            scorePercent >= 73 -> 6
            scorePercent >= 60 -> 5
            scorePercent >= 48 -> 4
            scorePercent >= 35 -> 3
            scorePercent >= 20 -> 2
            else -> 1
        }
        return "Estimasi Setara IB Grade: ~Band $b / 7" to
            "*Estimasi simulasi. Grade Band resmi IB (1–7) ditetapkan melalui Grade Boundaries komite penguji."
    }
}

class SatScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "sat"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val scaled = (200 + (scorePercent / 100.0) * 600).roundToInt()
        return "Estimasi Skor Simulasi: ~$scaled / 800" to
            "*Estimasi linier simulasi. Skor resmi Digital SAT (200–800) menggunakan kalibrasi Multistage Adaptive Testing (MST) College Board."
    }
}

class SnbtScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = frameworkKey == "snbt"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val scaled = (200 + (scorePercent / 100.0) * 600).roundToInt()
        return "Estimasi Skor Simulasi: ~$scaled (Rentang 200–800)" to
            "*Estimasi simulasi berbobot kognitif. Skor resmi UTBK-SNBT BPPP dihitung dengan kalibrasi Item Response Theory (IRT) pasca-ujian nasional."
    }
}

class PusmendikTkaAkmScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean =
        frameworkKey == "tka" || frameworkKey == "akm"

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val kat = when {
            scorePercent >= 80 -> "Mahir (Perlu Pengayaan)"
            scorePercent >= 65 -> "Cakap (Sesuai Standar)"
            scorePercent >= 45 -> "Dasar (Pendampingan)"
            else -> "Perlu Intervensi Khusus"
        }
        return "Estimasi Kategori Pusmendik: $kat" to
            "*Pemetaan simulasi berdasarkan persentase skor polytomous. Penyetaraan resmi TKA/AKM Pusmendik mengacu pada standar nasional."
    }
}

class MerdekaKktpScaleStrategy : ScaleEquivalenceStrategy {
    override fun supports(appMode: String, frameworkKey: String): Boolean = true

    override fun resolveScale(
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val kktp = when {
            scorePercent >= 86 -> "Mahir (86–100%)"
            scorePercent >= 71 -> "Cakap / Tuntas KKTP (71–85%)"
            scorePercent >= 55 -> "Layak / Bimbingan Terbatas (55–70%)"
            else -> "Baru Berkembang (<55%)"
        }
        return "Interval KKTP Kurikulum Merdeka: $kktp" to
            "Acuan Kriteria Ketercapaian Tujuan Pembelajaran (KKTP) berbasis interval nilai."
    }
}

class ScaleEquivalenceResolver(
    private val strategies: List<ScaleEquivalenceStrategy> = listOf(
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
) {
    fun resolve(
        appMode: String,
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> {
        val matched = strategies.firstOrNull { it.supports(appMode, frameworkKey) }
            ?: MerdekaKktpScaleStrategy()
        return matched.resolveScale(frameworkKey, scorePercent, cogStats)
    }
}
