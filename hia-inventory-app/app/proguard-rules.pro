# ProGuard rules for the HIA Inventory App

# Keep the application class
-keep class com.example.hia.MainActivity { *; }

# Keep all classes in the UI package
-keep class com.example.hia.ui.** { *; }

# Keep all classes in the navigation package
-keep class com.example.hia.ui.navigation.** { *; }

# Keep all classes in the screens package
-keep class com.example.hia.ui.screens.** { *; }

# Keep all classes in the theme package
-keep class com.example.hia.ui.theme.** { *; }

# Keep all string resources
-keepclassmembers class * {
    @androidx.annotation.StringRes <fields>;
}

# Keep all Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Add any additional rules as needed for libraries or specific classes