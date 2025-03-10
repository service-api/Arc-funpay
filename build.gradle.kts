plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "arc.funpay"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Production
    implementation(rootProject.libs.ktor.core)
    implementation(rootProject.libs.ktor.cio)
    implementation(rootProject.libs.ktor.content)
    implementation(rootProject.libs.ktor.serialization)
    implementation(rootProject.libs.kotlinx.serialization)
    implementation(rootProject.libs.jsoup)
    implementation(rootProject.libs.logback)
    implementation(rootProject.libs.koin)

    // Test
    implementation(kotlin("test"))
    implementation(rootProject.libs.mockito)
    implementation(rootProject.libs.mockito.kotlin)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}