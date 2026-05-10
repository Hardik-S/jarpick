plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val releaseStoreFilePath = System.getenv("JARPICK_RELEASE_STORE_FILE")
    ?: localProperties.getProperty("JARPICK_RELEASE_STORE_FILE")
val hasReleaseSigning = !releaseStoreFilePath.isNullOrBlank()

android {
    namespace = "com.batb4016.jarpick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.batb4016.jarpick"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val productionBannerId = localProperties.getProperty("ADMOB_BANNER_ID")
            ?: providers.gradleProperty("ADMOB_BANNER_ID").orNull
            ?: ""
        buildConfigField("String", "ADMOB_PRODUCTION_BANNER_ID", "\"$productionBannerId\"")
        buildConfigField("String", "ADMOB_DEBUG_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"remove_ads_premium\"")
    }

    signingConfigs {
        create("releaseEnv") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = System.getenv("JARPICK_RELEASE_STORE_PASSWORD")
                    ?: localProperties.getProperty("JARPICK_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("JARPICK_RELEASE_KEY_ALIAS")
                    ?: localProperties.getProperty("JARPICK_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("JARPICK_RELEASE_KEY_PASSWORD")
                    ?: localProperties.getProperty("JARPICK_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("Boolean", "ALLOW_DEBUG_PREMIUM_OVERRIDE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "ALLOW_DEBUG_PREMIUM_OVERRIDE", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Local CI-free MVP builds must still produce an AAB. Play uploads
            // should configure releaseEnv with a real keystore before submission.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("releaseEnv")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation("com.android.billingclient:billing-ktx:8.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("app.cash.turbine:turbine:1.2.0")
}
