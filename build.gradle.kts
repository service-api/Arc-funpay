plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("maven-publish")
}

group = "arc.funpay"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Production
    implementation(rootProject.libs.ktor.core)
    implementation(rootProject.libs.ktor.okhttp)
    implementation(rootProject.libs.ktor.content)
    implementation(rootProject.libs.ktor.serialization)
    implementation(rootProject.libs.kotlinx.serialization)
    implementation(rootProject.libs.jsoup)
    implementation(rootProject.libs.slf4j)
    implementation(rootProject.libs.koin)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}