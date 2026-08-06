# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom rules here that might be needed for your dependencies (e.g., Retrofit, Hilt, Room, etc.)
-keep class com.streamix.data.** { *; }
-keep class com.streamix.scraper.** { *; }

# Fix R8 errors for missing classes
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn com.fasterxml.jackson.databind.ext.Java7SupportImpl
-dontwarn org.mozilla.javascript.**
