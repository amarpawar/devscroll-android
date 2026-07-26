import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    // On the classpath but not applied here: application is conditional on
    // google-services.json existing, which the plugins block cannot express.
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// App credentials live in local.properties (gitignored) rather than in tracked
// resources. See local.properties.example for the required keys.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(key: String): String = localProperties.getProperty(key) ?: ""

// The Google Services plugin hard-fails when google-services.json is absent, which would
// break the build for anyone who hasn't set up Firebase. Apply it only when the file is
// there; CrashReporter degrades to a no-op at runtime when Firebase isn't initialised.
if (file("google-services.json").exists()) {
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
    pluginManager.apply(libs.plugins.firebase.crashlytics.get().pluginId)
} else {
    logger.lifecycle("google-services.json not found — Crashlytics disabled for this build.")
}

android {
    namespace = "dev.devscroll"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.devscroll"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "google_web_client_id", secret("GOOGLE_WEB_CLIENT_ID"))
    }

    // Only configured when the keystore is actually present, so fresh clones and CI
    // still build (unsigned) instead of failing at configuration time.
    val releaseKeystore = secret("RELEASE_STORE_FILE")
        .takeIf { it.isNotBlank() }
        ?.let { rootProject.file(it) }
        ?.takeIf { it.exists() }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "env"
    productFlavors {
        create("local") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.29.95:8080/\"")
        }
        create("dev") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.devscroll.dev/\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"https://api.devscroll.in/\"")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Image loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Google Sign-In & Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Crash reporting. Always on the classpath so CrashReporter compiles; actual
    // collection depends on google-services.json being present (see above).
    // Deliberately no firebase-analytics: it collects the advertising ID, which the
    // privacy policy disclaims and which would require an EU consent gate.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
