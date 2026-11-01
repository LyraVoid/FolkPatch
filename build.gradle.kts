plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

project.ext.set("kernelPatchVersion", "0.13.9")

val androidMinSdkVersion by extra(26)
val androidTargetSdkVersion by extra(36)
val androidCompileSdkVersion by extra(37)
val androidBuildToolsVersion by extra("36.1.0")
val androidCompileNdkVersion by extra("30.0.15729638")
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())
val branchName by extra(getbranch())
fun Project.exec(command: String, default: String): String {
    return try {
        providers.exec {
            commandLine(command.split(" "))
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() } ?: default
    } catch (e: Exception) {
        default
    }
}

fun getVersionProperties(): java.util.Properties {
    val properties = java.util.Properties()
    File(rootDir, "version.properties").inputStream().use(properties::load)
    return properties
}

fun getVersionProperty(name: String): String {
    return getVersionProperties().getProperty(name)
        ?: error("$name not found in version.properties")
}

fun getGitCommitCount(): Int {
    val count = exec("git rev-list --count HEAD", "")
    return count.toIntOrNull()?.takeIf { it > 0 }
        ?: error("Failed to determine git commit count; GitHub checkout must use fetch-depth: 0")
}

fun getVersionCode(): Int {
    val code = getVersionProperty("managerVersionEpoch").toInt() + getGitCommitCount()
    val floor = getVersionProperty("managerVersionFloor").toInt()
    require(code > floor) {
        "Computed versionCode $code is not greater than managerVersionFloor=$floor"
    }
    return code
}

/**
 * Branch names may contain '/', which is not a legal character in a file name.
 * The branch only feeds the APK archive name, so keep it filesystem-safe here
 * instead of failing later in the manifest task.
 */
fun getbranch(): String {
    return exec("git rev-parse --abbrev-ref HEAD", "unknown")
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
}

fun getVersionName(): String {
    val name = exec("git rev-parse --verify --short HEAD", "")
    return name.takeIf { it.isNotEmpty() }
        ?: error("Failed to determine git commit for versionName")
}

tasks.register("printVersion") {
    doLast {
        println("Version code: $managerVersionCode")
        println("Version name: $managerVersionName")
        println("Commit count: ${getGitCommitCount()}")
        println("Version floor: ${getVersionProperty("managerVersionFloor")}")
    }
}
