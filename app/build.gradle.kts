import java.util.Properties

val properties = Properties().apply { load(rootProject.file("local.properties").inputStream()) }
val wheelhouse = layout.buildDirectory.dir("wheelhouse")
val pydanticDir = layout.buildDirectory.dir("pydantic")
val pydanticCoreDir = pydanticDir.map { it.dir("pydantic-core") }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "org.waterloorocketry.parsdroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.waterloorocketry.parsdroid"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.usbSerialForAndroid)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

chaquopy {
    defaultConfig {
        buildPython("python3.14")
        version = "3.14"
        pip {
            options("--find-links", wheelhouse.get().asFile.path)
            install("git+https://github.com/waterloo-rocketry/parsley.git@2026.5")
        }
    }
}

tasks.register<Exec>("clonePydantic") {
    doFirst { pydanticDir.get().asFile.deleteRecursively() }
    commandLine("git", "clone", "--depth", "1", "--branch", "core-v2.46.4",
        "https://github.com/pydantic/pydantic.git", pydanticDir.get().asFile.path)
    outputs.dir(pydanticDir)
}

tasks.register<Exec>("buildPydanticCoreWheels") {
    dependsOn("clonePydantic")

    environment("ANDROID_HOME", properties.getProperty("sdk.dir"))
    environment("CIBW_BUILD", "cp${chaquopy.defaultConfig.version!!.replace(".", "")}-android_*")
    workingDir = pydanticCoreDir.get().asFile

    commandLine("uvx", "cibuildwheel", "--platform", "android", "--archs", "x86_64,arm64_v8a", "--output-dir", wheelhouse.get().asFile.path)

    inputs.files(
        fileTree(pydanticCoreDir.get().asFile) {
            include(
                "src/**",
                "python/**",
                "pyproject.toml",
                "Cargo.toml",
                "Cargo.lock",
                "*.cfg"
            )
        }
    )
    outputs.dir(wheelhouse)
}

tasks.configureEach {
    if (name.matches(Regex(".*installDebugPythonRequirements"))) {
        dependsOn("buildPydanticCoreWheels")
    }
}
