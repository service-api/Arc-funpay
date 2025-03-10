plugins {
    kotlin("jvm")
}

group = "arc.funpay"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "reposiliteRepositoryReleases"
        url = uri("http://89.39.121.106:8080/releases")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.72.0")
    implementation("arc:funpay:1.0.2")

    implementation("io.insert-koin:koin-core:4.1.0-Beta5")
}

kotlin {
    jvmToolchain(21)
}