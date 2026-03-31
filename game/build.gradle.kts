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
    val javaWebSocketVersion = "1.5.6"
    val jsonJavaVersion = "20251224"
    val slf4jVersion = "2.0.13"
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    implementation("org.java-websocket:Java-WebSocket:$javaWebSocketVersion")
    implementation("org.json:json:$jsonJavaVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation(compose.desktop.currentOs)
}

application {
    val osName = System.getProperty("os.name").lowercase()
    val libPath =
        when {
            "windows" in osName -> "${project.rootDir}/../channel/build/Release"
            else -> "${project.rootDir}/../channel/build"
        }
    applicationDefaultJvmArgs =
        listOf(
            "-Djava.library.path=$libPath",
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
