# Proguard rules for WorkoutPlanner
# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep kotlinx serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**

# Keep models
-keep class com.mawusikpodo.workoutplanner.model.** { *; }
-keep class com.mawusikpodo.workoutplanner.data.db.** { *; }
