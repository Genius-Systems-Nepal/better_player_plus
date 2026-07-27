plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties
import java.nio.charset.Charset

group = "uz.shs.better_player_plus"
version = "1.0-SNAPSHOT"

val lifecycleVersion = "2.9.4"
val annotationVersion = "1.9.1"
// Quanteec Media3 adapter (quanteecMedia3Exoplayer 1.8.33) supports Media3 1.0.x–1.6.x.
val media3Version = "1.6.1"
val workVersion = "2.10.5"
val quanteecVersion = "1.8.33"

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader(Charset.forName("UTF-8")).use { localProperties.load(it) }
}
val quanteecUsername = localProperties.getProperty("quanteecUsername") ?: ""
val quanteecPassword = localProperties.getProperty("quanteecPassword") ?: ""

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.quanteec.com/repository/internal/")
            credentials {
                username = quanteecUsername
                password = quanteecPassword
            }
            authentication {
                create<org.gradle.authentication.http.BasicAuthentication>("basic")
            }
        }
    }
}

android {
    namespace = "uz.shs.better_player_plus"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
}

dependencies {
    implementation("androidx.media:media:1.7.1")

    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-datasource-cronet:$media3Version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3Version")
    // IMA / VAST mid-roll (parity with GSN better_player ExoPlayer extension-ima)
    implementation("androidx.media3:media3-exoplayer-ima:$media3Version")

    // Quanteec P2P (Media3 adapter — parity with GSN better_player quanteecExoPlugin2_17)
    implementation("com.quanteec:quanteecMedia3Exoplayer:$quanteecVersion")
    implementation("com.quanteec:quanteecPlugin:$quanteecVersion")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    implementation("androidx.annotation:annotation:$annotationVersion")
    implementation("androidx.work:work-runtime:$workVersion")

    //noinspection GradleDependency
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.20")
}
