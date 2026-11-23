# PulsePlayer — Android Native Music Player (Apple Music iOS Style)

Proyek aplikasi **Android Native Music Player** lengkap yang dibangun dengan **Kotlin**, **Jetpack Compose (Material 3)**, **AndroidX Media3 (ExoPlayer + MediaSessionService)**, dan **Palette API** — mengusung estetika minimalis khas **Apple Music iOS terbaru** dengan aksen `#FA2D48`, dukungan penuh **Dark Mode & Light Mode**, serta **GitHub Actions CI/CD** siap bangun APK otomatis.

## 1. Desain UI/UX (Apple Music iOS Inspired)

- **Palet Warna & Tipografi iOS**: Aksen merah/pink khas Apple Music (`#FA2D48`), dukungan otomatis **Dark Mode** (`#000000` / `#1C1C1E`) & **Light Mode** (`#FFFFFF` / `#F2F2F7`), sudut membulat halus, dan garis pemisah tipis (*hairline dividers*).
- **Navigasi Bawah Semi-Transparan**: 3 tab bersih — **Library** (dengan *segmented control* **Songs**, **Albums**, **Artists**), **Playlists**, dan **Search**.
- **Floating Mini Player**: Melayang tepat di atas bottom navigation bar, menampilkan thumbnail album, judul lagu (*marquee*), tombol **Play/Pause**, dan **Next**. Mendukung **klik** maupun **swipe ke atas** untuk membuka Now Playing.
- **Full-Screen Now Playing Sheet**:
  - Background dinamis yang diekstrak dari warna dominan Album Art menggunakan **AndroidX Palette API** dengan efek gradasi blur.
  - Cover Album besar di tengah dengan animasi pegas halus (membesar saat **Play**, mengecil saat **Pause** seperti iOS).
  - Progress bar interaktif (*seekable*), **Shuffle**, **Previous**, **Play/Pause**, **Next**, **Repeat**, **Volume Slider** terhubung langsung ke `AudioManager.STREAM_MUSIC`, serta tombol cepat **Equalizer**, **Speed**, **Sleep Timer**, dan **Queue**.
  - Dapat ditutup dengan **swipe ke bawah** pada *drag handle* atau tombol kembali.

## 2. Fitur Utama & AudioFX

- **Pemindai Musik Lokal (`MediaStore.Audio.Media`)**:
  - Memindai otomatis file **MP3, FLAC, M4A, WAV, dan OGG** di penyimpanan internal beserta metadata lengkap (Judul, Artis, Album, Durasi, dan Album Art).
  - Penanganan runtime permission modern: `READ_MEDIA_AUDIO` (+ `POST_NOTIFICATIONS`) pada **Android 13+ (API 33+)** dan `READ_EXTERNAL_STORAGE` pada **Android 12 ke bawah**.
  - Opsi *Muat Lagu Demo Online* jika perangkat/emulator belum memiliki file audio lokal.
- **Background Playback & Notifikasi**:
  - `MusicPlaybackService` berbasis `MediaSessionService` (Media3) menjaga musik tetap berputar saat layar mati atau aplikasi di-*minimize*, lengkap dengan kontrol di Notification Bar & Lock Screen.
- **5-Band Equalizer & Bass Boost (iOS Bottom Sheet)**:
  - Terhubung langsung ke `ExoPlayer.audioSessionId` menggunakan `android.media.audiofx.Equalizer` & `android.media.audiofx.BassBoost`.
  - Toggle On/Off, **7 Preset** (**Normal**, **Pop**, **Rock**, **Jazz**, **Classical**, **Bass Boost**, **Custom**), **5 slider vertikal** per band frekuensi, dan pengatur **Bass Boost**.

## 3. Konfigurasi Build & GitHub Actions CI

- **Toolchain Stabil**: Java **17**, Kotlin `2.0.20`, AGP `8.5.2`, Gradle Wrapper **8.7** lengkap (`gradlew`, `gradlew.bat`, `gradle-wrapper.properties`, `gradle-wrapper.jar`).
- **Android SDK**: `compileSdk = 34`, `minSdk = 26`, `targetSdk = 34`.
- **GitHub Actions**: Workflow siap pakai di [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) yang otomatis menjalankan:
  - `actions/setup-java@v4` (Temurin Java 17)
  - `chmod +x gradlew`
  - `./gradlew testDebugUnitTest` & `./gradlew assembleDebug`
  - Unggah hasil Debug APK melalui `actions/upload-artifact@v4`.

## Cara Build Lokal / GitHub Actions

```bash
# Jalankan unit test
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```

Hasil APK akan tersedia di `app/build/outputs/apk/debug/`.
