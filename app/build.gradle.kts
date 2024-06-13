
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"//"${libs.versions.symbolProcessingApi}"
}

android {
    namespace = "io.github.lightevent.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.lightevent.app"
        minSdk = 21
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.live.event.bus.x)

    implementation(libs.rxjava)

    implementation(libs.eventbus)
    kapt(libs.eventbus.annotation.processor)

    implementation(project(":lightevent"))

    implementation(project(":event-processor"))
    ksp(project(":event-processor"))
}

kapt {
    arguments {
        arg("eventBusIndex", "io.github.lightevent.benchmark.AppEventBusIndex")
    }
}

ksp {
    arg("eventCount", "200")
    arg("generatedPath", "${buildDir}/generated/ksp/debug/kotlin/")
}