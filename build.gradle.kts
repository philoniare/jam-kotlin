import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test-junit5"))
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "1.21"
    }

    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$rootDir/config/detekt/detekt.yml")
    }
}

tasks.register<Detekt>("detektAll") {
    description = "Runs detekt on the whole project at once."
    parallel = true
    config.setFrom(files("$rootDir/config/detekt.yml"))
    setSource(files(projectDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
    reports {
        xml.required.set(false)
        html.required.set(true)
        txt.required.set(false)
    }
}
