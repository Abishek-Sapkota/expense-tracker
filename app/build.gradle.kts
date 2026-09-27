import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystoreFile = rootProject.file("keystore.properties")
val allowDebugSigning = project.hasProperty("allowDebugSigning")

android {
    namespace = "com.abi.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abi.expensetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from keystore.properties (gitignored):
    //   storeFile=/path/to/release.jks  storePassword=…  keyAlias=…  keyPassword=…
    // Without it a release build fails (see the check at the end of this file) instead of
    // quietly signing with the debug key: a phone that once took a debug-signed release
    // refuses the properly signed one without an uninstall, and with allowBackup off an
    // uninstall wipes the database. -PallowDebugSigning signs a throwaway one on purpose.
    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                val props = Properties().apply { keystoreFile.inputStream().use { load(it) } }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinking and optimisation: Compose is several times faster in a
            // minified, non-debuggable build, and unused icons and code are dropped.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
                ?: if (allowDebugSigning) signingConfigs.getByName("debug") else null
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

    // Room's exported schemas as debug assets, so the Robolectric migration test can build
    // each old version (Robolectric reads the variant's assets, not the test set's). A few
    // kilobytes in the debug APK only; the release build does not carry them.
    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // The design's Material Symbols (tabs, settings rows, row badges) as sharp vectors.
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Installs the ahead-of-time baseline profiles that Compose and the other AndroidX
    // libraries ship, even for a sideloaded APK, so first launch and scrolling are not
    // running interpreted code.
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    // Room, JsonReader and the backup code on the JVM, without a device: an instrumented
    // test needs a debug APK on the phone, which cannot install over the signed release.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Fail an unsigned release loudly rather than leave an unsigned APK to be missed.
tasks.matching { it.name == "assembleRelease" || it.name == "installRelease" }.configureEach {
    doFirst {
        if (!keystoreFile.exists() && !allowDebugSigning) {
            throw GradleException(
                "No keystore.properties: refusing to build a release that later updates " +
                    "could not install over. Add it, or pass -PallowDebugSigning."
            )
        }
    }
}
