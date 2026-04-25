import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
}

val splitApks = !project.hasProperty("noSplits")
val abiFilterList = (properties["ABI_FILTERS"] as? String)?.split(';').orEmpty()
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

// Release signing is sourced from (in order of precedence):
//   1. keystore.properties at the repo root (gitignored)
//   2. LOTUS_KEYSTORE_FILE / _PASSWORD / LOTUS_KEY_ALIAS / _PASSWORD env vars (CI)
// If neither is configured, release falls back to debug signing with a warning
// so local dev builds still work; never ship that APK to users.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingField(propKey: String, envKey: String): String? =
    (keystoreProps[propKey] as? String) ?: System.getenv(envKey)
val releaseSigningAvailable =
    signingField("storeFile", "LOTUS_KEYSTORE_FILE") != null &&
    signingField("storePassword", "LOTUS_KEYSTORE_PASSWORD") != null &&
    signingField("keyAlias", "LOTUS_KEY_ALIAS") != null &&
    signingField("keyPassword", "LOTUS_KEY_PASSWORD") != null

android {
    namespace = "com.dn0ne.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dn0ne.lotus.community"
        minSdk = 24
        targetSdk = 35
        versionCode = 1_003_005
        versionName = "1.3.5-community"

        if (splitApks) {
            splits {
                abi {
                    isEnable = true
                    reset()
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                    isUniversalApk = true
                }
            }
        } else {
            ndk {
                abiFilters.addAll(abiFilterList)
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val name =
                    if (splitApks) {
                        output.filters
                            .find {
                                it.filterType ==
                                        com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                            }
                            ?.identifier
                    } else {
                        abiFilterList.firstOrNull()
                    }

                val baseAbiCode = abiCodes[name]

                if (baseAbiCode != null) {
                    output.versionCode.set(baseAbiCode + (output.versionCode.getOrElse(0)))
                }
            }
        }
    }

    if (releaseSigningAvailable) {
        signingConfigs {
            create("release") {
                storeFile = file(signingField("storeFile", "LOTUS_KEYSTORE_FILE")!!)
                storePassword = signingField("storePassword", "LOTUS_KEYSTORE_PASSWORD")
                keyAlias = signingField("keyAlias", "LOTUS_KEY_ALIAS")
                keyPassword = signingField("keyPassword", "LOTUS_KEY_PASSWORD")
            }
        }
    } else {
        logger.warn(
            "Lotus: no release keystore configured — release builds will be " +
            "signed with the debug keystore. Do not distribute these APKs. " +
            "See README for keystore.properties setup."
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseSigningAvailable) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "lotus-${defaultConfig.versionName}-${name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Lint runs in "report-only" mode for now, same posture as detekt/ktlint.
    // CI uploads the HTML report as an artifact so findings are visible
    // without gating the build. Burn down, then flip `abortOnError` to true.
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

tasks.withType<com.android.build.gradle.internal.tasks.CompileArtProfileTask> {
    enabled = false
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil)
    implementation(libs.kmpalette.core)
    implementation(libs.materialkolor)
    implementation(libs.jaudiotagger)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.reorderable)
    implementation(libs.scrollbars)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)
}