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

dependencies {
    val kotestVersion = "5.8.0"
    val mockkVersion = "1.13.13"
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    implementation(compose.desktop.currentOs)
}

application {
    applicationDefaultJvmArgs =
        listOf(
            "-Djava.library.path=${project.rootDir}/channel/build",
        )
    mainClass.set("com.heyteam.ufg.MainKt")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Djava.library.path=${project.rootDir}/channel/build")
}

kotlin {
    val jdk = 17
    jvmToolchain(jdk)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
