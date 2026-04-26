plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

project.ext.set("kernelPatchVersion", "0.13.1")

val androidMinSdkVersion by extra(26)
val androidTargetSdkVersion by extra(36)
val androidCompileSdkVersion by extra(36)
val androidBuildToolsVersion by extra("36.1.0")
val androidCompileNdkVersion by extra("29.0.14206865")
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

fun getGitCommitCount(): Int {
    return exec("git rev-list --count HEAD", "0").toInt()
}

fun getGitDescribe(): String {
    return exec("git rev-parse --verify --short HEAD", "unknown")
}

fun getVersionCode(): Int {
    return 114232
}

fun getbranch(): String {
    return exec("git rev-parse --abbrev-ref HEAD", "unknown")
}

fun getVersionName(): String {
    return "4.2"
}

tasks.register("printVersion") {
    doLast {
        println("Version code: $managerVersionCode")
        println("Version name: $managerVersionName")
    }
}
