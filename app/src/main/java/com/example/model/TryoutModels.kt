package com.example.model

import com.example.domain.scoring.CompositeItemScorer
import com.example.domain.scoring.ScaleEquivalenceResolver
import com.example.domain.serialization.TryoutPayloadSerializer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

data class CurriculumFramework(
    val key: String,
    val group: String, // "nasional" | "internasional" | "bk"
    val name: String,
    val badge: String,
    val desc: String,
    val cogLevels: List<String>,
    val stages: Map<String, List<String>>,
    val promptRules: String
)

object CurriculumCatalog {
    val FRAMEWORKS: Map<String, CurriculumFramework> = linkedMapOf(
        "merdeka" to CurriculumFramework(
            key = "merdeka",
            group = "nasional",
            name = "Kurikulum Merdeka (Kemendikdasmen)",
            badge = "MERDEKA",
            desc = "Berbasis Capaian Pembelajaran (CP) per Fase A–F, mengutamakan pemahaman bermakna, konteks nyata, dan penalaran.",
            cogLevels = listOf("C1-Mengingat", "C2-Memahami", "C3-Menerapkan", "C4-Menganalisis", "C5-Mengevaluasi", "C6-Mencipta"),
            stages = linkedMapOf(
                "Fase A (Kelas 1–2 SD/MI)" to listOf("Bahasa Indonesia", "Matematika", "Pendidikan Pancasila", "Seni dan Budaya"),
                "Fase B (Kelas 3–4 SD/MI)" to listOf("Bahasa Indonesia", "Matematika", "IPAS (Ilmu Pengetahuan Alam & Sosial)", "Pendidikan Pancasila", "Bahasa Inggris"),
                "Fase C (Kelas 5–6 SD/MI)" to listOf("Bahasa Indonesia", "Matematika", "IPAS (Ilmu Pengetahuan Alam & Sosial)", "Pendidikan Pancasila", "Bahasa Inggris", "Koding & Kecerdasan Artifisial"),
                "Fase D (Kelas 7–9 SMP/MTs)" to listOf("Matematika", "Bahasa Indonesia", "Bahasa Inggris", "IPA Terpadu", "IPS Terpadu", "Informatika", "Pendidikan Pancasila"),
                "Fase E (Kelas 10 SMA/MA/SMK)" to listOf("Matematika", "Bahasa Indonesia", "Bahasa Inggris", "Fisika", "Kimia", "Biologi", "Sejarah", "Ekonomi", "Geografi", "Sosiologi", "Informatika"),
                "Fase F (Kelas 11–12 SMA/MA/SMK)" to listOf("Matematika Wajib", "Matematika Tingkat Lanjut", "Bahasa Indonesia", "Bahasa Inggris", "Fisika", "Kimia", "Biologi", "Ekonomi", "Sosiologi", "Geografi", "Sejarah", "Informatika", "Projek Kreatif & Kewirausahaan (SMK)")
            ),
            promptRules = "Gunakan acuan Capaian Pembelajaran (CP) Kurikulum Merdeka pada Fase yang dipilih. Soal harus berangkat dari konteks/fenomena yang dekat dengan kehidupan murid Indonesia, menguji pemahaman konsep (bukan sekadar hafalan definisi), serta menyertakan taksonomi kognitif Anderson & Krathwohl (C1–C6)."
        ),
        "tka" to CurriculumFramework(
            key = "tka",
            group = "nasional",
            name = "TKA Resmi (Perka BSKAP 2025/2026)",
            badge = "TKA RESMI",
            desc = "Tes Kemampuan Akademik terstandar nasional: Level Kognitif Pengetahuan & Pemahaman, Aplikasi, dan Penalaran. Bentuk soal: Pilihan Ganda Sederhana, PGK MCMA, dan PGK Kategori (Benar/Salah).",
            cogLevels = listOf("L1 - Pengetahuan & Pemahaman", "L2 - Aplikasi", "L3 - Penalaran"),
            stages = linkedMapOf(
                "SD/MI (Kelas 6)" to listOf("Matematika (Numerasi TKA)", "Bahasa Indonesia (Literasi Membaca TKA)"),
                "SMP/MTs (Kelas 9)" to listOf("Matematika", "Bahasa Indonesia", "Bahasa Inggris", "IPA"),
                "SMA/MA/SMK (Kelas 12 - Wajib & Pilihan)" to listOf("Bahasa Indonesia (Wajib)", "Matematika (Wajib)", "Bahasa Inggris (Wajib)", "Matematika Tingkat Lanjut", "Fisika", "Kimia", "Biologi", "Ekonomi", "Sosiologi", "Geografi", "Sejarah")
            ),
            promptRules = "IKUTI KERANGKA ASESMEN TKA PUSMENDIK KEMENDIKDASMEN SECARA KETAT: 3 Level Kognitif (L1 Pengetahuan & Pemahaman, L2 Aplikasi, L3 Penalaran). Gunakan stimulus/wacana kontekstual atau tabel data. Kombinasikan Pilihan Ganda Sederhana (pilgan), PGK MCMA (kompleks), dan PGK Kategori Benar/Salah (benarsalah)."
        ),
        "akm" to CurriculumFramework(
            key = "akm",
            group = "nasional",
            name = "ANBK - AKM (Literasi & Numerasi)",
            badge = "AKM-ANBK",
            desc = "Asesmen Kompetensi Minimum Pusmendik: Literasi Membaca dan Numerasi dalam konteks Personal, Sosial-Budaya, dan Saintifik.",
            cogLevels = listOf("L1 - Menemukan Informasi / Knowing", "L2 - Interpretasi & Integrasi / Applying", "L3 - Evaluasi & Refleksi / Reasoning"),
            stages = linkedMapOf(
                "Level 1–3 (SD/MI Kelas 5)" to listOf("AKM Literasi Membaca", "AKM Numerasi"),
                "Level 4 (SMP/MTs Kelas 8)" to listOf("AKM Literasi Membaca", "AKM Numerasi"),
                "Level 5 (SMA/MA/SMK Kelas 11)" to listOf("AKM Literasi Membaca", "AKM Numerasi")
            ),
            promptRules = "Rancang soal berstandar AKM Pusmendik: Setiap soal WAJIB memiliki 'stimulus' dengan konteks Personal, Sosial-Budaya, atau Saintifik. Gunakan 4 bentuk soal AKM: pilgan, kompleks, benarsalah, dan isian."
        ),
        "snbt" to CurriculumFramework(
            key = "snbt",
            group = "nasional",
            name = "UTBK - SNBT (Seleksi Nasional PTN)",
            badge = "UTBK-SNBT",
            desc = "Standar BPPP: Tes Potensi Skolastik (TPS) dan Tes Literasi + Penalaran Matematika dengan bobot HOTS & IRT tinggi.",
            cogLevels = listOf("Pemahaman Konsep", "Analisis & Inferensi Logis", "Evaluasi & Problem Solving HOTS"),
            stages = linkedMapOf(
                "SNBT - Tes Potensi Skolastik (TPS)" to listOf("Penalaran Umum (Induktif, Deduktif, Kuantitatif)", "Pengetahuan dan Pemahaman Umum (PPU)", "Pemahaman Bacaan dan Menulis (PBM)", "Pengetahuan Kuantitatif (PK)"),
                "SNBT - Literasi & Penalaran Matematika" to listOf("Literasi dalam Bahasa Indonesia", "Literasi dalam Bahasa Inggris", "Penalaran Matematika")
            ),
            promptRules = "Gunakan standar UTBK-SNBT BPPP terkini: pengecoh sangat rapat dan menguji jebakan logika/kuantitatif. Sertakan soal analisis pernyataan dan isian singkat numerik."
        ),
        "k13" to CurriculumFramework(
            key = "k13",
            group = "nasional",
            name = "Kurikulum 2013 (KI-KD & Taksonomi Bloom)",
            badge = "KURIKULUM 2013",
            desc = "Berbasis Kompetensi Inti (KI 3 & KI 4) dan Kompetensi Dasar (KD) dengan pengkategorian LOTS (C1-C2), MOTS (C3), dan HOTS (C4-C6).",
            cogLevels = listOf("LOTS (C1-C2)", "MOTS (C3)", "HOTS (C4-C6)"),
            stages = linkedMapOf(
                "SD/MI (Kelas 4–6)" to listOf("Tematik Terpadu", "Matematika", "Bahasa Indonesia", "IPA", "IPS", "PPKn"),
                "SMP/MTs (Kelas 7–9)" to listOf("Matematika", "Bahasa Indonesia", "Bahasa Inggris", "IPA", "IPS", "PPKn"),
                "SMA/MA IPA (Kelas 10–12)" to listOf("Matematika Wajib", "Matematika Peminatan", "Fisika", "Kimia", "Biologi", "Bahasa Indonesia", "Bahasa Inggris"),
                "SMA/MA IPS (Kelas 10–12)" to listOf("Ekonomi", "Geografi", "Sosiologi", "Sejarah Peminatan", "Matematika Wajib", "Bahasa Indonesia", "Bahasa Inggris")
            ),
            promptRules = "Susun soal sesuai KI-KD Kurikulum 2013 Revisi. Cantumkan indikator pencapaian kompetensi (IPK) dengan kata kerja operasional (KKO) Taksonomi Bloom yang terukur."
        ),
        "pisa" to CurriculumFramework(
            key = "pisa",
            group = "internasional",
            name = "OECD PISA (Reading, Math & Science Literacy)",
            badge = "OECD PISA",
            desc = "Programme for International Student Assessment (OECD) untuk usia 15 tahun. Mengukur kemampuan menerapkan pengetahuan pada masalah dunia nyata (Proficiency Level 1–6).",
            cogLevels = listOf("PISA Level 1–2 (Baseline)", "PISA Level 3–4 (Intermediate)", "PISA Level 5–6 (Advanced High-Order)"),
            stages = linkedMapOf(
                "PISA Core Domains (Age 15 / Grade 9–10)" to listOf("Mathematical Literacy (Formulate, Employ, Interpret)", "Reading Literacy (Locate, Understand, Evaluate & Reflect)", "Scientific Literacy (Explain Phenomena, Design Inquiry, Interpret Data)", "Creative Thinking")
            ),
            promptRules = "Rancang unit soal bergaya OECD PISA resmi: WAJIB diawali stimulus otentik dunia nyata (data statistik, studi kasus ilmiah, kebijakan publik). Soal menguji proses kognitif PISA pada Level 1 s.d. Level 6."
        ),
        "timss" to CurriculumFramework(
            key = "timss",
            group = "internasional",
            name = "IEA TIMSS (Mathematics & Science)",
            badge = "IEA TIMSS",
            desc = "Trends in International Mathematics and Science Study (IEA) untuk Grade 4 dan Grade 8 dengan 3 domain kognitif internasional: Knowing, Applying, Reasoning.",
            cogLevels = listOf("TIMSS Knowing", "TIMSS Applying", "TIMSS Reasoning"),
            stages = linkedMapOf(
                "TIMSS Grade 4 (SD Kelas 4)" to listOf("Mathematics (Number, Measurement & Geometry, Data)", "Science (Life Science, Physical Science, Earth Science)"),
                "TIMSS Grade 8 (SMP Kelas 8)" to listOf("Mathematics (Number, Algebra, Geometry, Data & Probability)", "Science (Biology, Chemistry, Physics, Earth Science)")
            ),
            promptRules = "Gunakan kerangka asesmen IEA TIMSS: bagi soal secara proporsional pada domain kognitif Knowing (35%), Applying (40%), dan Reasoning (25%)."
        ),
        "cambridge" to CurriculumFramework(
            key = "cambridge",
            group = "internasional",
            name = "Cambridge Assessment (IGCSE, AS/A Level)",
            badge = "CAMBRIDGE",
            desc = "Cambridge International Education dengan Assessment Objectives: AO1 Knowledge with Understanding, AO2 Handling Information & Problem Solving, AO3 Experimental/Analytical Skills.",
            cogLevels = listOf("AO1 - Knowledge & Understanding", "AO2 - Handling Info & Problem Solving", "AO3 - Analysis, Evaluation & Investigation"),
            stages = linkedMapOf(
                "Cambridge Primary & Lower Secondary (Stage 6–9)" to listOf("Mathematics (0862)", "Science (0893)", "English as a Second Language"),
                "Cambridge IGCSE / O Level (Grade 9–10)" to listOf("Mathematics (0580)", "Additional Mathematics (0606)", "Physics (0625)", "Chemistry (0620)", "Biology (0610)", "Economics (0455)", "Computer Science (0478)"),
                "Cambridge International AS & A Level (Grade 11–12)" to listOf("Mathematics Pure & Mechanics (9709)", "Physics (9702)", "Chemistry (9701)", "Biology (9700)", "Economics (9708)", "Computer Science (9618)")
            ),
            promptRules = "Susun soal berstandar Cambridge International Examinations (CAIE): gunakan command words resmi Cambridge (State, Calculate, Explain, Suggest, Deduce, Determine) serta pemetaan AO1, AO2, dan AO3."
        ),
        "ib" to CurriculumFramework(
            key = "ib",
            group = "internasional",
            name = "International Baccalaureate (IB MYP & DP)",
            badge = "IB WORLD",
            desc = "Kurikulum IB berbasis inkuiri dan pemahaman konseptual (Standard Level & Higher Level) dengan kriteria penilaian A–D.",
            cogLevels = listOf("Criterion A - Knowing & Understanding", "Criterion B/C - Investigating & Communicating", "Criterion D - Thinking Critically / Real-World Transfer"),
            stages = linkedMapOf(
                "IB MYP (Middle Years Programme Year 4–5)" to listOf("Mathematics", "Integrated Sciences", "Individuals and Societies", "Language and Literature"),
                "IB Diploma Programme (DP SL / HL)" to listOf("Mathematics: Analysis & Approaches (AA)", "Mathematics: Applications & Interpretation (AI)", "Physics", "Chemistry", "Biology", "Economics")
            ),
            promptRules = "Rancang soal bergaya IB Examination Paper 1/2: tekankan koneksi antar-konsep, interpretasi data/grafik eksperimental, dan aplikasi pada konteks global."
        ),
        "sat" to CurriculumFramework(
            key = "sat",
            group = "internasional",
            name = "College Board Digital SAT & AP",
            badge = "SAT / AP",
            desc = "Standar masuk universitas internasional: Digital SAT (Reading & Writing, Math) dan Advanced Placement (AP).",
            cogLevels = listOf("Foundation", "Medium", "Advanced (Routing Hard Module)"),
            stages = linkedMapOf(
                "Digital SAT" to listOf("SAT Reading and Writing", "SAT Math (Algebra, Advanced Math, Data Analysis, Geometry & Trig)"),
                "Advanced Placement (AP)" to listOf("AP Calculus AB/BC", "AP Physics 1 / C", "AP Chemistry", "AP Biology", "AP Microeconomics / Macroeconomics", "AP Computer Science A")
            ),
            promptRules = "Ikuti format College Board Digital SAT / AP: untuk SAT Reading & Writing gunakan 1 passage ringkas per soal; untuk SAT Math kombinasikan multiple-choice 4 opsi dan Student-Produced Response (isian numerik)."
        ),
        "bk_riasec" to CurriculumFramework(
            key = "bk_riasec",
            group = "bk",
            name = "BK Peminatan Karir & Jurusan (Holland RIASEC) [BETA]",
            badge = "BK RIASEC",
            desc = "Memetakan 6 tipe kepribadian-vokasional Holland (Realistic, Investigative, Artistic, Social, Enterprising, Conventional) untuk rekomendasi peminatan SMA/SMK dan jurusan kuliah.",
            cogLevels = listOf("Realistic (R)", "Investigative (I)", "Artistic (A)", "Social (S)", "Enterprising (E)", "Conventional (C)"),
            stages = linkedMapOf(
                "SMP/MTs (Pemilihan SMA/SMK & Rumpun)" to listOf("Eksplorasi Minat Karir & Pemilihan SMA/SMK"),
                "SMA/MA/SMK (Pemilihan Mapel Pilihan & Prodi PTN/PTS)" to listOf("Pemetaan Kode Holland RIASEC & Rekomendasi Jurusan Kuliah")
            ),
            promptRules = "Rancang instrumen asesmen psikopedagogis Holland RIASEC (NON-KLINIS). Gunakan tipe soal 'likert' (5 tingkat kesesuaian diri, optionWeights [20, 40, 60, 80, 100]) dan 'sjt' (Situational Judgment Test 4 opsi perilaku nyata dengan bobot masing-masing). Petakan setiap butir ke salah satu dimensi di 'cognitiveLevel': Realistic (R), Investigative (I), Artistic (A), Social (S), Enterprising (E), atau Conventional (C) secara merata."
        ),
        "bk_vark" to CurriculumFramework(
            key = "bk_vark",
            group = "bk",
            name = "BK Diagnostik Gaya Belajar & Kognitif (VARK + Metakognisi) [BETA]",
            badge = "BK VARK",
            desc = "Memetakan preferensi modalitas belajar (Visual, Auditory, Read/Write, Kinesthetic) serta regulasi diri (Self-Regulated Learning) sesuai Asesmen Diagnostik Non-Kognitif Kurikulum Merdeka.",
            cogLevels = listOf("Visual (V)", "Auditory (A)", "Read-Write (R)", "Kinesthetic (K)", "Metakognisi & Regulasi Diri"),
            stages = linkedMapOf(
                "SD / SMP / SMA Sederajat" to listOf("Asesmen Diagnostik Non-Kognitif Gaya Belajar & Strategi Belajar Efektif")
            ),
            promptRules = "Rancang instrumen diagnostik gaya belajar VARK + Metakognisi menggunakan kombinasi soal 'sjt' dan 'likert'. Petakan setiap butir ke salah satu dimensi: Visual (V), Auditory (A), Read-Write (R), Kinesthetic (K), atau Metakognisi & Regulasi Diri."
        ),
        "bk_wellbeing" to CurriculumFramework(
            key = "bk_wellbeing",
            group = "bk",
            name = "BK Kesiapan Mental, Motivasi & Kejenuhan Belajar (Academic Wellbeing) [BETA]",
            badge = "BK WELLBEING",
            desc = "Mengukur 5 pilar kesejahteraan psikologis akademik siswa: Growth Mindset, Motivasi Intrinsik, Resiliensi Akademik, Manajemen Waktu, dan Daya Dukung Emosional (Deteksi Dini Burnout).",
            cogLevels = listOf("Growth Mindset", "Motivasi Intrinsik", "Resiliensi Akademik", "Manajemen Waktu & Fokus", "Keseimbangan Emosi (Anti-Burnout)"),
            stages = linkedMapOf(
                "SMP / SMA / SMK / Persiapan Ujian Akhir" to listOf("Evaluasi Kesiapan Mental Menghadapi Ujian & Pencegahan Academic Burnout")
            ),
            promptRules = "Rancang instrumen kesejahteraan belajar (Academic Wellbeing) yang ramah remaja, empatik, dan non-menghakimi menggunakan tipe 'likert' dan 'sjt'. Petakan secara seimbang ke 5 dimensi: Growth Mindset, Motivasi Intrinsik, Resiliensi Akademik, Manajemen Waktu & Fokus, serta Keseimbangan Emosi (Anti-Burnout)."
        ),
        "bk_sosial" to CurriculumFramework(
            key = "bk_sosial",
            group = "bk",
            name = "BK Kematangan Sosial-Emosional (CASEL & Profil Pelajar Pancasila) [BETA]",
            badge = "BK SOSIAL-EMOSIONAL",
            desc = "Memetakan 5 kompetensi sosial-emosional (Kesadaran Diri, Manajemen Diri, Kesadaran Sosial/Empati, Keterampilan Relasi, dan Pengambilan Keputusan Bertanggung Jawab).",
            cogLevels = listOf("Kesadaran Diri", "Manajemen Diri", "Empati & Kesadaran Sosial", "Keterampilan Relasi", "Keputusan Bertanggung Jawab"),
            stages = linkedMapOf(
                "SD / SMP / SMA Sederajat" to listOf("Pemetaan Kompetensi Sosial-Emosional (KSE) & Karakter")
            ),
            promptRules = "Rancang situasi dilema sosial-sekolah sehari-hari (peer pressure, kerja kelompok, empati, integritas) menggunakan tipe 'sjt' dan 'likert' untuk mengukur 5 dimensi CASEL."
        )
    )

    val STUDENT_GRADE_OPTIONS: List<String> = listOf(
        "Kelas 12 SMA/MA/SMK",
        "Kelas 11 SMA/MA/SMK",
        "Kelas 10 SMA/MA/SMK",
        "Kelas 9 SMP/MTs",
        "Kelas 8 SMP/MTs",
        "Kelas 7 SMP/MTs",
        "Kelas 6 SD/MI",
        "Kelas 5 SD/MI",
        "Umum / Alumni"
    )

    fun frameworksForMode(mode: String): List<CurriculumFramework> {
        val targetGroup = when (mode) {
            "internasional" -> "internasional"
            "bk" -> "bk"
            else -> "nasional"
        }
        return FRAMEWORKS.values.filter { fw ->
            if (mode == "reverse") fw.group != "bk" else fw.group == targetGroup
        }
    }

    /**
     * Automatically selects the most appropriate curriculum stage/phase for a given framework
     * based on the student's account gradeClass (Kelas), so the student doesn't have to
     * manually re-configure their grade level on every session.
     */
    fun matchStageForGrade(frameworkKey: String, gradeClass: String): String {
        val fw = FRAMEWORKS[frameworkKey] ?: return ""
        val stages = fw.stages.keys.toList()
        if (stages.isEmpty()) return ""
        if (gradeClass.isBlank()) return stages.first()

        val g = gradeClass.uppercase()
        val isSd = g.contains("SD") || g.contains("MI") || g.contains("KELAS 5") || g.contains("KELAS 6")
        val isSmp = g.contains("SMP") || g.contains("MTS") || g.contains("KELAS 7") || g.contains("KELAS 8") || g.contains("KELAS 9")
        val isKelas10 = g.contains("KELAS 10")
        val isSma = g.contains("SMA") || g.contains("SMK") || g.contains("KELAS 11") || g.contains("KELAS 12") || isKelas10

        val matched = when {
            isSd -> stages.firstOrNull {
                it.contains("SD", ignoreCase = true) ||
                    it.contains("Fase C", ignoreCase = true) ||
                    it.contains("Grade 4", ignoreCase = true) ||
                    it.contains("Primary", ignoreCase = true)
            }
            isSmp -> stages.firstOrNull {
                it.contains("SMP", ignoreCase = true) ||
                    it.contains("Fase D", ignoreCase = true) ||
                    it.contains("Grade 8", ignoreCase = true) ||
                    it.contains("Grade 9", ignoreCase = true) ||
                    it.contains("MYP", ignoreCase = true)
            }
            isKelas10 -> stages.firstOrNull {
                it.contains("Fase E", ignoreCase = true) ||
                    it.contains("Kelas 10", ignoreCase = true) ||
                    it.contains("Grade 9–10", ignoreCase = true) ||
                    it.contains("IGCSE", ignoreCase = true)
            }
            isSma -> stages.firstOrNull {
                it.contains("Fase F", ignoreCase = true) ||
                    it.contains("SMA", ignoreCase = true) ||
                    it.contains("Kelas 11", ignoreCase = true) ||
                    it.contains("Kelas 12", ignoreCase = true) ||
                    it.contains("A Level", ignoreCase = true) ||
                    it.contains("DP", ignoreCase = true)
            }
            else -> null
        }
        return matched ?: stages.lastOrNull() ?: stages.first()
    }
}

data class BlueprintRow(
    val domain: String = "Materi Pokok",
    val indicator: String = "Menganalisis konsep sesuai kisi-kisi",
    val cognitiveLevel: String = "L2 - Aplikasi",
    val questionType: String = "pilgan", // pilgan | kompleks | benarsalah | isian
    val count: Int = 1
)

data class BlueprintData(
    val detectedSubject: String = "Matematika & Literasi Sains",
    val styleDNA: String = "Stimulus berbasis data tabel & kasus kontekstual (120–180 kata), pengecoh homogen berbasis miskonsepsi umum siswa.",
    val items: List<BlueprintRow> = emptyList()
)

data class QuestionItem(
    val type: String = "pilgan", // pilgan | kompleks | benarsalah | isian | likert | sjt
    val topic: String = "Konsep Utama",
    val indicator: String = "",
    val cognitiveLevel: String = "Aplikasi (L2)",
    val stimulus: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val statements: List<String> = emptyList(),
    val optionWeights: List<Int> = emptyList(),
    val singleAnswer: Int = 0,
    val multiAnswer: List<Int> = emptyList(),
    val boolAnswers: List<Boolean> = emptyList(),
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = ""
)

data class UserAnswerState(
    val singleChoice: Int? = null,
    val multiChoices: List<Int> = emptyList(),
    val boolChoices: List<Boolean?> = emptyList(),
    val textAnswer: String = ""
) {
    fun isAnswered(q: QuestionItem?): Boolean {
        if (q == null) return false
        return when (q.type) {
            "pilgan", "likert", "sjt" -> singleChoice != null
            "kompleks" -> multiChoices.isNotEmpty()
            "benarsalah" -> boolChoices.size >= q.statements.size && q.statements.indices.all { boolChoices.getOrNull(it) != null }
            "isian" -> textAnswer.trim().isNotEmpty()
            else -> false
        }
    }
}

data class DimensionStat(
    val label: String,
    val got: Double,
    val max: Double
) {
    val percentage: Int
        get() = if (max > 0.0) ((got / max) * 100.0).roundToInt().coerceIn(0, 100) else 0
}

data class PakReiSummary(
    val topic: String,
    val explanation: String
)

data class ItemScoreResult(
    val got: Double,
    val max: Double,
    val fullCorrect: Boolean
)

data class QuestionReviewItem(
    val index: Int,
    val question: QuestionItem,
    val userAnswerText: String,
    val correctAnswerText: String,
    val gotScore: Double,
    val maxScore: Double,
    val fullCorrect: Boolean
)

// ================= FIRESTORE DATA CLASSES =================

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val authMethod: String = "google", // "google" | "email" | "facebook"
    val nisn: String = "",
    val institution: String = "",
    val gradeClass: String = "",
    val photoBase64: String = "",
    val loginCount: Long = 1L,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LoginHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val authMethod: String = "google", // "google" | "email" | "facebook"
    val deviceInfo: String = "Android",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object PendingAuthProfileStore {
    @Volatile
    var pendingMethod: String = "google"

    @Volatile
    var pendingEmail: String = ""

    @Volatile
    var pendingDisplayName: String = ""

    @Volatile
    var pendingNisn: String = ""

    @Volatile
    var pendingInstitution: String = ""

    @Volatile
    var pendingGradeClass: String = ""

    @Volatile
    var pendingPhotoBase64: String = ""

    fun recordPendingAuth(
        method: String,
        email: String = "",
        displayName: String = "",
        nisn: String = "",
        institution: String = "",
        gradeClass: String = "",
        photoBase64: String = ""
    ) {
        pendingMethod = method
        pendingEmail = email.trim()
        pendingDisplayName = displayName.trim()
        pendingNisn = nisn.trim()
        pendingInstitution = institution.trim()
        pendingGradeClass = gradeClass.trim()
        pendingPhotoBase64 = photoBase64.trim()
    }

    fun consumeOrDefault(
        fallbackEmail: String,
        fallbackDisplayName: String
    ): Triple<String, String, String> {
        val method = pendingMethod.ifBlank { "google" }
        val email = pendingEmail.ifBlank { fallbackEmail }.ifBlank { "pengguna@smarttryout.id" }
        val name = pendingDisplayName.ifBlank { fallbackDisplayName }.ifBlank {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        return Triple(method, email, name)
    }
}

@IgnoreExtraProperties
data class ActiveExamSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appMode: String = "nasional",
    val participantName: String = "",
    val frameworkKey: String = "merdeka",
    val frameworkName: String = "",
    val stage: String = "",
    val subjectName: String = "",
    val topicFocus: String = "",
    val difficultyCode: String = "Standar",
    val scoringMethod: String = "polytomous",
    val lang: String = "id",
    val durationMinutes: Long = 45L,
    val totalTarget: Long = 20L,
    val currentIdx: Long = 0L,
    val timerSeconds: Long = 2700L,
    val questionsPayload: String = "[]",
    val answersPayload: String = "[]",
    val doubtsPayload: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamResultRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appMode: String = "nasional",
    val participantName: String = "",
    val frameworkKey: String = "merdeka",
    val frameworkName: String = "",
    val stage: String = "",
    val subjectName: String = "",
    val scoringMethod: String = "polytomous",
    val scorePercent: Long = 0L,
    val earnedPoints: Double = 0.0,
    val maxPoints: Double = 0.0,
    val abilityIndexScore: Long = 500L,
    val logitIndex: Double = 0.0,
    val scaleBadge: String = "",
    val scaleDisclaimer: String = "",
    val durationTakenMinutes: Long = 1L,
    val pakReiComment: String = "",
    val radarPayload: String = "[]",
    val topicPayload: String = "[]",
    val summariesPayload: String = "[]",
    val reviewPayload: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedBlueprintRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val detectedSubject: String = "",
    val frameworkKey: String = "tka",
    val stage: String = "",
    val styleDNA: String = "",
    val totalItemsCount: Long = 20L,
    val itemsPayload: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ================= PSYCHOMETRIC SCORING & SERIALIZATION FACADE (SOLID) =================

/**
 * Clean facade that delegates:
 * - Item scoring to [CompositeItemScorer] (OCP & LSP)
 * - Scale equivalence to [ScaleEquivalenceResolver] (OCP & LSP)
 * - Payload serialization to [TryoutPayloadSerializer] (SRP)
 */
object PsychometricEngine {

    private val defaultItemScorer = CompositeItemScorer()
    private val defaultScaleResolver = ScaleEquivalenceResolver()

    fun getCognitiveWeight(appMode: String, cogLabel: String): Double {
        if (appMode == "bk") return 1.0
        val c = cogLabel.uppercase()
        if (c.contains("L3") || c.contains("C4") || c.contains("C5") || c.contains("C6") ||
            c.contains("PENALARAN") || c.contains("REASONING") || c.contains("HOTS") ||
            c.contains("AO3") || c.contains("LEVEL 5") || c.contains("LEVEL 6") || c.contains("ADVANCED")
        ) {
            return 1.5
        }
        if (c.contains("L2") || c.contains("C3") || c.contains("APLIKASI") || c.contains("APPLYING") ||
            c.contains("MOTS") || c.contains("AO2") || c.contains("LEVEL 3") || c.contains("LEVEL 4") || c.contains("MEDIUM")
        ) {
            return 1.25
        }
        return 1.0
    }

    fun computeItemScore(
        appMode: String,
        scoringMethod: String,
        q: QuestionItem,
        a: UserAnswerState
    ): ItemScoreResult {
        val w = if (scoringMethod == "polytomous") getCognitiveWeight(appMode, q.cognitiveLevel) else 1.0
        return defaultItemScorer.score(
            question = q,
            answer = a,
            cognitiveWeight = w,
            isStrict = scoringMethod == "strict"
        )
    }

    fun computeEstimatedAbilityIndex(scorePercent: Int): Pair<Double, Int> {
        val p = (scorePercent / 100.0).coerceIn(0.04, 0.96)
        val rawLogit = ln(p / (1.0 - p)).coerceIn(-3.0, 3.0)
        val logitIndex = (rawLogit * 100.0).roundToInt() / 100.0
        val indexScore = (500.0 + logitIndex * 100.0).roundToInt()
        return logitIndex to indexScore
    }

    fun getScaleEquivalent(
        appMode: String,
        frameworkKey: String,
        scorePercent: Int,
        cogStats: List<DimensionStat>
    ): Pair<String, String> =
        defaultScaleResolver.resolve(appMode, frameworkKey, scorePercent, cogStats)

    fun formatUserAnswerText(q: QuestionItem, a: UserAnswerState): String {
        val letters = listOf("A", "B", "C", "D", "E")
        return when (q.type) {
            "pilgan", "likert", "sjt" -> {
                val idx = a.singleChoice
                if (idx != null && idx in q.options.indices) {
                    "${letters.getOrElse(idx) { "${idx + 1}" }}. ${q.options[idx]}"
                } else {
                    "Belum dijawab"
                }
            }
            "kompleks" -> {
                if (a.multiChoices.isEmpty()) "Belum dijawab"
                else a.multiChoices.mapNotNull { idx ->
                    q.options.getOrNull(idx)?.let { "${letters.getOrElse(idx) { "${idx + 1}" }}. $it" }
                }.joinToString("\n")
            }
            "isian" -> a.textAnswer.trim().ifEmpty { "Belum dijawab" }
            "benarsalah" -> {
                if (a.boolChoices.isEmpty() || a.boolChoices.all { it == null }) "Belum dijawab"
                else q.statements.mapIndexed { i, stmt ->
                    val b = a.boolChoices.getOrNull(i)
                    val label = when (b) {
                        true -> "Benar"
                        false -> "Salah"
                        null -> "Kosong"
                    }
                    "• $stmt — [$label]"
                }.joinToString("\n")
            }
            else -> "-"
        }
    }

    fun formatCorrectAnswerText(q: QuestionItem): String {
        val letters = listOf("A", "B", "C", "D", "E")
        return when (q.type) {
            "likert" -> "Refleksi Diri (Bobot: ${q.optionWeights.joinToString(" / ")})"
            "sjt" -> {
                val best = q.singleAnswer.coerceIn(0, max(0, q.options.size - 1))
                "Respon Adaptif: ${letters.getOrElse(best) { "${best + 1}" }}. ${q.options.getOrElse(best) { "" }}"
            }
            "pilgan" -> {
                val ans = q.singleAnswer.coerceIn(0, max(0, q.options.size - 1))
                "${letters.getOrElse(ans) { "${ans + 1}" }}. ${q.options.getOrElse(ans) { "" }}"
            }
            "kompleks" -> q.multiAnswer.mapNotNull { idx ->
                q.options.getOrNull(idx)?.let { "${letters.getOrElse(idx) { "${idx + 1}" }}. $it" }
            }.joinToString("\n")
            "isian" -> q.acceptedAnswers.joinToString(" / ")
            "benarsalah" -> q.statements.mapIndexed { i, stmt ->
                val b = q.boolAnswers.getOrNull(i) ?: false
                "• $stmt — [${if (b) "Benar" else "Salah"}]"
            }.joinToString("\n")
            else -> "-"
        }
    }

    // --- JSON Serialization Delegates (SRP via TryoutPayloadSerializer) ---

    fun serializeQuestions(list: List<QuestionItem>): String =
        TryoutPayloadSerializer.serializeQuestions(list)

    fun deserializeQuestions(jsonStr: String): List<QuestionItem> =
        TryoutPayloadSerializer.deserializeQuestions(jsonStr)

    fun serializeAnswers(list: List<UserAnswerState>): String =
        TryoutPayloadSerializer.serializeAnswers(list)

    fun deserializeAnswers(jsonStr: String, targetSize: Int): List<UserAnswerState> =
        TryoutPayloadSerializer.deserializeAnswers(jsonStr, targetSize)

    fun serializeDoubts(list: List<Boolean>): String =
        TryoutPayloadSerializer.serializeDoubts(list)

    fun deserializeDoubts(jsonStr: String, targetSize: Int): List<Boolean> =
        TryoutPayloadSerializer.deserializeDoubts(jsonStr, targetSize)

    fun serializeDimensionStats(list: List<DimensionStat>): String =
        TryoutPayloadSerializer.serializeDimensionStats(list)

    fun deserializeDimensionStats(jsonStr: String): List<DimensionStat> =
        TryoutPayloadSerializer.deserializeDimensionStats(jsonStr)

    fun serializeSummaries(list: List<PakReiSummary>): String =
        TryoutPayloadSerializer.serializeSummaries(list)

    fun deserializeSummaries(jsonStr: String): List<PakReiSummary> =
        TryoutPayloadSerializer.deserializeSummaries(jsonStr)

    fun serializeBlueprintItems(list: List<BlueprintRow>): String =
        TryoutPayloadSerializer.serializeBlueprintItems(list)

    fun deserializeBlueprintItems(jsonStr: String): List<BlueprintRow> =
        TryoutPayloadSerializer.deserializeBlueprintItems(jsonStr)

    fun serializeReviewItems(list: List<QuestionReviewItem>): String =
        TryoutPayloadSerializer.serializeReviewItems(list)

    fun deserializeReviewItems(jsonStr: String): List<QuestionReviewItem> =
        TryoutPayloadSerializer.deserializeReviewItems(jsonStr)
}
