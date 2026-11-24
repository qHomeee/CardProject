plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

val ktlint = configurations.dependencyScope("ktlint")
val ktlintClasspath = configurations.resolvable("ktlintClasspath") {
    extendsFrom(ktlint.get())
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
}

dependencies {
    add(ktlint.name, libs.ktlint.cli)
}

val ktlintCheck = tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin source and Gradle scripts with ktlint."
    classpath(ktlintClasspath)
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/*.kt",
        "**/*.kts",
        "!**/build/**",
        "!**/.gradle/**",
        "!**/.idea/**",
        "!**/.kotlin/**",
    )
}

tasks.check {
    dependsOn(ktlintCheck)
}
