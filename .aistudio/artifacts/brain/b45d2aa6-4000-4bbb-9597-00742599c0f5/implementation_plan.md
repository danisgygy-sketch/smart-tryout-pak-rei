# Smart Tryout Pak Rei (Edisi Android & Cloud Sync)

Transformasi penuh platform asesmen adaptif **AI Smart Tryout Pak Rei v3.1** dari versi web ke aplikasi native Android modern berbasis Jetpack Compose, dilengkapi autentikasi Google Sign-In, sinkronisasi real-time lintas perangkat via Cloud Firestore, serta pemrosesan AI langsung menggunakan **Firebase AI Logic (Gemini)** dengan proteksi App Check.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Berikut adalah keputusan utama yang telah dikonfirmasi dari preferensi Anda dan siap dieksekusi setelah Anda menyetujui rencana ini:

- **Cakupan Sinkronisasi Cloud Firestore**: Menyinkronkan secara otomatis (1) **Sesi Ujian Aktif** lintas perangkat (soal, jawaban, penanda ragu-ragu, dan sisa waktu), (2) **Riwayat Hasil Ujian & Grafik Radar Kemampuan** beserta analisis diagnostik Pak Rei, dan (3) **Blueprint Kisi-Kisi PDF** hasil Reverse Engineering.
- **Mesin Pemrosesan AI (Firebase AI Logic + Pengaturan Model)**: Menggunakan integrasi native **Firebase AI Logic (`Firebase.ai`)** dengan proteksi **Firebase App Check** tanpa perlu menempelkan API Key manual, dilengkapi dialog pengaturan pemilihan varian model (`gemini-3.8-flash`, `gemini-3.5-flash-lite`, `gemini-4-argon`) dan opsi **Verifikasi Kunci Ganda (Dual-Pass Psychometric Audit)**.
- **Tab Riwayat Cloud & Statistik Perkembangan Siswa**: Menambahkan navigasi ke halaman **Riwayat & Statistik Cloud** yang menampilkan tren capaian skor Polytomous, perkembangan Indeks Kemampuan Estimatif (Logit), koleksi Blueprint tersimpan, dan akses ulang ke laporan diagnostik penuh kapan saja.

---

### 1. Overview & Core Concept

- **What It Does**: Aplikasi evaluasi akademik & psikopedagogis adaptif yang mendukung **4 Mode Asesmen Utama** seperti versi website:
  1. **Kurikulum Nasional**: Kurikulum Merdeka (Fase A–F), TKA Resmi BSKAP 2025/2026, ANBK-AKM (Literasi & Numerasi), UTBK-SNBT (TPS & Literasi), dan Kurikulum 2013.
  2. **Reverse Engineering Kisi-Kisi PDF (Pro)**: Ekstraksi dokumen PDF kisi-kisi/silabus & contoh soal menggunakan Android native `PdfRenderer` + **Gemini Multimodal Vision** untuk membedah matriks indikator, level kognitif, bentuk soal, dan DNA gaya soal menjadi **Blueprint Workbench** interaktif yang dapat diedit dan disimpan ke Cloud.
  3. **Standar Internasional**: OECD PISA, IEA TIMSS, Cambridge Assessment (IGCSE/AS/A Level), IB (MYP & DP), serta Digital SAT & AP dalam pilihan Bahasa Indonesia, English, atau Bilingual.
  4. **Asesmen BK Psikologis [BETA]**: Pemetaan non-klinis standar POP BK Kemendikdasmen meliputi Minat Karir **Holland RIASEC**, Gaya Belajar **VARK + Metakognisi**, **Academic Wellbeing (Anti-Burnout)**, dan **Kompetensi Sosial-Emosional (CASEL)**.
- **Target Audience / Persona**: Siswa SD/SMP/SMA/SMK, peserta persiapan TKA/SNBT/Olimpiade, serta Guru & Konselor BK yang membutuhkan instrumen tryout terkalibrasi dan rekam jejak perkembangan yang tersinkronisasi di cloud.
- **Key Value**: Pengalaman pengerjaan ujian bebas hambatan di perangkat mobile dengan dukungan 6 tipe butir soal (`pilgan`, `kompleks` MCMA partial credit, `benarsalah`, `isian`, `likert`, `sjt`), kalkulasi skor **Polytomous Berbobot Kognitif & Indeks Kemampuan Estimatif (Logit)**, visualisasi **Radar Kemampuan Multi-Dimensi**, pemetaan **Zone of Proximal Development (ZPD)**, serta sinkronisasi otomatis ke akun Google pengguna.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Gerbang Autentikasi Google (Auth Gate)**: Saat aplikasi dibuka, pengguna disambut layar masuk berdesain modern dengan tombol **"Sign in with Google"** (menggunakan Jetpack Credential Manager) serta dukungan auto sign-in.
  2. **Beranda Pemilihan Mode & Banner Sesi Aktif**: Menampilkan 4 kartu mode asesmen utama (Nasional, Reverse Engineering Pro, Internasional, Asesmen BK Beta), lencana status Model AI & Sinkronisasi Cloud di bagian atas, serta kartu **Lanjutkan Sesi Ujian** otomatis jika sistem mendeteksi sesi yang belum selesai di Cloud Firestore.
  3. **Konfigurasi, Blueprint Workbench & Kartu Identitas Peserta**:
     - Pemilihan kerangka kurikulum, jenjang/fase/kelas, mata pelajaran (dengan saran otomatis), fokus topik, kalibrasi level (`Standar`, `HOTS`, `Fondasi`, `Olimpiade`), metode penilaian (`Polytomous` vs `Dikotomis Ketat`), jumlah butir, durasi, dan bahasa pengantar.
     - Pada mode **Reverse Engineering**, pengguna dapat memilih file PDF Kisi-Kisi & Contoh Soal dari penyimpanan perangkat atau memuat dari koleksi Blueprint Cloud, membedahnya dengan AI menjadi tabel **Blueprint Workbench** yang dapat diedit per baris, lalu mengompilasi **Kartu Identitas Ujian Digital (ID Card)** beserta foto profil dan opsi **Inisiasi Sistem Pak Rei (AI)** atau **Simulasi Cepat (Demo)**.
  4. **Ruang Ujian Adaptif & Background Batching**:
     - Header menampilkan identitas peserta, mata pelajaran, penghitung waktu mundur real-time, indikator pemuatan gelombang soal berikutnya di latar belakang, dan tombol Akhiri.
     - Dilengkapi **Peta Navigasi Butir** berwarna (Aktif, Terjawab, Ragu-ragu, Kosong), lencana Level Kognitif & Topik, kotak Indikator & Stimulus (mendukung format tabel & notasi ilmiah), serta komponen input interaktif untuk keenam bentuk soal. Setiap perubahan jawaban langsung disinkronkan ke Cloud Firestore.
  5. **Laporan Diagnostik, Radar Kemampuan & Ulasan Butir**:
     - Menampilkan Kartu Skor Utama dengan animasi angka, lencana penyetaraan skala kurikulum (KKTP, Kategori Pusmendik, Estimasi Skor SNBT/SAT/PISA/TIMSS/Cambridge/IB, atau Kode Dominan Holland RIASEC), dan **Indeks Kemampuan Estimatif (Logit)**.
     - Grafik **Radar Pemetaan Kemampuan** berbasis `Canvas` interaktif (3–6 sumbu), bilah progres penguasaan Level Kognitif & Topik, matriks 3 kolom **Zone of Proximal Development (ZPD)** (Zona Mandiri, Zona ZPD, Zona Scaffolding), komentar mentor/konselor **Pak Rei**, serta akordion **Review Respons & Analisis Butir** lengkap.
  6. **Tab Riwayat Cloud & Statistik Perkembangan Siswa**:
     - Menampilkan ringkasan metrik performa pengguna (total sesi selesai, rata-rata skor Polytomous, indeks kemampuan tertinggi), grafik tren perkembangan skor dari waktu ke waktu, daftar riwayat hasil ujian yang dapat dibuka kembali secara detail, serta daftar Blueprint Kisi-Kisi tersimpan.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Academic Precision & Deep Intelligence** — perpaduan kartu bersih berelevasi halus untuk ruang baca ujian yang nyaman dengan aksen gradien khas Pak Rei pada kartu identitas dan layar laporan diagnostik.
  - *Color Palette & Mood*:
    - Primary Academic Navy (`#1E3A8A`) & Electric Royal Blue (`#2563EB`) untuk elemen utama dan Kurikulum Nasional.
    - Reverse Engineering Violet-Indigo (`#4F46E5` ke `#9333EA`) dengan aksen Gold (`#FACC15`).
    - International Emerald-Teal (`#047857` ke `#0D9488`).
    - Asesmen BK Rose-Amber (`#BE185D` ke `#E11D48` & `#F59E0B`).
    - Diagnostic Dark Slate (`#0F172A` & `#1E293B`) dengan aksen Cyan (`#38BDF8`) dan Emerald (`#22C55E`) pada layar hasil & radar.
  - *Typography & Hierarchy*: Menggunakan keluarga font lokal **Plus Jakarta Sans** untuk judul/badan teks yang tajam dan modern serta **JetBrains Mono** untuk penghitung waktu, ID kartu ujian, dan indeks numerik/logit.
  - *Interactive Feedback & Motion*: Animasi penghitungan skor, indikator pemindaian AI pada cincin profil hasil ujian, transisi halaman yang mulus, dan umpan balik sentuh Material 3 berukuran minimal `48.dp`.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Pemrosesan AI Native dengan Firebase AI Logic & Schema Terstruktur**
  - *Chosen Approach*: Menggunakan SDK `Firebase.ai.generativeModel(...)` dengan `responseMimeType = "application/json"` dan `Schema.obj(...)` yang diproteksi oleh **Firebase App Check**, serta tetap menerapkan validator ketat **Zero Default Answer Fallback** (menolak butir soal yang memiliki kunci tidak valid agar tidak pernah jatuh ke indeks 0 secara diam-diam).
  - *Why*: Menjamin keamanan tingkat produksi tanpa risiko kebocoran API key di sisi klien dan memastikan output JSON soal maupun analisis Pak Rei selalu terstruktur dan terverifikasi.
- **Decision 2: Ekstraksi PDF Kisi-Kisi dengan Native `PdfRenderer` + Gemini Multimodal**
  - *Chosen Approach*: Menggunakan Android `PdfRenderer` (melalui zero-permission system document picker `ActivityResultContracts.OpenDocument`) untuk merender halaman PDF yang dipilih pengguna menjadi citra beresolusi terukur dan mengirimkannya ke model multimodal Gemini (`content { image(bitmap); text(prompt) }`).
  - *Why*: Menangani baik PDF tabel teks maupun PDF hasil scan/foto secara langsung dan akurat di Android tanpa ketergantungan pustaka WebView/DOM eksternal.
- **Decision 3: Arsitektur Cloud Firestore Terisolasi per Pengguna (`users/{userId}/...`)**
  - *Chosen Approach*: Menyimpan sesi ujian aktif, riwayat hasil ujian, dan blueprint kisi-kisi di bawah subkoleksi milik pengguna yang terautentikasi (`users/{userId}/active_sessions`, `users/{userId}/exam_results`, `users/{userId}/blueprints`) dengan aturan keamanan Zero-Trust (`firestore.rules`).
  - *Why*: Memastikan privasi penuh antar peserta, latensi pembacaan real-time yang cepat melalui `snapshots()`, serta kemudahan melanjutkan ujian dari perangkat Android mana pun.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│     1. configureAppCheck(this, intent) [Debug Token / reCAPTCHA]           │
│     2. AuthGateComposable (FirebaseAuth + Jetpack CredentialManager)       │
└───────────────────┬────────────────────────────────────┬───────────────────┘
                    │ Unauthenticated                    │ Authenticated (uid)
                    ▼                                    ▼
      ┌───────────────────────────┐        ┌─────────────────────────────────┐
      │        AuthScreen         │        │       MainTryoutScaffold        │
      │ • Silent Auto Sign-In     │        │ • Top Bar (User, Model, Sync)   │
      │ • Interactive Google Btn  │        │ • Bottom Navigation Bar:        │
      └───────────────────────────┘        │   1. Evaluasi & Asesmen         │
                                           │   2. Riwayat & Statistik Cloud  │
                                           │   3. Blueprint Kisi-Kisi Cloud  │
                                           └────────┬───────────────┬────────┘
                                                    │               │
                          ┌─────────────────────────┘               └──────────────────────┐
                          ▼                                                                ▼
┌───────────────────────────────────────────────────┐            ┌────────────────────────────────────────────┐
│             TryoutSessionViewModel                │            │            CloudSyncRepository             │
│ • Mode & Curriculum Framework State               │            │ • Named DB: R.string.firestore_database_id │
│ • PDF Rendering & Blueprint Workbench State       │◄──────────►│ • /users/{uid}/active_sessions/{sessionId} │
│ • Exam Engine (Timer, Navigator, 6 Question Types)│            │ • /users/{uid}/exam_results/{resultId}     │
│ • Polytomous & Strict Scoring + Logit Calculator  │            │ • /users/{uid}/blueprints/{blueprintId}    │
│ • ZPD & Multi-Axis Radar Data Aggregator          │            │ • Structured handleFirestoreError logging  │
└─────────────────────────┬─────────────────────────┘            └────────────────────────────────────────────┘
                          │
                          ▼
┌───────────────────────────────────────────────────┐
│              PakReiAiLogicRepository              │
│ • Lazy Firebase.ai.generativeModel(selectedModel) │
│ • Multimodal PDF Blueprint Reverse Engineering    │
│ • Batch Question Generation + Dual-Pass Audit     │
│ • Zero-Default-Fallback Psychometric Validator    │
│ • Pak Rei Diagnostic & BK Counseling Generator    │
└───────────────────────────────────────────────────┘
```

- **Data Model & State (Firestore Collections)**:
  - **`/users/{userId}/active_sessions/{sessionId}` (`ActiveExamSession`)**: Menyimpan sesi ujian yang sedang berlangsung (`userId`, `appMode`, `participantName`, `frameworkKey`, `frameworkName`, `stage`, `subjectName`, `difficultyCode`, `scoringMethod`, `lang`, `durationMinutes`, `totalTarget`, `currentIdx`, `timerSeconds`, `questionsJson`, `answersJson`, `doubtsJson`, `createdAt`, `updatedAt`).
  - **`/users/{userId}/exam_results/{resultId}` (`ExamResultRecord`)**: Menyimpan laporan diagnostik ujian yang telah selesai (`userId`, `appMode`, `participantName`, `frameworkKey`, `frameworkName`, `stage`, `subjectName`, `scoringMethod`, `scorePercent`, `earnedPoints`, `maxPoints`, `abilityIndexScore`, `logitIndex`, `scaleBadge`, `scaleDisclaimer`, `durationTakenMinutes`, `pakReiComment`, `radarDimensionsJson`, `topicBreakdownJson`, `summariesJson`, `questionsReviewJson`, `createdAt`, `updatedAt`).
  - **`/users/{userId}/blueprints/{blueprintId}` (`SavedBlueprintRecord`)**: Menyimpan hasil bedah kisi-kisi PDF (`userId`, `title`, `detectedSubject`, `frameworkKey`, `stage`, `styleDNA`, `totalItemsCount`, `itemsJson`, `createdAt`, `updatedAt`).

- **Interactive Component & State Mapping**:
  - **Pemilihan Mode & Kurikulum Dinamis**: Mengubah `frameworkKey` secara reaktif memperbarui daftar `stages` (Jenjang/Fase/Kelas) dan saran `subjects`, serta menyesuaikan opsi bahasa pengantar atau banner Etika Asesmen BK Non-Klinis.
  - **Blueprint Workbench Interaktif**: Pengguna dapat menambah baris indikator, mengubah domain, rumusan indikator, level kognitif, bentuk soal (`pilgan`, `kompleks`, `benarsalah`, `isian`), dan jumlah butir secara langsung, serta menyimpannya ke Cloud Firestore.
  - **Sinkronisasi Jawaban & Sesi Otomatis**: Setiap kali peserta memilih opsi jawaban, menandai ragu-ragu, atau berpindah nomor soal, state sesi diperbarui secara lokal dan disinkronkan ke dokumen `active_sessions/current` milik pengguna di Firestore. Ketika ujian diselesaikan, dokumen sesi aktif dihapus dan dokumen laporan disimpan ke `exam_results`.
  - **Pengaturan Model & Audit Ganda**: Dialog pengaturan memungkinkan pengguna memilih model (`gemini-3.8-flash` default, `gemini-3.5-flash-lite`, atau `gemini-4-argon`) dan mengaktifkan/menonaktifkan **Dual-Pass Psychometric Audit** yang langsung diterapkan pada pemanggilan `PakReiAiLogicRepository`.
