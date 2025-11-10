// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
}

tasks.register<Exec>("installGitHook") {
    group = "verification"
    description = "Installs the git hook."
    commandLine("git", "config", "core.hooksPath", "hooks")
    doLast {
        println("Git hook installed successfully.")
    }
}
