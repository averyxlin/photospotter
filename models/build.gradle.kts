val releaseVersion: String by project

kotlin {
    jvmToolchain(17)
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "org.example"
version = releaseVersion

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}