import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("com.codingfeline.buildkonfig")
}

val lifecycleVersion by extra("2.4.0-rc01")
val coroutinesVersion by extra("1.6.0")
val ktorVersion by extra("2.0.0-beta-1")
val settingsVersion by extra("0.8.1")
val kermitVersion by extra("1.0.3")

kotlin {
    android()

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget = when {
        System.getenv("SDK_NAME")?.startsWith("iphoneos") == true -> ::iosArm64
        System.getenv("NATIVE_ARCH")?.startsWith("arm") == true -> ::iosSimulatorArm64
        else -> ::iosX64
    }

    iosTarget("ios") {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion") {
                    isForce = true
                }

                implementation("com.benasher44:uuid:0.3.1")

                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                implementation("com.russhwolf:multiplatform-settings-coroutines:$settingsVersion")

                // Using api to export Kermit to iOS
                api("co.touchlab:kermit:$kermitVersion")
                api("co.touchlab:kermit-crashlytics:$kermitVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                // TODO: might want to use JUnit with AssertJ instead (https://stackoverflow.com/a/63427057)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

                implementation("com.russhwolf:multiplatform-settings-test:$settingsVersion")

                implementation("co.touchlab:kermit-test:$kermitVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

                implementation("io.ktor:ktor-client-android:$ktorVersion")

                implementation("androidx.datastore:datastore-preferences:1.0.0")
                implementation("androidx.startup:startup-runtime:1.1.0")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:$ktorVersion")
            }
        }
        val iosTest by getting
    }

    // To make kermit available in iOS project - increases binary size because of the extra headers.
    // More info: https://github.com/touchlab/Kermit/blob/main/samples/sample-swift-export/README.md
    targets.withType<KotlinNativeTarget> {
        binaries.withType<Framework> {
            export("co.touchlab:kermit:$kermitVersion")
        }
    }
}

android {
    compileSdkVersion(31)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(30)
    }
}

buildkonfig {
    packageName = "eu.rajniak.cat"

    defaultConfigs {
        buildConfigField(
            type = STRING,
            name = "api_key",
            value = findProperty("catsApiKey") as? String ?: throw Exception("catsApiKey is not set")
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}
