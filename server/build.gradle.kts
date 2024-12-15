import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val slf4jVersion: String by project
val releaseVersion: String by project

kotlin {
    jvmToolchain(17)
}

plugins {
    application
    kotlin("jvm")
    id("io.ktor.plugin")
}

ktor {
    docker {
        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.googleContainerRegistry(
                projectName = provider { "cs346-photospotter" },
                appName = provider { "northamerica-northeast2-docker.pkg.dev" },
                username = provider { "" },
                password = provider { "" }
            )
        )
    }
}

group = "com.example"
version = releaseVersion

application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("org.jetbrains.exposed", "exposed-core", "0.44.0")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.44.0")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.44.0")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.44.0")
    implementation("org.jetbrains.exposed", "exposed-kotlin-datetime", "0.44.0")
    implementation ("com.google.firebase:firebase-admin:9.2.0")
    implementation(project(":models"))
    implementation("com.google.firebase:firebase-common-ktx:20.4.2")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.1.1")
    testImplementation(kotlin("test"))
}

val compileKotlin: KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileJava.destinationDirectory.set(compileKotlin.destinationDirectory)

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}



