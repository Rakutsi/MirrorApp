// App-level build.gradle

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.mirrorapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mirrorapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule",
                "META-INF/groovy-release-info.properties",
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:1.3.0")
    implementation("androidx.compose.material3:material3:1.0.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.3.0")

    // Kalender och nätverk
    implementation("org.mnode.ical4j:ical4j:3.0.24")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.sf.biweekly:biweekly:0.6.8")
    implementation(libs.androidx.work.runtime.ktx)

    // Testning
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Ytterligare beroenden
    implementation("io.ktor:ktor-client-core:2.2.1")
    implementation("io.ktor:ktor-client-cio:2.2.1")
    implementation("io.ktor:ktor-client-serialization:2.2.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21") // Eller den senaste versionen
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.compose.ui:ui:1.1.1")
    implementation("androidx.compose.material3:material3:1.0.0-alpha03")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("androidx.compose.foundation:foundation:1.2.0")
    implementation("androidx.compose.material3:material3:1.0.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.2.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

}
