plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import org.gradle.api.GradleException
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun releaseProperty(name: String): String? =
    (System.getenv(name)
        ?: localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseStoreFilePath = releaseProperty("JARPICK_RELEASE_STORE_FILE")
val releaseStorePassword = releaseProperty("JARPICK_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProperty("JARPICK_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseProperty("JARPICK_RELEASE_KEY_PASSWORD")
val productionAdMobAppId = releaseProperty("ADMOB_APP_ID")
val productionBannerId = releaseProperty("ADMOB_BANNER_ID")
val debugAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val debugBannerId = "ca-app-pub-3940256099942544/9214589741"
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

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

        buildConfigField("String", "ADMOB_PRODUCTION_BANNER_ID", "\"${productionBannerId.orEmpty()}\"")
        buildConfigField("String", "ADMOB_DEBUG_BANNER_ID", "\"$debugBannerId\"")
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"remove_ads_premium\"")
        manifestPlaceholders["adMobApplicationId"] = debugAdMobAppId
    }

    signingConfigs {
        create("releaseEnv") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("Boolean", "ALLOW_DEBUG_PREMIUM_OVERRIDE", "true")
            manifestPlaceholders["adMobApplicationId"] = debugAdMobAppId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "ALLOW_DEBUG_PREMIUM_OVERRIDE", "false")
            manifestPlaceholders["adMobApplicationId"] = productionAdMobAppId.orEmpty()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Keep the fallback for Gradle sync, but validatePlayReleaseConfig
            // blocks any release build from shipping with debug signing.
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

tasks.register("validatePlayReleaseConfig") {
    group = "verification"
    description = "Fails Play release builds unless signing and production AdMob values are configured."

    doLast {
        val errors = mutableListOf<String>()

        if (!hasReleaseSigning) {
            errors += "Configure JARPICK_RELEASE_STORE_FILE, JARPICK_RELEASE_STORE_PASSWORD, JARPICK_RELEASE_KEY_ALIAS, and JARPICK_RELEASE_KEY_PASSWORD."
        }
        if (!releaseStoreFilePath.isNullOrBlank() && !file(releaseStoreFilePath).isFile) {
            errors += "JARPICK_RELEASE_STORE_FILE does not point to a readable keystore: $releaseStoreFilePath"
        }
        if (productionAdMobAppId.isNullOrBlank()) {
            errors += "Configure ADMOB_APP_ID with the production AdMob Android app ID."
        }
        if (productionAdMobAppId == debugAdMobAppId) {
            errors += "ADMOB_APP_ID must not use Google's public test app ID."
        }
        if (productionBannerId.isNullOrBlank()) {
            errors += "Configure ADMOB_BANNER_ID with the production AdMob banner ad unit ID."
        }
        if (productionBannerId == debugBannerId) {
            errors += "ADMOB_BANNER_ID must not use Google's public test banner ad unit ID."
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                "Play release configuration is incomplete:\n" +
                    errors.joinToString(separator = "\n") { "- $it" }
            )
        }
    }
}

fun requestedPlayReleaseArtifact(): Boolean =
    gradle.startParameter.taskNames
        .map { it.substringAfterLast(":").lowercase() }
        .any { it == "bundlerelease" || it == "assemblerelease" }

tasks.configureEach {
    if (requestedPlayReleaseArtifact() && (name == "preReleaseBuild" || name == "bundleRelease" || name == "assembleRelease")) {
        dependsOn("validatePlayReleaseConfig")
    }
}
