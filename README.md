<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Smart Tryout Pak Rei (Android App)

Platform asesmen adaptif AI dengan Kurikulum Nasional & Internasional, Reverse Engineering Kisi-Kisi PDF, Asesmen BK Psikologis, serta sinkronisasi cloud Firebase.

View your app in AI Studio: https://ai.studio/apps/b45d2aa6-4000-4bbb-9597-00742599c0f5

---

## 🚀 Build APK Otomatis (GitHub Actions)
Setiap kali ada commit atau push ke branch `main`, GitHub Actions akan otomatis melakukan kompilasi proyek ini menjadi file Android APK (`.apk`):
1. Buka tab **Actions** di repositori GitHub ini.
2. Pilih workflow **Build Android APK**.
3. Setelah workflow selesai, unduh file APK dari bagian **Artifacts** (*SmartTryout-Debug-APK*).

---

## 💻 Menjalankan Secara Lokal (Android Studio)

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Buka Android Studio.
2. Pilih **Open** dan pilih folder repositori ini.
3. Buat file `.env` dan atur `GEMINI_API_KEY` (lihat `.env.example`).
4. Klik **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
