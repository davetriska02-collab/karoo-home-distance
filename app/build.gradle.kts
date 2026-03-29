plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "uk.co.triska.karoohome"
    compileSdk = 34

    defaultConfig {
        applicationId = "uk.co.triska.karoohome"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Karoo extension SDK - fetched from GitHub Packages (see gradle.properties for credentials)
    implementation(libs.hammerhead.karoo.ext)

    // AndroidX core
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.bundles.androidx.lifeycle)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)

    // Glance for remote views (used by the bearing data field graphical widget)
    implementation(libs.androidx.glance.appwidget)

    // Unit tests
    testImplementation(libs.junit)
}
