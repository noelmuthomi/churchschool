plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.noelmuthomi.churchschool"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.noelmuthomi.churchschool"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // The page the app opens (your Apps Script web app)
        buildConfigField(
            "String", "APP_URL",
            "\"https://script.google.com/macros/s/AKfycbzZq8yKnOq_TBpZ9uQAhit2pMY8aWLPR4oB5_0VciHLjEGKIVU4Sf3ZkVZQA4TnWt8j/exec\""
        )
    }

    // Stable signing key (from GitHub secrets) so updates install over older versions
    val ksPath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksPath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
