import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Version lives in version.properties so the build can bump it (see bumpVersionCode below).
// Read through providers so the configuration cache notices when the file changes.
val versionProps = Properties().apply {
    load(providers.fileContents(layout.projectDirectory.file("version.properties")).asText.get().reader())
}
val appVersionCode = versionProps.getProperty("VERSION_CODE", "1").trim().toInt()
val appVersionName = versionProps.getProperty("VERSION_NAME", "1.0.0").trim()

android {
    namespace = "com.campus.campusmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.campus.campusmate"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Fill these in your local.properties or environment variables for security
            // storeFile = file("path/to/your/keystore.jks")
            // storePassword = "your_store_password"
            // keyAlias = "your_key_alias"
            // keyPassword = "your_key_password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.work.runtime)
    implementation(libs.security.crypto)
    implementation(libs.timber)
    annotationProcessor(libs.glide.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

/**
 * Advances the version in version.properties. Runs after a successful assemble/bundle, so the
 * build that just finished keeps its version and the next build gets the following one.
 *
 * VERSION_NAME (major.minor.patch) rolls over: patch counts 0-9, minor 0-99, so
 * 1.18.9 -> 1.19.0 and 1.99.9 -> 2.0.0. VERSION_CODE (Android's upgrade number) simply
 * increases by one every time and must never go down.
 */
abstract class BumpVersionCode : DefaultTask() {
    @get:Internal
    abstract val propsFile: RegularFileProperty

    @TaskAction
    fun bump() {
        val file = propsFile.get().asFile
        // Rewrite line by line (not Properties.store) to keep the comments and avoid a timestamp.
        val lines = file.readLines().map { line ->
            val value = line.substringAfter("=").trim()
            when {
                line.trim().startsWith("VERSION_CODE=") -> "VERSION_CODE=" + (value.toInt() + 1)
                line.trim().startsWith("VERSION_NAME=") -> "VERSION_NAME=" + nextVersionName(value)
                else -> line
            }
        }
        file.writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        logger.lifecycle("Next build: " + lines.filter { it.startsWith("VERSION_") }.joinToString(", "))
    }

    private fun nextVersionName(current: String): String {
        val parts = current.split(".").map { it.toInt() }
        require(parts.size == 3) { "VERSION_NAME must be major.minor.patch, was '$current'" }
        var (major, minor, patch) = parts
        patch++
        if (patch > 9) { patch = 0; minor++ }
        if (minor > 99) { minor = 0; major++ }
        return "$major.$minor.$patch"
    }
}

val bumpVersionCode = tasks.register<BumpVersionCode>("bumpVersionCode") {
    propsFile.set(layout.projectDirectory.file("version.properties"))
}

// Android Studio's Run uses assembleDebug; release builds use assembleRelease/bundleRelease.
// Finalizers don't run when the build fails before the assemble task executes.
tasks.matching { Regex("(assemble|bundle)(Debug|Release)?").matches(it.name) }.configureEach {
    finalizedBy(bumpVersionCode)
}
