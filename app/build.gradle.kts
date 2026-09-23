plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android { namespace="com.example.zihinkutusu"; compileSdk=35
    defaultConfig { applicationId="com.example.zihinkutusu"; minSdk=23; targetSdk=35; versionCode=3; versionName="3.0" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
