import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun stringProperty(name: String): String = providers.gradleProperty(name).get()

/** A non-blank -PproxyUrl wins; otherwise the per-build-type default from gradle.properties. */
fun proxyUrl(buildType: String): String =
    providers.gradleProperty("proxyUrl").orNull?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty("proxyUrl.$buildType").get()

val appId = "io.github.darieno.wdiw"

android {
    namespace = appId
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = appId
        minSdk = 26
        targetSdk = 36
        versionName = "0.1.0"
        // 1.2.3 becomes 10203, so every release has a higher code than the one before it.
        versionCode = versionName!!.split(".").map(String::toInt).let { (major, minor, patch) ->
            major * 10_000 + minor * 100 + patch
        }
        buildConfigField("String", "UPDATE_REPO", "\"${stringProperty("updateRepo")}\"")
    }

    // Release signing is configured only when keystore.properties exists (see docs/releasing.md).
    val keystoreFile = rootProject.file("keystore.properties")
    val keystore = if (keystoreFile.exists()) {
        Properties().apply { keystoreFile.inputStream().use(::load) }
    } else {
        null
    }
    if (keystore != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Must match the provider authority in the manifest, which follows the application ID.
            resValue("string", "search_authority", "$appId.debug.search")
            buildConfigField("String", "PROXY_URL", "\"${proxyUrl("debug")}\"")
        }
        release {
            resValue("string", "search_authority", "$appId.search")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "PROXY_URL", "\"${proxyUrl("release")}\"")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        localeFilters += listOf("en", "es")
    }
}

/** Ships registry/providers.json inside the APK as the offline fallback. */
abstract class CopyRegistry : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        source.get().asFile.copyTo(outputDir.get().asFile.resolve("providers.json"), overwrite = true)
    }
}

val copyRegistry = tasks.register<CopyRegistry>("copyRegistry") {
    source.set(rootProject.layout.projectDirectory.file("../registry/providers.json"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyRegistry, CopyRegistry::outputDir)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.palette)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.zxing)

    testImplementation(libs.junit)
}
