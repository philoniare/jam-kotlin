plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

val os = org.gradle.internal.os.OperatingSystem.current()!!
val libPrefix = if (os.isWindows) "" else "lib"
val libSuffix = when {
    os.isMacOsX -> "dylib"
    os.isLinux -> "so"
    os.isWindows -> "dll"
    else -> throw GradleException("Unsupported operating system")
}

val nativeLibName = "${libPrefix}bandersnatch_vrfs_wrapper.$libSuffix"
val rustProjectDir = File(project.projectDir.parent)
    .resolve("jam-vrfs")
    .resolve("bandersnatch-vrfs-wrapper")
val nativeLibs = File(project.projectDir.parent)
    .resolve("build")
    .resolve("native-libs")
val osName = when {
    os.isMacOsX -> "mac"
    os.isLinux -> "linux"
    os.isWindows -> "windows"
    else -> throw GradleException("Unsupported operating system")
}

tasks.register<Copy>("copyNativeLib") {
    dependsOn("buildRust")

    from(rustProjectDir.resolve("target/release/$nativeLibName"))
    into(nativeLibs.resolve(osName))
    doFirst {
        nativeLibs.resolve(osName).mkdirs()
    }
    doLast {
        val libFile = nativeLibs.resolve(osName).resolve(nativeLibName)
        if (libFile.exists()) {
            libFile.setExecutable(true, false)
        } else {
            throw GradleException("Failed to copy native library to: ${libFile.absolutePath}")
        }
    }
}

fun Process.text(): String {
    return inputStream.bufferedReader().readText()
}

tasks.register<Exec>("buildRust") {
    workingDir(rustProjectDir)
    println("Working directory: $rustProjectDir")

    // Get the cargo executable path
    val cargoPath = when {
        os.isWindows -> "where cargo".execute().text().trim()
        else -> "which cargo".execute().text().trim()
    }
    println("Cargo path: $cargoPath")

    // Use the full path to cargo
    if (cargoPath.isNotEmpty()) {
        commandLine(cargoPath, "build", "--release")
    } else {
        throw GradleException("Could not find cargo executable")
    }
}

// Extension function to execute shell commands
fun String.execute(): Process {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(10, TimeUnit.SECONDS)
    return proc
}



dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    dependsOn("copyNativeLib")
    useJUnitPlatform()

    doFirst {
        // Set library path to include our native libs directory
        val libraryPath = System.getProperty("java.library.path")
        val updatedPath = "${nativeLibs.absolutePath}${File.pathSeparator}$libraryPath"
        systemProperty("java.library.path", updatedPath)
    }
}

tasks.clean {
    doLast {
        // Clean Rust build artifacts
        exec {
            workingDir(rustProjectDir)
            commandLine("cargo", "clean")
        }
    }
}

tasks.jar {
    dependsOn("copyNativeLib")
    from(nativeLibs) {
        into("native-libs")
    }
}
