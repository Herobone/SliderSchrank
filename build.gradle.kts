import java.util.Locale

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
}

tasks.register("installGitHook") {
    group = "verification"
    description = "Installs the git hook."
    doLast {
        val osName = System.getProperty("os.name")
        val isWindows = osName.lowercase(Locale.getDefault()).contains("windows")
        val hookFile = if (isWindows) file("hooks/pre-commit.bat") else file("hooks/pre-commit")
        val gitHookDir = file(".git/hooks")
        if (!gitHookDir.exists()) {
            gitHookDir.mkdirs()
        }
        val gitHook = file("${gitHookDir.path}/pre-commit")
        hookFile.copyTo(gitHook, overwrite = true)
        if (!isWindows) {
            gitHook.setExecutable(true, false)
        }
        println("Git hook installed successfully.")
    }
}
