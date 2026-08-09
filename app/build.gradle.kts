@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.Properties
import java.io.File
import java.io.FileInputStream

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.cmaker)
    alias(libs.plugins.refine)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra
val branchName: String by rootProject.extra
val kernelPatchVersion: String by rootProject.extra

// Load keystore properties
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Load local properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

val ccache = System.getenv("PATH")?.split(File.pathSeparator)
    ?.map { File(it, "ccache") }?.firstOrNull { it.exists() }?.absolutePath

val baseFlags = listOf(
    "-Wall", "-Qunused-arguments", "-fno-rtti", "-fvisibility=hidden",
    "-fvisibility-inlines-hidden", "-fno-exceptions", "-fno-stack-protector",
    "-fomit-frame-pointer", "-Wno-builtin-macro-redefined", "-Wno-unused-value",
    "-D__FILE__=__FILE_NAME__",
    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON", "-Wno-unused", "-Wno-unused-parameter",
    "-Wno-unused-command-line-argument", "-Wno-incompatible-function-pointer-types",
    "-U_FORTIFY_SOURCE", "-D_FORTIFY_SOURCE=0"
)

val baseArgs = mutableListOf(
    "-DANDROID_STL=none", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
    "-DCMAKE_CXX_STANDARD=23", "-DCMAKE_C_STANDARD=23",
    "-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON", "-DCMAKE_VISIBILITY_INLINES_HIDDEN=ON",
    "-DCMAKE_CXX_VISIBILITY_PRESET=hidden", "-DCMAKE_C_VISIBILITY_PRESET=hidden"
).apply { if (ccache != null) add("-DANDROID_CCACHE=$ccache") }

android {
    namespace = "me.bmax.apatch"
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("KEYSTORE_FILE") ?: "debug.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = keystoreProperties.getProperty("KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: "android"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_CXX_FLAGS_DEBUG=-Og", "-DCMAKE_C_FLAGS_DEBUG=-Og")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            multiDexEnabled = true
            vcsInfo.include = false
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    val relFlags = listOf(
                        "-flto", "-ffunction-sections", "-fdata-sections", "-Wl,--gc-sections",
                        "-fno-unwind-tables", "-fno-asynchronous-unwind-tables", "-Wl,--exclude-libs,ALL",
                        "-Ofast", "-fmerge-all-constants", "-flto=full", "-ffat-lto-objects",
                        "-fno-semantic-interposition", "-fno-threadsafe-statics"
                    )
                    cppFlags += relFlags
                    cFlags += relFlags
                    arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_CXX_FLAGS_RELEASE=-O3 -DNDEBUG", "-DCMAKE_C_FLAGS_RELEASE=-O3 -DNDEBUG")
                }
            }
        }
    }

    dependenciesInfo.includeInApk = false

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        prefab = true
    }

    defaultConfig {
        applicationId = "me.yuki.folk"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName
        buildConfigField("String", "buildKPV", "\"$kernelPatchVersion\"")
        buildConfigField("boolean", "DEBUG_FAKE_ROOT", localProperties.getProperty("debug.fake_root", "false"))

        base.archivesName = "FolkPatch_${managerVersionCode}_${managerVersionName}_on_${branchName}"

        ndk.abiFilters.addAll(arrayOf("arm64-v8a"))
        externalNativeBuild {
            cmake {
                cppFlags += baseFlags + "-std=c++2b"
                cFlags += baseFlags + "-std=c2x"
                arguments += baseArgs
                
                // Pass Token and Signature Hash to CMake
                val authProps = Properties()
                val authFile = rootProject.file("auth.properties")
                if (authFile.exists()) {
                    authProps.load(FileInputStream(authFile))
                }
                val token = authProps.getProperty("api.token", "")
                val signatureHash = authProps.getProperty("app.signature.hash", "")

                // Pass to C++ compiler directly via flags
                // Only add flags if values are non-empty to avoid compiler errors
                if (token.isNotEmpty()) {
                    cppFlags += "-DAPI_TOKEN=\"$token\""
                }
                if (signatureHash.isNotEmpty()) {
                    cppFlags += "-DAPP_SIGNATURE_HASH=\"$signatureHash\""
                }
                cppFlags += "-DAPP_PACKAGE_NAME=\"$applicationId\""
                
                abiFilters("arm64-v8a")
            }
        }
        
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "**"
            merges += "META-INF/com/google/android/**"
        }
    }

    externalNativeBuild {
        cmake {
            version = "3.28.0+"
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    android.sourceSets.named("main") {
        kotlin.directories += "build/generated/ksp/$name/kotlin"
        jniLibs.directories += "libs"
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

fun registerDownloadTask(
    taskName: String, srcUrl: String, destPath: String, project: Project, version: String? = null
) {
    project.tasks.register(taskName) {
        val destFile = File(destPath)
        val versionFile = File("$destPath.version")

        doLast {
            var forceDownload = false
            if (version != null) {
                if (!versionFile.exists() || versionFile.readText().trim() != version) {
                    forceDownload = true
                }
            }

            if (!destFile.exists() || forceDownload || isFileUpdated(srcUrl, destFile)) {
                println(" - Downloading $srcUrl to ${destFile.absolutePath}")
                downloadFile(srcUrl, destFile)
                if (version != null) {
                    versionFile.writeText(version)
                }
                println(" - Download completed.")
            } else {
                println(" - File is up-to-date, skipping download.")
            }
        }
    }
}

fun isFileUpdated(url: String, localFile: File): Boolean {
    val connection = URI.create(url).toURL().openConnection()
    val remoteLastModified = connection.getHeaderFieldDate("Last-Modified", 0L)
    return remoteLastModified > localFile.lastModified()
}

fun downloadFile(url: String, destFile: File) {
    URI.create(url).toURL().openStream().use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

/** Download with connect/read timeouts and retries (robust against flaky networks). */
fun downloadFileRetry(url: String, destFile: File, maxRetries: Int = 5) {
    var attempt = 0
    while (true) {
        try {
            val conn = URI.create(url).toURL().openConnection()
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.getInputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        } catch (e: Exception) {
            attempt++
            if (attempt >= maxRetries) throw e
            println(" - download attempt $attempt/$maxRetries failed for $url: ${e.message}")
            Thread.sleep(2000L * attempt)
        }
    }
}

registerDownloadTask(
    taskName = "downloadKpimg",
    srcUrl = "https://github.com/LyraVoid/KernelPatch/releases/download/$kernelPatchVersion/kpimg-android",
    destPath = "${project.projectDir}/src/main/assets/kpimg",
    project = project,
    version = kernelPatchVersion
)

registerDownloadTask(
    taskName = "downloadKptools",
    srcUrl = "https://github.com/LyraVoid/KernelPatch/releases/download/$kernelPatchVersion/kptools-android",
    destPath = "${project.projectDir}/libs/arm64-v8a/libkptools.so",
    project = project,
    version = kernelPatchVersion
)

// Compat kp version less than 0.10.7
// TODO: Remove in future
registerDownloadTask(
    taskName = "downloadCompatKpatch",
    srcUrl = "https://github.com/bmax121/KernelPatch/releases/download/0.10.7/kpatch-android",
    destPath = "${project.projectDir}/libs/arm64-v8a/libkpatch.so",
    project = project,
    version = "0.10.7"
)

// Jailbreak mode: download KernelPatch ko for every supported kernel KMI and
// package them into the APK assets so the app can load the matching one.
val jailbreakKmis = listOf(
    "android12-5.10", "android13-5.10", "android13-5.15",
    "android14-5.15", "android14-6.1", "android15-6.6", "android16-6.12",
)

tasks.register("downloadJailbreakKo") {
    doLast {
        val assetsDir = File("${project.projectDir}/src/main/assets")
        assetsDir.mkdirs()
        jailbreakKmis.forEach { kmi ->
            val srcUrl =
                "https://github.com/LyraVoid/KernelPatch/releases/download/$kernelPatchVersion/${kmi}_kernelpatch.ko"
            val destFile = File(assetsDir, "${kmi}_kernelpatch.ko")
            if (!destFile.exists()) {
                println(" - Downloading $srcUrl to ${destFile.absolutePath}")
                downloadFileRetry(srcUrl, destFile)
            } else {
                println(" - $kmi kernelpatch.ko already present.")
            }
        }
    }
}

tasks.register<Copy>("mergeScripts") {
    into("${project.projectDir}/src/main/resources/META-INF/com/google/android")
    from(rootProject.file("${project.rootDir}/scripts/update_binary.sh")) {
        rename { "update-binary" }
    }
    from(rootProject.file("${project.rootDir}/scripts/update_script.sh")) {
        rename { "updater-script" }
    }
}

// Build fpd (FolkPatch service binary) for arm64
tasks.register<Exec>("buildFpd") {
    executable("cargo")
    args("ndk", "-t", "arm64-v8a", "build", "--release")
    workingDir("${project.rootDir}/fpd")
    doFirst {
        println("Building fpd for arm64...")
    }
    doLast {
        val fpdBinary = file("${project.rootDir}/fpd/target/aarch64-linux-android/release/fpd")
        val serviceDir = file("src/main/assets/Service")
        serviceDir.mkdirs()
        fpdBinary.copyTo(file("${serviceDir}/fpd"), overwrite = true)
        println("fpd binary built and copied to Service/fpd")
    }
}

tasks.getByName("preBuild").dependsOn(
    "downloadKpimg",
    "downloadKptools",
    "downloadCompatKpatch",
    "downloadJailbreakKo",
    "mergeScripts",
    "buildFpd",
)

// https://github.com/bbqsrc/cargo-ndk
// cargo ndk -t arm64-v8a build --release
tasks.register<Exec>("cargoBuild") {
    executable("cargo")
    args("ndk", "-t", "arm64-v8a", "build", "--release")
    workingDir("${project.rootDir}/apd")
    environment("APATCH_VERSION_CODE", "${managerVersionCode}")
    environment("APATCH_VERSION_NAME", "${managerVersionCode}-Matsuzaka-yuki")
}

tasks.register<Copy>("buildApd") {
    dependsOn("cargoBuild")
    from("${project.rootDir}/apd/target/aarch64-linux-android/release/apd")
    into("${project.projectDir}/libs/arm64-v8a")
    rename("apd", "libapd.so")
}

tasks.configureEach {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("buildApd")
    }
}

tasks.register<Exec>("cargoClean") {
    executable("cargo")
    args("clean")
    workingDir("${project.rootDir}/apd")
}

tasks.register<Delete>("apdClean") {
    dependsOn("cargoClean")
    delete(file("${project.projectDir}/libs/arm64-v8a/libapd.so"))
}

tasks.clean {
    dependsOn("apdClean")
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.biometric)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime.livedata)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.nio)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.dev.rikka.shizuku.api)
    implementation(libs.dev.rikka.shizuku.provider)

    // Shizuku server 内置所需：hidden API 兼容库与 refine 运行时
    implementation(libs.dev.rikka.hidden.compat)
    compileOnly(libs.dev.rikka.hidden.stub)
    implementation(libs.dev.rikka.refine.runtime)

    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.io.coil.kt.coil.gif)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.me.zhanghai.android.appiconloader.coil)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)

    implementation(libs.markdown)

    implementation(libs.ini4j)

    implementation(libs.google.code.gson)

    implementation(libs.liquid)

    implementation(libs.materialKolor)

    compileOnly(libs.cxx)
}
