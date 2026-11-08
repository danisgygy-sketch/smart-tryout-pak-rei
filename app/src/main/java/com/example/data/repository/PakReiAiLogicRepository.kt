package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.contracts.BlueprintExtractorService
import com.example.domain.contracts.DiagnosticAnalyzerService
import com.example.domain.contracts.QuestionGeneratorService
import com.example.model.BlueprintData
import com.example.model.BlueprintRow
import com.example.model.CurriculumCatalog
import com.example.model.DimensionStat
import com.example.model.PakReiSummary
import com.example.model.QuestionItem
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

private const val TAG = "PakReiAiLogicRepo"

class PakReiAiLogicRepository :
    QuestionGeneratorService,
    BlueprintExtractorService,
    DiagnosticAnalyzerService {

    private val blueprintSchema = Schema.obj(
        properties = mapOf(
            "detectedSubject" to Schema.string("Detected subject name"),
            "styleDNA" to Schema.string("Summary of stimulus style, conceptual depth, and distractor patterns"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "domain" to Schema.string("Topic or competency domain"),
                        "indicator" to Schema.string("Specific question indicator"),
                        "cognitiveLevel" to Schema.string("Cognitive level label"),
                        "questionType" to Schema.string("pilgan, kompleks, benarsalah, or isian"),
                        "count" to Schema.integer("Number of questions for this indicator")
                    )
                ),
                description = "List of blueprint indicator rows"
            )
        )
    )

    private val questionsSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "type" to Schema.string("pilgan, kompleks, benarsalah, isian, likert, or sjt"),
                        "topic" to Schema.string("Topic or sub-aspect"),
                        "indicator" to Schema.string("Competency or behavioral indicator"),
                        "cognitiveLevel" to Schema.string("Cognitive level or BK dimension"),
                        "stimulus" to Schema.string("Reading passage, markdown table, or case context"),
                        "text" to Schema.string("Main question stem or reflection statement"),
                        "options" to Schema.array(Schema.string(), "Choice options (4 for pilgan/kompleks/sjt, 5 for likert)"),
                        "statements" to Schema.array(Schema.string(), "Statements for benarsalah questions"),
                        "optionWeights" to Schema.array(Schema.integer(), "Weights 0-100 for likert or sjt options"),
                        "singleAnswer" to Schema.integer("Correct 0-based index for pilgan or sjt/likert"),
                        "multiAnswer" to Schema.array(Schema.integer(), "Array of 0-based correct indices for kompleks"),
                        "boolAnswers" to Schema.array(Schema.boolean(), "Array of booleans for benarsalah statements"),
                        "acceptedAnswers" to Schema.array(Schema.string(), "Accepted short answers for isian"),
                        "explanation" to Schema.string("Step-by-step academic explanation or BK reflection insight")
                    ),
                    optionalProperties = listOf(
                        "stimulus",
                        "options",
                        "statements",
                        "optionWeights",
                        "singleAnswer",
                        "multiAnswer",
                        "boolAnswers",
                        "acceptedAnswers"
                    )
                )
            )
        )
    )

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "comment" to Schema.string("Empathetic and sharp commentary from Pak Rei (2-3 sentences)"),
            "summaries" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "topic" to Schema.string("Topic or career recommendation title"),
                        "explanation" to Schema.string("Actionable diagnosis or remediation advice")
                    )
                )
            )
        )
    )

    // Lazy initialization per Firebase AI Logic best practices
    private val flash38QuestionsModel by lazy { createModel("gemini-3.8-flash", questionsSchema) }
    private val flashLiteQuestionsModel by lazy { createModel("gemini-3.5-flash-lite", questionsSchema) }
    private val argonQuestionsModel by lazy { createModel("gemini-4-argon", questionsSchema) }

    private val flash38BlueprintModel by lazy { createModel("gemini-3.8-flash", blueprintSchema) }
    private val flashLiteBlueprintModel by lazy { createModel("gemini-3.5-flash-lite", blueprintSchema) }
    private val argonBlueprintModel by lazy { createModel("gemini-4-argon", blueprintSchema) }

    private val flash38AnalysisModel by lazy { createModel("gemini-3.8-flash", analysisSchema) }
    private val flashLiteAnalysisModel by lazy { createModel("gemini-3.5-flash-lite", analysisSchema) }
    private val argonAnalysisModel by lazy { createModel("gemini-4-argon", analysisSchema) }

    private fun createModel(modelName: String, schema: Schema): GenerativeModel {
        val cfg = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
            temperature = 0.45f
        }
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = cfg
        )
    }

    private fun selectQuestionsModel(modelName: String): GenerativeModel = when (modelName) {
        "gemini-3.5-flash-lite" -> flashLiteQuestionsModel
        "gemini-4-argon" -> argonQuestionsModel
        else -> flash38QuestionsModel
    }

    private fun selectBlueprintModel(modelName: String): GenerativeModel = when (modelName) {
        "gemini-3.5-flash-lite" -> flashLiteBlueprintModel
        "gemini-4-argon" -> argonBlueprintModel
        else -> flash38BlueprintModel
    }

    private fun selectAnalysisModel(modelName: String): GenerativeModel = when (modelName) {
        "gemini-3.5-flash-lite" -> flashLiteAnalysisModel
        "gemini-4-argon" -> argonAnalysisModel
        else -> flash38AnalysisModel
    }

    // ================= 1. REVERSE ENGINEERING KISI-KISI PDF =================

    override suspend fun reverseEngineerPdfBlueprint(
        modelName: String,
        frameworkKey: String,
        stage: String,
        targetQCount: Int,
        kisiBitmaps: List<Bitmap>,
        sampleBitmaps: List<Bitmap>
    ): Result<BlueprintData> {
        val fw = CurriculumCatalog.FRAMEWORKS[frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["tka"]!!
        val prompt = """
            Anda adalah Mesin Reverse Engineering Asesmen Pendidikan Pak Rei v3.1.
            Tugas Anda: Bedah halaman dokumen KISI-KISI / SILABUS (dan CONTOH SOAL bila dilampirkan) menjadi BLUEPRINT UJIAN terstruktur untuk total $targetQCount butir soal.
            Kerangka acuan: ${fw.name} ($stage).

            ATURAN PENTING:
            1. Buat 6 s.d. 12 baris indikator di 'items' yang mewakili seluruh cakupan dokumen secara proporsional.
            2. Total penjumlahan 'count' dari seluruh baris di 'items' HARUS SAMA DENGAN $targetQCount.
            3. Pilih 'cognitiveLevel' dari: ${fw.cogLevels.joinToString(" | ")}.
            4. Variasikan 'questionType' (pilgan, kompleks, benarsalah, isian) sesuai karakter materi.
        """.trimIndent()

        return try {
            val model = selectBlueprintModel(modelName)
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        kisiBitmaps.take(4).forEach { bmp -> image(bmp) }
                        sampleBitmaps.take(2).forEach { bmp -> image(bmp) }
                        text(prompt)
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Respons AI kosong saat membedah PDF.")
            val json = JSONObject(rawJson)
            val itemsArr = json.optJSONArray("items") ?: JSONArray()
            val rows = (0 until itemsArr.length()).map { i ->
                val o = itemsArr.getJSONObject(i)
                val qType = o.optString("questionType", "pilgan")
                BlueprintRow(
                    domain = o.optString("domain", "Materi Pokok").ifBlank { "Materi Pokok" },
                    indicator = o.optString("indicator", "Menganalisis konsep sesuai kisi-kisi").ifBlank { "Menganalisis konsep sesuai kisi-kisi" },
                    cognitiveLevel = o.optString("cognitiveLevel", fw.cogLevels.first()),
                    questionType = if (qType in listOf("pilgan", "kompleks", "benarsalah", "isian")) qType else "pilgan",
                    count = max(1, o.optInt("count", 1))
                )
            }
            if (rows.isEmpty()) throw IllegalStateException("Blueprint tidak menghasilkan baris indikator.")
            Result.success(
                BlueprintData(
                    detectedSubject = json.optString("detectedSubject", "Matematika & Sains"),
                    styleDNA = json.optString("styleDNA", "Soal berbasis stimulus kontekstual dengan pengecoh homogen."),
                    items = rows
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Blueprint reverse engineering timed out", e)
            Result.failure(IllegalStateException("Waktu pemrosesan PDF habis (60 detik). Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Blueprint reverse engineering failed", e)
            Result.failure(e)
        }
    }

    // ================= 2. QUESTION GENERATION + DUAL-PASS AUDIT =================

    override suspend fun generateQuestionBatch(
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
    ): Result<List<QuestionItem>> {
        val fw = CurriculumCatalog.FRAMEWORKS[frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["merdeka"]!!
        val prompt = if (appMode == "bk") {
            """
                Rancang TEPAT $amountNeeded butir instrumen Asesmen Bimbingan & Konseling (BK) Psikopedagogis NON-KLINIS:
                KERANGKA: ${fw.name}
                JENJANG SASARAN: $stage
                FOKUS PEMETAAN: $subjectName ${if (topicFocus.isNotBlank()) "($topicFocus)" else ""}
                DIMENSI YANG WAJIB DIPETAKAN MERATA DI 'cognitiveLevel': ${fw.cogLevels.joinToString(" | ")}
                PEDOMAN: ${fw.promptRules}

                ATURAN FORMAT:
                - Gunakan kombinasi 'type': 'likert' (5 opsi: Sangat Tidak Sesuai s.d. Sangat Sesuai, 'optionWeights' berisi 5 angka 20-100, misal [20, 40, 60, 80, 100]) dan 'sjt' (Situational Judgment Test 4 opsi tindakan nyata di 'options', 'optionWeights' berisi 4 angka misal [100, 75, 50, 25]).
                - Isi 'singleAnswer' dengan indeks opsi berbobot tertinggi (0-4 untuk likert, 0-3 untuk sjt).
            """.trimIndent()
        } else {
            val langInstruction = when (lang) {
                "en" -> "LANGUAGE: Write ALL stimulus, question stems, options, statements, and explanations strictly in academic ENGLISH."
                "bilingual" -> "LANGUAGE: Write the stimulus, question stem, and options in ENGLISH, and provide the 'explanation' in BAHASA INDONESIA."
                else -> "BAHASA: Gunakan Bahasa Indonesia baku (EYD V) yang lugas dan ilmiah."
            }

            val slots = getBlueprintSlots(blueprint, startIdx, amountNeeded)
            val specBlock = if (slots != null) {
                val slotLines = slots.mapIndexed { i, s ->
                    "Soal #${i + 1}: Domain=\"${s.domain}\" | Indikator=\"${s.indicator}\" | Level Kognitif=\"${s.cognitiveLevel}\" | Bentuk=\"${s.questionType}\""
                }.joinToString("\n")
                """
                    BLUEPRINT SPESIFIK PER NOMOR SOAL (WAJIB DIIKUTI 1:1):
                    $slotLines
                    DNA Gaya Soal: ${blueprint?.styleDNA.orEmpty()}
                """.trimIndent()
            } else {
                """
                    STANDAR KURIKULUM: ${fw.name}
                    JENJANG / FASE / KELAS: $stage
                    MATA PELAJARAN / SUBTES: $subjectName
                    ${if (topicFocus.isNotBlank()) "FOKUS TOPIK / CP / KD: $topicFocus" else "CAKUPAN: Sebar merata pada elemen/pokok bahasan utama di jenjang ini."}
                    KALIBRASI KESULITAN: $difficultyCode
                    PEDOMAN KURIKULUM: ${fw.promptRules}
                    LEVEL KOGNITIF YANG DIGUNAKAN (SEBAR MERATA): ${fw.cogLevels.joinToString(", ")}
                """.trimIndent()
            }

            """
                Rancang TEPAT $amountNeeded butir soal evaluasi akademik berkualitas tinggi.
                $langInstruction

                $specBlock

                ATURAN KUALITAS & ANTI-HALUSINASI:
                1. SINTESIS ORISINAL: Buat kasus, tabel data pipe Markdown, atau wacana kontekstual di 'stimulus'.
                2. KOMBINASI BENTUK SOAL:
                   - "pilgan": 4 opsi jawaban homogen di 'options', tepat 1 kunci benar di 'singleAnswer' (index 0–3).
                   - "kompleks": Pilihan Ganda Kompleks (MCMA) 4 opsi di 'options', 'multiAnswer' berupa array index benar (misal [0, 2]).
                   - "benarsalah": 4 pernyataan analitis pada array 'statements', 'boolAnswers' berupa array 4 boolean [true, false, true, false].
                   - "isian": Jawaban singkat eksak, isi 'acceptedAnswers' dengan variasi penulisan yang benar (misal ["90", "90.0"]).
                3. VERIFIKASI MANDIRI: Hitung ulang sebelum mengisi kunci jawaban dan tulis langkah penyelesaian lengkap di 'explanation'.
            """.trimIndent()
        }

        return try {
            val model = selectQuestionsModel(modelName)
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawText = response.text ?: throw IllegalStateException("Model AI mengembalikan respons kosong.")
            var normalized = normalizeQuestionsFromJson(rawText, appMode, fw.cogLevels.first())
            if (normalized.isEmpty()) {
                throw IllegalStateException("Tidak ada butir soal yang lolos validasi kunci ketat.")
            }

            if (dualPassVerify && appMode != "bk") {
                try {
                    val verifyPrompt = """
                        Anda adalah Auditor Psikometri Pak Rei. Periksa kembali kebenaran perhitungan matematis/faktual dan kunci jawaban ('singleAnswer', 'multiAnswer', 'boolAnswers', 'acceptedAnswers') dari daftar soal JSON berikut.
                        Jika ada kunci yang keliru, perbaiki kuncinya dan perjelas 'explanation'-nya. Kembalikan JSON dengan jumlah soal yang sama:
                        $rawText
                    """.trimIndent()
                    val verifiedResp = withTimeout(60_000L) {
                        model.generateContent(verifyPrompt)
                    }
                    val verifiedList = verifiedResp.text?.let {
                        normalizeQuestionsFromJson(it, appMode, fw.cogLevels.first())
                    }.orEmpty()
                    if (verifiedList.size == normalized.size) {
                        normalized = verifiedList
                    }
                } catch (auditErr: CancellationException) {
                    throw auditErr
                } catch (auditErr: Exception) {
                    Log.w(TAG, "Dual-pass audit skipped due to secondary error: ${auditErr.message}")
                }
            }

            Result.success(normalized)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Question generation timed out", e)
            Result.failure(IllegalStateException("Waktu peracikan soal habis (60 detik). Silakan coba kembali."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Question generation failed", e)
            Result.failure(e)
        }
    }

    // ================= 3. ZERO-DEFAULT-FALLBACK STRICT VALIDATOR =================

    fun normalizeQuestionsFromJson(rawJson: String, appMode: String, defaultCog: String): List<QuestionItem> {
        return try {
            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("questions") ?: return emptyList()
            val out = mutableListOf<QuestionItem>()

            for (i in 0 until arr.length()) {
                val q = arr.optJSONObject(i) ?: continue
                val type = q.optString("type", "").trim()
                if (type !in listOf("pilgan", "kompleks", "benarsalah", "isian", "likert", "sjt")) continue

                val text = q.optString("text", "").trim()
                if (text.isEmpty()) continue

                val topic = q.optString("topic", if (appMode == "bk") "Dimensi Psikopedagogis" else "Konsep Utama").trim()
                val indicator = q.optString("indicator", "").trim()
                val cognitiveLevel = q.optString("cognitiveLevel", defaultCog).trim().ifEmpty { defaultCog }
                val stimulus = q.optString("stimulus", "").trim()
                val explanation = q.optString("explanation", "Pembahasan terverifikasi.").trim()

                when (type) {
                    "likert" -> {
                        val options = listOf(
                            "Sangat Tidak Sesuai (STS)",
                            "Tidak Sesuai (TS)",
                            "Ragu-ragu / Netral (N)",
                            "Sesuai (S)",
                            "Sangat Sesuai (SS)"
                        )
                        val wArr = q.optJSONArray("optionWeights")
                        val weights = if (wArr != null && wArr.length() == 5) {
                            (0 until 5).map { idx -> wArr.optInt(idx, (idx + 1) * 20).coerceIn(0, 100) }
                        } else {
                            listOf(20, 40, 60, 80, 100)
                        }
                        val bestIdx = weights.indices.maxByOrNull { weights[it] } ?: 4
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                options = options,
                                optionWeights = weights,
                                singleAnswer = bestIdx,
                                explanation = explanation
                            )
                        )
                    }
                    "sjt" -> {
                        val optArr = q.optJSONArray("options") ?: continue
                        val options = (0 until minOf(4, optArr.length())).map { optArr.optString(it, "").trim() }.filter { it.isNotEmpty() }
                        if (options.size < 4) continue
                        val wArr = q.optJSONArray("optionWeights")
                        val weights = if (wArr != null && wArr.length() >= 4) {
                            (0 until 4).map { idx -> wArr.optInt(idx, 100 - idx * 25).coerceIn(0, 100) }
                        } else {
                            listOf(100, 75, 50, 25)
                        }
                        val bestIdx = weights.indices.maxByOrNull { weights[it] } ?: 0
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                options = options,
                                optionWeights = weights,
                                singleAnswer = bestIdx,
                                explanation = explanation
                            )
                        )
                    }
                    "benarsalah" -> {
                        val stArr = q.optJSONArray("statements") ?: continue
                        val statements = (0 until minOf(4, stArr.length())).map { stArr.optString(it, "").trim() }.filter { it.isNotEmpty() }
                        if (statements.size < 2) continue
                        val bArr = q.optJSONArray("boolAnswers") ?: q.optJSONArray("answer") ?: continue
                        if (bArr.length() < statements.size) continue
                        val bools = mutableListOf<Boolean>()
                        var validBools = true
                        for (j in statements.indices) {
                            val rawVal = bArr.get(j)
                            when (rawVal) {
                                is Boolean -> bools.add(rawVal)
                                is String -> {
                                    if (rawVal.trim().equals("true", true) || rawVal.trim().equals("benar", true)) bools.add(true)
                                    else if (rawVal.trim().equals("false", true) || rawVal.trim().equals("salah", true)) bools.add(false)
                                    else validBools = false
                                }
                                else -> validBools = false
                            }
                        }
                        if (!validBools) continue
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                statements = statements,
                                boolAnswers = bools,
                                explanation = explanation
                            )
                        )
                    }
                    "isian" -> {
                        val accArr = q.optJSONArray("acceptedAnswers")
                        val accepted = mutableListOf<String>()
                        if (accArr != null) {
                            for (j in 0 until accArr.length()) {
                                val s = accArr.optString(j, "").trim()
                                if (s.isNotEmpty()) accepted.add(s)
                            }
                        }
                        if (accepted.isEmpty()) continue
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                acceptedAnswers = accepted,
                                explanation = explanation
                            )
                        )
                    }
                    "kompleks" -> {
                        val optArr = q.optJSONArray("options") ?: continue
                        val options = (0 until minOf(5, optArr.length())).map { optArr.optString(it, "").trim() }.filter { it.isNotEmpty() }
                        if (options.size < 4) continue
                        val mArr = q.optJSONArray("multiAnswer") ?: q.optJSONArray("answer") ?: continue
                        if (mArr.length() == 0) continue
                        val indices = mutableListOf<Int>()
                        var allValid = true
                        for (j in 0 until mArr.length()) {
                            val idx = mArr.optInt(j, -1)
                            if (idx in options.indices) indices.add(idx) else allValid = false
                        }
                        if (!allValid || indices.isEmpty()) continue
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                options = options,
                                multiAnswer = indices.distinct().sorted(),
                                explanation = explanation
                            )
                        )
                    }
                    "pilgan" -> {
                        val optArr = q.optJSONArray("options") ?: continue
                        val options = (0 until minOf(5, optArr.length())).map { optArr.optString(it, "").trim() }.filter { it.isNotEmpty() }
                        if (options.size < 4) continue
                        // Strict validation: never default an out-of-range or missing answer to 0
                        if (!q.has("singleAnswer") && !q.has("answer")) continue
                        val ansIdx = if (q.has("singleAnswer") && !q.isNull("singleAnswer")) {
                            q.optInt("singleAnswer", -1)
                        } else {
                            q.optInt("answer", -1)
                        }
                        if (ansIdx !in options.indices) continue
                        out.add(
                            QuestionItem(
                                type = type,
                                topic = topic,
                                indicator = indicator,
                                cognitiveLevel = cognitiveLevel,
                                stimulus = stimulus,
                                text = text,
                                options = options,
                                singleAnswer = ansIdx,
                                explanation = explanation
                            )
                        )
                    }
                }
            }
            out
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions JSON", e)
            emptyList()
        }
    }

    private fun getBlueprintSlots(blueprint: BlueprintData?, startIdx: Int, count: Int): List<BlueprintRow>? {
        val items = blueprint?.items?.takeIf { it.isNotEmpty() } ?: return null
        val expanded = mutableListOf<BlueprintRow>()
        for (item in items) {
            val c = max(1, item.count)
            repeat(c) { expanded.add(item) }
        }
        if (expanded.isEmpty()) return null
        return (0 until count).map { i -> expanded[(startIdx + i) % expanded.size] }
    }

    // ================= 4. PAK REI DIAGNOSTIC & BK ANALYSIS =================

    override suspend fun generatePakReiAnalysis(
        modelName: String,
        appMode: String,
        participantName: String,
        frameworkName: String,
        stage: String,
        subjectName: String,
        scorePercent: Int,
        mistakesList: List<String>,
        cogStats: List<DimensionStat>
    ): Pair<String, List<PakReiSummary>> {
        val dimProfile = cogStats.joinToString(", ") { "${it.label}: ${it.percentage}%" }
        val prompt = if (appMode == "bk") {
            """
                Berikan laporan konseling psikopedagogis NON-KLINIS untuk siswa:
                Nama: $participantName | Instrumen: $frameworkName ($stage) | Profil Dimensi: $dimProfile.
                Tuliskan komentar empatik Konselor Pak Rei (2-3 kalimat) di 'comment' dan 2-3 rekomendasi pengembangan diri/jurusan/strategi belajar di 'summaries'.
            """.trimIndent()
        } else {
            """
                Berikan evaluasi diagnostik akademik akhir untuk peserta:
                Nama: $participantName | Kurikulum: $frameworkName ($stage) | Mapel: $subjectName | Skor Polytomous: $scorePercent% | Profil Level Kognitif: $dimProfile.
                Topik yang perlu penguatan: ${if (mistakesList.isEmpty()) "Tidak ada (Sempurna 100%)" else mistakesList.joinToString(", ")}.
                Tuliskan komentar tajam & memotivasi Pak Rei (2-3 kalimat) di 'comment' dan langkah remedi konkret per topik di 'summaries'.
            """.trimIndent()
        }

        return try {
            val model = selectAnalysisModel(modelName)
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty analysis")
            val json = JSONObject(raw)
            val comment = json.optString("comment", "Evaluasi selesai. Pelajari peta kemampuan dan ulasan butir di bawah ini.")
            val sumArr = json.optJSONArray("summaries") ?: JSONArray()
            val summaries = (0 until sumArr.length()).map { i ->
                val o = sumArr.getJSONObject(i)
                PakReiSummary(
                    topic = o.optString("topic", "Rekomendasi Pak Rei"),
                    explanation = o.optString("explanation", "")
                )
            }
            comment to summaries
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pak Rei AI analysis fallback used: ${e.message}")
            if (appMode == "bk") {
                "Profil potensi dan kecenderungan psikopedagogis Anda telah dipetakan secara utuh. Gunakan peta radar di bawah sebagai bahan diskusi bersama Guru BK dan orang tua." to listOf(
                    PakReiSummary(
                        topic = "Rekomendasi Pengembangan Diri & Karir",
                        explanation = "Fokuskan pemilihan kegiatan ekstrakurikuler, mata pelajaran pilihan, dan eksplorasi program studi pada 2–3 dimensi tertinggi pada grafik Radar di atas."
                    ),
                    PakReiSummary(
                        topic = "Strategi Regulasi & Keseimbangan Belajar",
                        explanation = "Pertahankan ritme belajar terstruktur (teknik Pomodoro 25/5 menit) dan komunikasikan tantangan akademik sejak dini."
                    )
                )
            } else {
                val fallbackSummaries = mistakesList.map { t ->
                    PakReiSummary(
                        topic = t,
                        explanation = "Topik \"$t\" berada pada prioritas latihan. Tinjau kembali konsep kunci pada kartu pembahasan butir di bawah."
                    )
                }
                "Skor terkalibrasi Anda mencapai $scorePercent%. Perhatikan peta Radar Kemampuan dan pembagian Zona ZPD di bawah untuk memprioritaskan materi penguatan." to fallbackSummaries
            }
        }
    }

    // ================= 5. BUILT-IN SAMPLE BLUEPRINT & DEMO QUESTIONS =================

    override fun getSampleBlueprint(frameworkKey: String, subjectName: String): BlueprintData {
        val fw = CurriculumCatalog.FRAMEWORKS[frameworkKey] ?: CurriculumCatalog.FRAMEWORKS["tka"]!!
        return BlueprintData(
            detectedSubject = subjectName.ifBlank { "Matematika & Literasi Sains" },
            styleDNA = "Stimulus berbasis data tabel & kasus kontekstual (120–180 kata), pengecoh homogen berbasis miskonsepsi umum siswa, proporsi HOTS 40%.",
            items = listOf(
                BlueprintRow(
                    domain = "Literasi Data & Statistika",
                    indicator = "Disajikan tabel tren kapasitas energi, peserta didik dapat menghitung persentase perubahan tertinggi",
                    cognitiveLevel = fw.cogLevels.getOrElse(1) { "L2 - Aplikasi" },
                    questionType = "pilgan",
                    count = 1
                ),
                BlueprintRow(
                    domain = "Aljabar & Fungsi Kuadrat",
                    indicator = "Menganalisis titik potong, sumbu simetri, dan nilai ekstrem fungsi kuadrat dari beberapa pernyataan",
                    cognitiveLevel = fw.cogLevels.getOrElse(2) { "L3 - Penalaran" },
                    questionType = "kompleks",
                    count = 1
                ),
                BlueprintRow(
                    domain = "Inkuiri & Eksperimen Ilmiah",
                    indicator = "Mengevaluasi kebenaran simpulan variabel bebas/terikat dari data percobaan laju fotosintesis",
                    cognitiveLevel = fw.cogLevels.getOrElse(2) { "L3 - Penalaran" },
                    questionType = "benarsalah",
                    count = 1
                ),
                BlueprintRow(
                    domain = "Pemecahan Masalah Numerik",
                    indicator = "Menentukan nilai rata-rata gabungan dari penambahan data baru secara eksak",
                    cognitiveLevel = fw.cogLevels.getOrElse(1) { "L2 - Aplikasi" },
                    questionType = "isian",
                    count = 1
                )
            )
        )
    }

    override fun getDemoQuestions(appMode: String): List<QuestionItem> {
        return if (appMode == "bk") {
            listOf(
                QuestionItem(
                    type = "likert",
                    topic = "Minat Eksplorasi Ilmiah & Analitik",
                    indicator = "Mengukur ketertarikan memecahkan teka-teki ilmiah, data, dan cara kerja sistem (Holland Investigative)",
                    cognitiveLevel = "Investigative (I)",
                    stimulus = "Bayangkan kamu diberikan proyek bebas di sekolah selama satu semester penuh.",
                    text = "Saya sangat menikmati kegiatan meneliti penyebab suatu fenomena, menganalisis data, atau mencari tahu cara kerja algoritma/sains secara mendalam.",
                    options = listOf(
                        "Sangat Tidak Sesuai (STS)",
                        "Tidak Sesuai (TS)",
                        "Ragu-ragu / Netral (N)",
                        "Sesuai (S)",
                        "Sangat Sesuai (SS)"
                    ),
                    optionWeights = listOf(20, 40, 60, 80, 100),
                    singleAnswer = 4,
                    explanation = "Skor tinggi pada dimensi Investigative (I) menunjukkan kecocokan kuat pada bidang Sains, Kedokteran, Teknik, Riset, Data Science, dan Teknologi."
                ),
                QuestionItem(
                    type = "sjt",
                    topic = "Empati & Kolaborasi Sosial",
                    indicator = "Memetakan kecenderungan membantu, mengajar, dan berempati pada orang lain (Holland Social & KSE)",
                    cognitiveLevel = "Social (S)",
                    stimulus = "Saat menjelang ujian akhir, kamu melihat seorang teman sekelas tampak sangat cemas dan tertinggal pada materi yang kebetulan sudah kamu kuasai.",
                    text = "Tindakan manakah yang paling menggambarkan respon alami dirimu dalam situasi tersebut?",
                    options = listOf(
                        "Mengajaknya belajar bersama dan menjelaskan konsep dasar secara bertahap dengan sabar.",
                        "Meminjamkan catatan rangkuman milikmu agar ia bisa mempelajarinya sendiri di rumah.",
                        "Menyarankannya untuk bertanya langsung kepada guru mata pelajaran terkait.",
                        "Fokus menyelesaikan target belajarmu sendiri terlebih dahulu agar nilaimu tetap aman."
                    ),
                    optionWeights = listOf(100, 75, 55, 30),
                    singleAnswer = 0,
                    explanation = "Dimensi Social (S) menonjolkan kemampuan interpersonal, empati, komunikasi edukatif, serta cocok pada bidang Psikologi, Pendidikan, Kesehatan Masyarakat, dan Konseling."
                ),
                QuestionItem(
                    type = "likert",
                    topic = "Kreativitas & Ekspresi Gagasan",
                    indicator = "Mengukur preferensi terhadap inovasi, desain visual, sastra, dan solusi non-konvensional (Holland Artistic)",
                    cognitiveLevel = "Artistic (A)",
                    stimulus = "",
                    text = "Saya lebih suka tugas yang membebaskan saya merancang ide orisinal, desain visual, cerita, atau pendekatan kreatif dibandingkan tugas dengan aturan kaku yang berulang.",
                    options = listOf(
                        "Sangat Tidak Sesuai (STS)",
                        "Tidak Sesuai (TS)",
                        "Ragu-ragu / Netral (N)",
                        "Sesuai (S)",
                        "Sangat Sesuai (SS)"
                    ),
                    optionWeights = listOf(20, 40, 60, 80, 100),
                    singleAnswer = 4,
                    explanation = "Dimensi Artistic (A) mencerminkan daya imajinasi dan inovasi tinggi untuk bidang Arsitektur, Desain Komunikasi Visual, Media Digital, UI/UX, dan Industri Kreatif."
                ),
                QuestionItem(
                    type = "sjt",
                    topic = "Kepemimpinan & Inisiatif Proyek",
                    indicator = "Mengukur keberanian mengambil inisiatif, negosiasi, dan manajemen tim (Holland Enterprising)",
                    cognitiveLevel = "Enterprising (E)",
                    stimulus = "Kelompok tugasmu mengalami kebuntuan karena tidak ada anggota yang berani membagi peran dan menentukan tenggat waktu.",
                    text = "Apa langkah pertama yang paling nyaman kamu ambil?",
                    options = listOf(
                        "Mengambil inisiatif memimpin diskusi, memetakan kekuatan tiap anggota, dan menyepakati jadwal kerja.",
                        "Mengusulkan pembuatan tabel jadwal rapi dan mencatat progres administrasi kelompok.",
                        "Langsung mengerjakan bagian teknis/praktik yang paling sulit sendirian.",
                        "Menunggu kesepakatan anggota lain agar tidak terlihat mendominasi."
                    ),
                    optionWeights = listOf(100, 75, 55, 30),
                    singleAnswer = 0,
                    explanation = "Dimensi Enterprising (E) berkaitan erat dengan kepemimpinan, manajemen bisnis, hukum, kebijakan publik, dan kewirausahaan."
                )
            )
        } else {
            listOf(
                QuestionItem(
                    type = "pilgan",
                    topic = "Literasi Data & Statistika",
                    indicator = "Disajikan tabel produksi energi terbarukan, peserta didik dapat menentukan persentase kenaikan tertinggi (L2 - Aplikasi)",
                    cognitiveLevel = "L2 - Aplikasi",
                    stimulus = "| Tahun | PLTS (MW) | PLTB (MW) |\n|---|---|---|\n| 2024 | 400 | 250 |\n| 2025 | 520 | 300 |\n| 2026 | 650 | 360 |",
                    text = "Berdasarkan tabel kapasitas terpasang di atas, berapakah persentase kenaikan kapasitas PLTS dari tahun 2024 ke tahun 2025?",
                    options = listOf("20%", "25%", "30%", "35%"),
                    singleAnswer = 2,
                    explanation = "Kenaikan = 520 - 400 = 120 MW. Persentase kenaikan = (120 / 400) × 100% = 30%."
                ),
                QuestionItem(
                    type = "kompleks",
                    topic = "Penalaran Kuantitatif & Aljabar",
                    indicator = "Menganalisis sifat-sifat fungsi kuadrat dari persamaan yang diberikan (L3 - Penalaran / HOTS)",
                    cognitiveLevel = "L3 - Penalaran",
                    stimulus = "Diketahui fungsi kuadrat f(x) = x² - 6x + 8 yang memotong sumbu-X di dua titik dan memiliki titik puncak (xp, yp).",
                    text = "Manakah di antara pernyataan berikut yang BENAR mengenai grafik fungsi f(x)? (Pilih semua jawaban yang benar)",
                    options = listOf(
                        "Grafik memotong sumbu-X di titik (2, 0) dan (4, 0)",
                        "Sumbu simetri grafik berada pada garis x = 3",
                        "Nilai minimum fungsi adalah y = 1",
                        "Grafik memotong sumbu-Y di titik (0, 8)"
                    ),
                    multiAnswer = listOf(0, 1, 3),
                    explanation = "Faktorisasi x² - 6x + 8 = (x - 2)(x - 4), sehingga memotong sumbu-X di (2,0) dan (4,0). Sumbu simetri xp = 6/2 = 3. Nilai minimum yp = 3² - 18 + 8 = -1 (bukan 1). Titik potong sumbu-Y adalah f(0) = 8."
                ),
                QuestionItem(
                    type = "benarsalah",
                    topic = "Literasi Saintifik (PISA / TKA)",
                    indicator = "Mengevaluasi kebenaran simpulan berdasarkan data percobaan laju fotosintesis (L3 - Penalaran)",
                    cognitiveLevel = "L3 - Penalaran",
                    stimulus = "Seorang peneliti mengukur jumlah gelembung oksigen yang dihasilkan tanaman air Hydrilla selama 5 menit pada jarak lampu dan penambahan NaHCO₃ yang berbeda:\n• Perlakuan P (Jarak 20 cm, tanpa NaHCO₃): 18 gelembung\n• Perlakuan Q (Jarak 10 cm, tanpa NaHCO₃): 42 gelembung\n• Perlakuan R (Jarak 10 cm, +0,5% NaHCO₃): 75 gelembung",
                    text = "Tentukan apakah setiap pernyataan simpulan berikut Benar atau Salah berdasarkan data percobaan!",
                    statements = listOf(
                        "Intensitas cahaya berbanding terbalik dengan jarak sumber cahaya ke tanaman.",
                        "Penambahan NaHCO₃ berfungsi meningkatkan ketersediaan CO₂ terlarut.",
                        "Variabel terikat dalam percobaan tersebut adalah jarak lampu ke tanaman.",
                        "Kombinasi cahaya kuat dan peningkatan CO₂ menghasilkan laju fotosintesis tertinggi."
                    ),
                    boolAnswers = listOf(true, true, false, true),
                    explanation = "Variabel terikat adalah jumlah gelembung O₂ (laju fotosintesis), sedangkan jarak lampu dan penambahan NaHCO₃ adalah variabel bebas."
                ),
                QuestionItem(
                    type = "isian",
                    topic = "Konsep Dasar & Pemahaman (L1)",
                    indicator = "Menghitung nilai rata-rata gabungan dari dua kelompok data (L1 - Pengetahuan & Pemahaman)",
                    cognitiveLevel = "L1 - Pengetahuan & Pemahaman",
                    stimulus = "Rata-rata nilai tryout dari 8 siswa adalah 75. Setelah 2 siswa susulan digabungkan, rata-rata nilai seluruh 10 siswa tersebut menjadi 78.",
                    text = "Berapakah rata-rata nilai dari 2 siswa yang mengikuti tryout susulan tersebut? (Ketik angka saja)",
                    acceptedAnswers = listOf("90", "90.0", "90,0"),
                    explanation = "Total nilai 8 siswa = 8 × 75 = 600. Total nilai 10 siswa = 10 × 78 = 780. Jumlah nilai 2 siswa susulan = 780 - 600 = 180, sehingga rata-ratanya = 180 / 2 = 90."
                )
            )
        }
    }
}
