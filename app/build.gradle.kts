import com.android.build.api.variant.ResValue

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("io.realm.kotlin")
}

android {
    namespace = "io.github.alexispurslane.bloc"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.alexispurslane.bloc"
        minSdk = 31
        targetSdk = 35
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
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
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
            signingConfig = signingConfigs.getByName("debug")
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

    androidComponents {
        onVariants { variant ->
            variant.resValues.put(
                variant.makeResValueKey("string", "app_id"),
                ResValue(variant.applicationId.get())
            )
        }
    }

    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
    }
}

task<Exec>("stopApp") {
    commandLine("adb", "shell", "am", "force-stop", "io.github.alexispurslane.bloc")
}

task<Exec>("appStart") {
    dependsOn("installDebug")
    dependsOn("stopApp")
    commandLine("adb", "shell", "am", "start", "-n", "io.github.alexispurslane.bloc/.MainActivity")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.7.0-rc01")
    implementation("androidx.compose.ui:ui-graphics:1.7.0-rc01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0-rc01")
    implementation("androidx.compose.material3:material3:1.2.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("net.folivo:trixnity-client:4.6.1")
    implementation("net.folivo:trixnity-client-repository-realm:4.6.1")
    implementation("net.folivo:trixnity-client-media-okio-jvm:4.6.1")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-server-resources:2.3.12")

    implementation("org.jetbrains:markdown:0.1.45")
    implementation("io.github.aghajari:AnnotatedText:1.0.3")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    ksp("com.google.dagger:dagger-compiler:2.48")// Dagger compiler
    ksp("com.google.dagger:hilt-compiler:2.48")  // Hilt compiler    ksp("com.google.dagger:hilt-android-compiler:2.44")
    implementation("com.google.dagger:hilt-android:2.49")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("io.realm.kotlin:library-base:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.work:work-multiprocess:2.9.1")

    debugImplementation("org.slf4j:slf4j-api:2.0.15")
    debugImplementation("com.github.tony19:logback-android:3.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
