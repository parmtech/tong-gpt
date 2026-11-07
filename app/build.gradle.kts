plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "au.parmtech.tonggpt"
    compileSdk = 35
    defaultConfig {
        applicationId = "au.parmtech.tonggpt"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0-dev"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = true }
}
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.webkit:webkit:1.13.0")
    testImplementation("junit:junit:4.13.2")
}
