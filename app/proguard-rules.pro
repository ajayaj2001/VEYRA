# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.veyra.data.model.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }
