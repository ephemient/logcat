import com.android.utils.appendCapitalized
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("com.android.library") version "8.8.0"
    kotlin("multiplatform") version "2.0.21"
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    namespace = "com.github.ephemient.logcat"
    sourceSets {
        getByName("debug").jniLibs.srcDir(File(buildDir, "intermediates/jniLibs/debug"))
        getByName("release").jniLibs.srcDir(File(buildDir, "intermediates/jniLibs/release"))
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        managedDevices.localDevices.create("pixel2Api30") {
            device = "Pixel 2"
            apiLevel = 30
            systemImageSource = "aosp"
        }
    }
}

val copyJniLibs = NativeBuildType.DEFAULT_BUILD_TYPES.associateWith {
    tasks.register("copy".appendCapitalized(it.getName(), "jniLibs"), Sync::class) {
        into(File(buildDir, "intermediates/jniLibs/${it.getName()}"))
    }
}

androidComponents.onVariants { variant ->
    variant.lifecycleTasks.registerPreBuild(if (variant.debuggable) "copyDebugJniLibs" else "copyReleaseJniLibs")
}

kotlin {
    androidTarget {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
        publishLibraryVariants("release")
    }
    mapOf(
        "armeabi-v7a" to androidNativeArm32(),
        "arm64-v8a" to androidNativeArm64(),
        "x86" to androidNativeX86(),
        "x86_64" to androidNativeX64(),
    ).forEach { (abi, target) ->
        target.binaries.sharedLib("nativelogcat") {
            if (buildType == NativeBuildType.DEBUG && abi != "arm64-v8a") return@sharedLib
            copyJniLibs[buildType]?.configure {
                from(linkTaskProvider) {
                    into("libs/$abi")
                }
            }
        }
        target.compilations.getByName("main").cinterops.create("logging") {
            definitionFile = file("src/androidNativeMain/cinterop/logging.def")
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        androidInstrumentedTest {
            dependencies {
                runtimeOnly("androidx.test:runner:1.6.2")
                implementation("androidx.test.ext:junit-ktx:1.2.1")
            }
        }
    }
}

dependencies {
    androidTestUtil("androidx.test:orchestrator:1.5.1")
}
