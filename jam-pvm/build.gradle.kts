plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    implementation(project(":jam-core"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}
