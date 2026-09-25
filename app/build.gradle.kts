import java.util.Properties
import java.util.Base64

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
signingConfigs {
    create("release") {
        val keystoreBase64 = System.getenv("KEYSTORE_BASE64")

        if (!keystoreBase64.isNullOrBlank()) {
            val keystoreFile = layout.buildDirectory.file("release-signing.jks").get().asFile
            keystoreFile.parentFile.mkdirs()

            keystoreFile.writeBytes(
                Base64.getMimeDecoder().decode(keystoreBase64)
            )

            storeFile = keystoreFile
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "zihinkutusu"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
}
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }



    namespace="com.example.zihinkutusu"
    compileSdk=36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    defaultConfig {
        applicationId="com.zihinkutusu.app"
        minSdk=23
        targetSdk=36
        versionCode=9
        versionName="9.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
