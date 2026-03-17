# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# MariaDB JDBC Driver rules
-keep class org.mariadb.jdbc.** { *; }
-keepclassmembers class org.mariadb.jdbc.** { *; }
-dontwarn org.mariadb.jdbc.**

# Keep JDBC interfaces
-keep interface java.sql.** { *; }
-keep class java.sql.** { *; }

# Keep database driver
-keep class * implements java.sql.Driver { *; }

# Keep SSL/TLS classes
-keep class javax.net.ssl.** { *; }
-dontwarn javax.net.ssl.**

# Keep security classes
-keep class javax.security.** { *; }
-dontwarn javax.security.**