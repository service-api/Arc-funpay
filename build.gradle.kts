
plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("maven-publish")
}
base {
    archivesName.set("funpay")
}

group = "arc.funpay"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(rootProject.libs.ktor.core)
    implementation(rootProject.libs.ktor.okhttp)
    implementation(rootProject.libs.ktor.content)
    implementation(rootProject.libs.ktor.serialization)
    implementation(rootProject.libs.kotlinx.serialization)
    implementation(rootProject.libs.jsoup)
    implementation(rootProject.libs.slf4j)
    implementation(rootProject.libs.koin)
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