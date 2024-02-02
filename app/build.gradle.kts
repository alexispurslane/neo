import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

val environment = System.getenv()
fun getLocalProperty(key: String) =
    gradleLocalProperties(rootDir).getProperty(key)

fun String.toFile() = File(this)

plugins {
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "io.github.alexispurslane.bloc"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.alexispurslane.bloc"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = getLocalProperty("signing.keyAlias")
                ?: environment["SIGNING_KEY_ALIAS"]
                        ?: error("Cannot find signing key alias")
            storeFile = file(
                getLocalProperty("signing.storeFile")
                    ?: environment["SIGNING_STORE_FILE"]
                    ?: error("Cannot find signing keystore file")
            )
            keyPassword = getLocalProperty("signing.keyPassword")
                ?: environment["SIGNING_KEY_PASSWORD"]
                        ?: error("Cannot find signing key password")
            storePassword = getLocalProperty("signing.storePassword")
                ?: environment["SIGNING_STORE_PASSWORD"]
                        ?: error("Cannot find signing keystore password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

task<Exec>("stopApp") {
    commandLine(
        "adb",
        "shell",
        "am",
        "force-stop",
        "io.github.alexispurslane.bloc"
    )
}

task<Exec>("appStart") {
    dependsOn("installDebug")
    dependsOn("stopApp")
    commandLine(
        "adb",
        "shell",
        "am",
        "start",
        "-n",
        "io.github.alexispurslane.bloc/.MainActivity"
    )
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    implementation("androidx.compose.material3:material3:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("com.google.dagger:hilt-android:2.44")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    implementation("androidx.navigation:navigation-compose:2.6.0")

    implementation("com.halilibo.compose-richtext:richtext-ui:0.17.0")
    implementation("com.halilibo.compose-richtext:richtext-ui-material3:0.17.0")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")

    implementation("androidx.work:work-runtime:2.8.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.work:work-gcm:2.8.1")

    implementation("io.coil-kt:coil:2.4.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("androidx.work:work-multiprocess:2.8.1")

    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.1.1")

    implementation("com.github.Abhimanyu14:emoji-core:1.0.4")
    implementation("androidx.emoji2:emoji2:1.4.0")

    kapt("com.google.dagger:hilt-android-compiler:2.44")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}