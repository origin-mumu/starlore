plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.starlore.webview"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.starlore.webview"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
