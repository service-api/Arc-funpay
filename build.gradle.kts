plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("maven-publish")
}

base {
    archivesName.set("funpay")
}

group = "arc.funpay"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.content)
    implementation(libs.ktor.serialization)
    implementation(libs.kotlinx.serialization)
    implementation(libs.jsoup)
    implementation(libs.slf4j)
    implementation(libs.koin)
}

kotlin {
    jvmToolchain(21)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "arc"
            artifactId = project.base.archivesName.get()
            version = project.version.toString()

            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }

    repositories {
        mavenLocal()
    }
}