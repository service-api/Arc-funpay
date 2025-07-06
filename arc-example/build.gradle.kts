plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "arc.funpay"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("io.insert-koin:koin-core:4.1.0")
}

kotlin {
    jvmToolchain(21)
}