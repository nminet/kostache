plugins {
    val kotlinVersion = "1.8.0"
    kotlin("multiplatform") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("maven-publish")
}

group = "dev.noemi.kostache"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    macosArm64("osx")
    iosArm64("ios")
    iosSimulatorArm64()


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("scripting-jsr223"))
                implementation("io.kotest:kotest-assertions-core:5.5.4")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.snakeyaml:snakeyaml-engine:2.6")
            }
        }
        val osxMain by getting
        val osxTest by getting
        val iosMain by getting
        val iosTest by getting
        val iosSimulatorArm64Main by getting
        val iosSimulatorArm64Test by getting

        iosSimulatorArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Test.dependsOn(iosTest)
    }

    tasks.matching { it.group == "publishing" && it.name.contains("IosSimulator") }
        .configureEach { enabled = false }
}
