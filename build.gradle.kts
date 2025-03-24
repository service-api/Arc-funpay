plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("maven-publish")
}

group = "arc.funpay"
version = "1.0-SNAPSHOT"

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

    // Test
    implementation(kotlin("test"))
    implementation(rootProject.libs.mockito)
    implementation(rootProject.libs.mockito.kotlin)
}

tasks.test {
    useJUnitPlatform()
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
}

publishing {
    repositories {
        maven {
            name = "yamidaRepo"
            url = uri("http://89.39.121.106:8080/releases")
            isAllowInsecureProtocol = true
            credentials {
                username = System.getenv("REPO_NAME") ?: error("REPO_NAME environment variable is not set")
                password = System.getenv("REPO_TOKEN") ?: error("REPO_TOKEN environment variable is not set")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "arc"
            artifactId = "funpay"
            version = "1.3.6"
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
        }
    }
}


kotlin {
    jvmToolchain(21)
}