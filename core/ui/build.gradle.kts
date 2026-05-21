plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.alarm.core.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
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
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.animation:animation:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("com.google.dagger:hilt-android:2.48")
    implementation(project(":core:sound"))
    implementation(project(":data:alarm_repository"))
}
