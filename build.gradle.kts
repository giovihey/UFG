plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.heyteam.UFG"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("com.heyteam.UFG.MainKt")
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