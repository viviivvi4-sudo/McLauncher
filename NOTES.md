# Catatan Build

## gradlew / gradle-wrapper.jar
`gradle-wrapper.jar` adalah file BINARY, jadi tidak bisa dibuat sebagai teks di
sini. Workflow `build.yml` sudah disesain untuk **tidak butuh file itu** — ia
memakai action resmi `gradle/actions/setup-gradle` lalu menjalankan `gradle`
langsung (bukan `./gradlew`), jadi build di GitHub Actions tetap jalan tanpa
wrapper jar.

Kalau kamu tetap mau punya `gradlew` untuk build lokal di Termux/PC, jalankan
sekali (butuh Gradle & internet):

    gradle wrapper --gradle-version 8.0

Ini akan men-generate `gradlew`, `gradlew.bat`, dan `gradle-wrapper.jar` yang
sebenarnya.

## URL download JRE & assets
`MainActivity.java` memakai placeholder URL (`https://example.com/...`).
Ganti `JRE_URL` dan `ASSETS_URL` dengan link JRE Android (misalnya build JRE
17 untuk arm64) dan link assets/library Minecraft yang kamu siapkan sendiri.

## Native libraries (liblwjgl.so, libgl4es.so, dll)
Taruh file `.so` kamu di `app/src/main/jniLibs/arm64-v8a/`. Gradle akan
otomatis mem-package-nya ke dalam APK karena `jniLibs.srcDirs` sudah diatur
di `app/build.gradle`.
