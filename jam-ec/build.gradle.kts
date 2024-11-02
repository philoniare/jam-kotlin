plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation(project(":jam-core"))
    testImplementation(project(":jam-core", configuration = "testArtifacts"))
}

sourceSets {
    test {
        java.srcDirs("src/test/kotlin")
    }
}
