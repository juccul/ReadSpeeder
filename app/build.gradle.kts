plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jukul.readspeeder"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jukul.readspeeder"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmarkRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        if (variant.name in setOf("benchmarkRelease", "nonMinifiedRelease")) {
            variant.sources.kotlin?.addStaticSourceDirectory("src/profile/java")
            variant.sources.manifests.addStaticManifestFile("src/profile/AndroidManifest.xml")
        }
    }
}

composeCompiler {
    if (providers.gradleProperty("enableComposeCompilerReports").orNull == "true") {
        reportsDestination = layout.buildDirectory.dir("compose-compiler/reports")
        metricsDestination = layout.buildDirectory.dir("compose-compiler/metrics")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.haze.blur)
    implementation(libs.material)
    implementation(libs.pdfbox.android) {
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
    }
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
