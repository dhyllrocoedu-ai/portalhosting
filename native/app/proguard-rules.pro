# Keep server managers
-keep class com.portalhost.app.server.** { *; }

# Keep service
-keep class com.portalhost.app.service.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
