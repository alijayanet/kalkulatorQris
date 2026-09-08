# 📱 Kalkulator QRIS Dinamis & POS Thermal Printer Bluetooth

Aplikasi kasir kalkulator pintar Android modern untuk konversi **QRIS Statis menjadi QRIS Dinamis (EMVCo Nasional)** secara instan, lengkap dengan kalkulator multi-item, cetak struk kasir Bluetooth Thermal Printer (ESC/POS 58mm/80mm), input nama pelanggan, kustomisasi header/footer struk, notifikasi real-time, dan penyimpanan lokal aman terenkripsi AES-256.

---

## 📥 Download APK Siap Pakai (Direct Download)

Bagi Anda yang ingin langsung menginstal aplikasi di smartphone Android tanpa perlu build dari source code:

[![Download APK](https://img.shields.io/badge/Download_APK-KalkulatorQRIS.apk-2ea44f?style=for-the-badge&logo=android)](https://github.com/alijayanet/kalkulatorQris/raw/main/KalkulatorQRIS.apk)

👉 **[Klik Di Sini Untuk Download File APK (KalkulatorQRIS.apk)](https://github.com/alijayanet/kalkulatorQris/raw/main/KalkulatorQRIS.apk)** *(Ukuran: ~21.95 MB)*

---

## 🌟 Fitur Utama

- 🧮 **Kalkulator Kasir Multi-Item:** Perhitungan cepat (penjumlahan, desimal, perkalian item seperti `2.5 x 5000 + 10000`).
- ⚡ **Generator QRIS Dinamis Otomatis:** Menghasilkan kode QRIS sesuai standar EMVCo Bank Indonesia & ASPI dengan nominal presisi tanpa biaya admin tambahan.
- 👤 **Input Nama Pelanggan:** Nama pembeli otomatis dicatat dan ditampilkan pada nota cetak serta riwayat transaksi.
- 🖨️ **Cetak Struk Thermal Bluetooth (ESC/POS):** Mendukung semua tipe printer kasir thermal Bluetooth 58mm & 80mm (POS-58, Panda, VSC, Iware, EPPOS, dsb).
- 🏷️ **Kustomisasi Header & Footer Struk:**
  - Alamat Toko pada bagian Header Struk
  - Nomor HP / WhatsApp pada Header Struk
  - Pesan penutup (Footer Struk) yang dapat disesuaikan dan disimpan
- 📊 **Dashboard & Laporan Penjualan:** Statistik pendapatan harian, mingguan, bulanan, dan total transaksi masuk.
- 🔒 **Keamanan Tingkat Tinggi:** Data toko & transaksi disimpan secara lokal di perangkat menggunakan Room Database & enkripsi AES-256 GCM.
- 📤 **Ekspor & Berbagi Struk Digital:** Simpan gambar QRIS ke galeri atau bagikan nota tagihan langsung ke WhatsApp pelanggan.

---

## 🛠️ Teknologi yang Digunakan

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room (SQLite) dengan EncryptedSharedPreferences (AES-256 GCM)
- **Printer Engine:** ESC/POS Bluetooth SPP RFCOMM
- **QR Engine:** ZXing QR Code Generator & Parser (EMVCo TLV Format)
- **Architecture:** MVVM (Model-View-ViewModel) + Kotlin Coroutines & StateFlow

---

## 🚀 Cara Menjalankan & Membangun Project

### Prasyarat
- Android Studio Ladybug / Meerkat atau yang lebih baru
- JDK 21 (direkomendasikan JBR Android Studio)
- Android SDK 35 (Minimum Android 8.0 Oreo / API 26)

### Kompilasi Debug APK
```bash
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk` ➔ `KalkulatorQRIS.apk`

### Kompilasi Release AAB (Google Play Store)
```bash
$env:STORE_PASSWORD = "qriscalc2026"
$env:KEY_PASSWORD = "qriscalc2026"
.\gradlew.bat bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab` ➔ `KalkulatorQRIS-release.aab`

---

## 👨‍💻 Pengembang & Dukungan Teknis

Aplikasi ini dikembangkan dan dikelola oleh:

- **Pengembang:** **ALIJAYA-NET**
- **WhatsApp:** **081947215703** ([Hubungi via WhatsApp](https://wa.me/6281947215703))
- **Repository GitHub:** [https://github.com/alijayanet/kalkulatorQris](https://github.com/alijayanet/kalkulatorQris)

---

## 📄 Lisensi
Hak Cipta © 2026 **ALIJAYA-NET**. Semua hak dilindungi undang-undang.
