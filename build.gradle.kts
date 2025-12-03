plugins {
    kotlin("jvm") version "2.1.10"
    application
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.dokka") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
    id("org.jetbrains.compose") version "1.9.3"
}

group = "com.heyteam.ufg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.jar {
    manifest {
        attributes(mapOf("Main-Class" to "com.heyteam.ufg.MainKt"))
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt.yml")
}

dependencies {
    val kotestVersion = "5.8.0"
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    implementation(compose.desktop.currentOs)
}

application {
    mainClass.set("com.heyteam.ufg.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    val jdk = 17
    jvmToolchain(jdk)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
