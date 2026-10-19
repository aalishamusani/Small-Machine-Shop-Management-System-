plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Address of the LIVE backend, used only by the release (installable APK)
// build. Pass it when building:  gradlew assembleRelease -PsmsmsBaseUrl=https://your-app.onrender.com/
val liveBaseUrl: String = (project.findProperty("smsmsBaseUrl") as String?) ?: "https://CHANGE-ME.onrender.com/"

// Address the DEBUG build talks to. Default = the emulator's alias for this PC.
// For a same-Wi-Fi demo on a real phone, pass the PC's address when building:
//   gradlew assembleDebug -PsmsmsDebugBaseUrl=http://172.16.87.142:4000/
val debugBaseUrl: String = (project.findProperty("smsmsDebugBaseUrl") as String?) ?: "http://10.0.2.2:4000/"

android {
    namespace = "com.smsms.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smsms.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // 10.0.2.2 is a special address the Android EMULATOR maps to the
        // host PC's own localhost. It only works from the emulator — a
        // real phone on the same Wi-Fi would need the PC's actual LAN IP
        // (e.g. 192.168.x.x) here instead.
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:4000/\"")
    }

    buildTypes {
        // Debug = what Android Studio's Run button builds: talks to the
        // backend on your own PC (emulator address above) over plain http.
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
            manifestPlaceholders["usesCleartext"] = "true"
        }
        // Release = the APK you share: talks to the live backend over https
        // only (plain http is switched off).
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"$liveBaseUrl\"")
            manifestPlaceholders["usesCleartext"] = "false"
            // Demo only: sign with the auto-generated debug key so the APK
            // installs without creating a keystore. A real Play Store
            // release would need its own signing key.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Retrofit talks to the Express backend over HTTP; Gson converts the
    // JSON responses into our Kotlin data classes (model/Order.kt etc).
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Persists the login token (and current user's role) to disk so a
    // user isn't logged out every time the app process is killed.
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
