plugins {
    kotlin("jvm") version "1.9.22"
    application
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "com.heyteam.ufg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val kotestVersion = "5.8.0"
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

application {
    mainClass.set("com.heyteam.UFG.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

kotlin {
    val jdk = 17
    jvmToolchain(jdk)
}
