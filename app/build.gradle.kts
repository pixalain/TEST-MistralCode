import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tvlive.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tvlive.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signature release uniquement si un keystore est fourni (CI sur tags v*)
    val keystoreB64: String? = System.getenv("ANDROID_KEYSTORE_B64")
        ?: (project.findProperty("keystoreB64") as? String)
    if (!keystoreB64.isNullOrBlank()) {
        val ksFile = File(buildDir, "signing/keystore.jks")
        ksFile.parentFile.mkdirs()
        ksFile.writeBytes(Base64.getDecoder().decode(keystoreB64))
        signingConfigs.create("release") {
            storeFile = ksFile
            storePassword = System.getenv("ANDROID_STORE_PASSWORD")
                ?: (project.findProperty("storePassword") as? String) ?: ""
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                ?: (project.findProperty("keyAlias") as? String) ?: ""
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                ?: (project.findProperty("keyPassword") as? String) ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (!keystoreB64.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Media3 (ExoPlayer) : lecture HLS + téléchargements hors ligne. Signatures
    // vérifiées contre les sources media3 1.4.1 (package exoplayer.offline).
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-database:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
