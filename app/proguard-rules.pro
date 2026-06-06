# Keep Room entities (used by FTS + serialization)
-keep class com.articlevault.data.db.entity.** { *; }
-keepattributes *Annotation*

# ML Kit
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# Jsoup HTML parser
-keep class org.jsoup.** { *; }

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Keep WorkManager workers
-keep class com.articlevault.worker.** { *; }

# LiteRT-LM
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
