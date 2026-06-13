# Keep Room entities
-keep class com.expensetracker.data.db.entity.** { *; }
-keepattributes *Annotation*

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Keep WorkManager workers
-keep class com.expensetracker.data.sync.** { *; }

# Google API
-dontwarn com.google.api.**
-keep class com.google.api.** { *; }
-dontwarn com.google.common.**
-keep class com.google.common.** { *; }
-dontwarn org.apache.http.**
-keep class org.apache.http.** { *; }
