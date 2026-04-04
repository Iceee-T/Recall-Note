plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.notewise"
    compileSdk = 35

    packaging {
        resources {
            // Exclude these to prevent "Duplicate Files" and "Logging Configuration" errors
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/log4j-provider.properties"
            excludes += "org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
            excludes += "META-INF/log4j-provider.properties"
            excludes += "org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
        }
    }

    defaultConfig {
        applicationId = "com.example.notewise"
        minSdk = 26
        targetSdk = 34
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

    // Add this if it's not there to help with modern Java features
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.ai.client.generativeai:common:0.9.0")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:31.1-android")
    // Use the main POI OOXML library
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // REDIRECT Log4j to SLF4J (This fixes the StatusLogger error)
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.17.1")
    implementation("org.slf4j:slf4j-android:1.7.36")

    // Force the use of a specific version of Log4j-api that avoids the 'metafactory' error
    implementation("org.apache.logging.log4j:log4j-api:2.17.1")

    // Keep your PDFBox
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
