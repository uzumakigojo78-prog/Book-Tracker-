plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Optional release signing, supplied by CI secrets. Without them the release
// build falls back to the debug key so it is still installable.
val releaseKeystore = System.getenv("SIGNING_KEYSTORE_PATH")

android {
    namespace = "com.booktracker.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.booktracker.app"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/module-info.class",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    // Home-screen widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    // Claude (AI genres & recommendations)
    implementation("com.anthropic:anthropic-java:2.65.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")

    // Material 3 Expressive APIs (MaterialExpressiveTheme, MotionScheme.expressive, LoadingIndicator...)
    implementation("androidx.compose.material3:material3:1.4.0-alpha15")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
