@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.cmaker)
    id("kotlin-parcelize")
}

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

android {
    namespace = "me.bmax.apatch"
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("KEYSTORE_FILE") ?: "debug.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = keystoreProperties.getProperty("KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: "android"
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
        }
    }

    dependenciesInfo.includeInApk = false

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        prefab = true
    }

    defaultConfig {
        applicationId = "mi.yuki.folk"
        buildConfigField("String", "buildKPV", "\"$kernelPatchVersion\"")
        buildConfigField("boolean", "DEBUG_FAKE_ROOT", localProperties.getProperty("debug.fake_root", "false"))

        // Load auth properties
        val authProps = Properties()
        val authFile = rootProject.file("auth.properties")
        if (authFile.exists()) {
            authProps.load(FileInputStream(authFile))
        }
        val token = authProps.getProperty("api.token", "")
        val signatureHash = authProps.getProperty("app.signature.hash", "")

        buildConfigField("String", "APP_SIGNATURE_HASH", "\"$signatureHash\"")

        externalNativeBuild {
            cmake {
                val nativeFlags = mutableListOf<String>()
                if (token.isNotEmpty()) {
                    nativeFlags += "-DAPI_TOKEN=\"$token\""
                }
                if (signatureHash.isNotEmpty()) {
                    nativeFlags += "-DAPP_SIGNATURE_HASH=\"$signatureHash\""
                }
                nativeFlags += "-DAPP_PACKAGE_NAME=\"$applicationId\""
                cppFlags += nativeFlags
            }
        }

        base.archivesName = "FolkLite_${managerVersionCode}_${managerVersionName}_${branchName}"
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

    sourceSets["main"].jniLibs.srcDir("libs")

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
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

registerDownloadTask(
    taskName = "downloadKpimg",
    srcUrl = "https://github.com/bmax121/KernelPatch/releases/download/$kernelPatchVersion/kpimg-android",
    destPath = "${project.projectDir}/src/main/assets/kpimg",
    project = project,
    version = kernelPatchVersion
)

registerDownloadTask(
    taskName = "downloadKptools",
    srcUrl = "https://github.com/bmax121/KernelPatch/releases/download/$kernelPatchVersion/kptools-android",
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

tasks.register<Copy>("mergeScripts") {
    into("${project.projectDir}/src/main/resources/META-INF/com/google/android")
    from(rootProject.file("${project.rootDir}/scripts/update_binary.sh")) {
        rename { "update-binary" }
    }
    from(rootProject.file("${project.rootDir}/scripts/update_script.sh")) {
        rename { "updater-script" }
    }
}

tasks.getByName("preBuild").dependsOn(
    "downloadKpimg",
    "downloadKptools",
    "downloadCompatKpatch",
    "mergeScripts",
    "buildHideArm64",
    "buildUmountArm64",
)

// Build Hide.zig for arm64
tasks.register<Exec>("buildHideArm64") {
    val zigExe = System.getenv("ZIG_EXE")?.let { File(it) }
        ?: localProperties.getProperty("zig.dir")?.let { zigDir ->
            if (System.getProperty("os.name").lowercase().contains("win")) {
                File(zigDir, "zig.exe")
            } else {
                File(zigDir, "zig")
            }
        }
        ?: if (System.getProperty("os.name").lowercase().contains("win")) {
            File("E:\\bin\\zig", "zig.exe")
        } else {
            File("zig")
        }

    val sourceFile = rootProject.file("FolkS/Hide.zig")
    val outputFile = file("src/main/assets/Service/Hide")
    val umountFile = file("src/main/assets/Service/Umount")

    onlyIf {
        !(outputFile.exists() && umountFile.exists()) && (!outputFile.exists() || sourceFile.lastModified() > outputFile.lastModified())
    }

    doFirst {
        outputFile.parentFile.mkdirs()
        println("Building Hide.zig for arm64 with Zig...")
        println("Zig: ${zigExe.absolutePath}")
    }

    commandLine(
        zigExe.absolutePath,
        "build-exe",
        "-target", "aarch64-linux-android",
        "-O", "ReleaseSmall",
        "-static",
        sourceFile.absolutePath,
        "-femit-bin=${outputFile.absolutePath}"
    )

    doLast {
        println("Hide binary built: ${outputFile.absolutePath}")
    }
}

// Build Umount.zig for arm64
tasks.register<Exec>("buildUmountArm64") {
    val zigExe = System.getenv("ZIG_EXE")?.let { File(it) }
        ?: localProperties.getProperty("zig.dir")?.let { zigDir ->
            if (System.getProperty("os.name").lowercase().contains("win")) {
                File(zigDir, "zig.exe")
            } else {
                File(zigDir, "zig")
            }
        }
        ?: if (System.getProperty("os.name").lowercase().contains("win")) {
            File("E:\\bin\\zig", "zig.exe")
        } else {
            File("zig")
        }

    val sourceFile = rootProject.file("FolkS/Umount.zig")
    val outputFile = file("src/main/assets/Service/Umount")
    val hideFile = file("src/main/assets/Service/Hide")

    onlyIf {
        !(outputFile.exists() && hideFile.exists()) && (!outputFile.exists() || sourceFile.lastModified() > outputFile.lastModified())
    }

    doFirst {
        outputFile.parentFile.mkdirs()
        println("Building Umount.zig for arm64 with Zig...")
    }

    commandLine(
        zigExe.absolutePath,
        "build-exe",
        "-target", "aarch64-linux-android",
        "-O", "ReleaseSmall",
        "-static",
        sourceFile.absolutePath,
        "-femit-bin=${outputFile.absolutePath}"
    )

    doLast {
        println("Umount binary built: ${outputFile.absolutePath}")
    }
}

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

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.miuix)
    implementation(libs.miuix.icons)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.nio)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.io.coil.kt.coil.compose)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.me.zhanghai.android.appiconloader.coil)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)

    implementation(libs.markdown)

    implementation(libs.ini4j)

    implementation(libs.capsule)

    // Haze (blur effects)
    implementation(libs.haze)

    // Backdrop (liquid glass)
    implementation(libs.backdrop)

    compileOnly(libs.cxx)
}

cmaker {
    default {
        arguments += "-DANDROID_STL=none"
        arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
        abiFilters("arm64-v8a")
        cppFlags += "-std=c++2b"
        cFlags += "-std=c2x"
    }
}
