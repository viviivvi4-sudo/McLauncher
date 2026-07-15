# Jangan obfuscate / optimize apa pun — cuma pakai R8 buat lewatin bug
# "Record desugaring: global synthetics" di jalur dex-per-library.
-dontobfuscate
-dontoptimize
-keep class ** { *; }
-dontwarn **
