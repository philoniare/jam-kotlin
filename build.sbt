import scala.sys.process._

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "io.forge.jam"
ThisBuild / version := "0.1.0-SNAPSHOT"

// Detect OS for native library paths
val osName = System.getProperty("os.name").toLowerCase
val osDirName = if (osName.contains("mac")) "mac"
                else if (osName.contains("linux")) "linux"
                else if (osName.contains("win")) "windows"
                else "mac"

val libSuffix = if (osName.contains("mac")) "dylib"
                else if (osName.contains("linux")) "so"
                else if (osName.contains("win")) "dll"
                else "dylib"

// Task key for building native library (defined before use)
lazy val buildNativeLib = taskKey[Unit]("Build native Bandersnatch VRF library")

lazy val root = (project in file("."))
    .aggregate(core, crypto, pvm, protocol)
    .settings(
        name := "jam",
        publish / skip := true
    )

lazy val core = (project in file("modules/core"))
    .settings(
        name := "jam-core",
        libraryDependencies ++= Seq(
            "org.typelevel" %% "spire" % "0.18.0",
            "org.bouncycastle" % "bcprov-jdk18on" % "1.77",
            "io.circe" %% "circe-core" % "0.14.6",
            "io.circe" %% "circe-parser" % "0.14.6",
            "org.scalatest" %% "scalatest" % "3.2.17" % Test
        ),
        scalacOptions ++= Seq(
            "-deprecation",
            "-feature",
            "-unchecked",
            "-language:implicitConversions"
        )
    )

lazy val crypto = (project in file("modules/crypto"))
    .dependsOn(core)
    .settings(
        name := "jam-crypto",
        libraryDependencies ++= Seq(
            "org.typelevel" %% "spire" % "0.18.0",
            "org.scalatest" %% "scalatest" % "3.2.17" % Test
        ),
        scalacOptions ++= Seq(
            "-deprecation",
            "-feature",
            "-unchecked",
            "-language:implicitConversions"
        ),
        // Native library build task implementation
        buildNativeLib := {
            val baseDir = (ThisBuild / baseDirectory).value
            val rustProjectDir = baseDir / "modules" / "crypto" / "native" / "bandersnatch-vrfs-wrapper"
            val targetDir = baseDir / "modules" / "crypto" / "native" / "build" / osDirName
            val libName = s"libbandersnatch_vrfs_wrapper.$libSuffix"

            val targetLib = targetDir / libName
            val sourceLib = rustProjectDir / "target" / "release" / libName

            if (!targetLib.exists()) {
                println(s"Building native library for $osDirName...")
                targetDir.mkdirs()

                val cargoPath = try {
                    if (osName.contains("win")) "where cargo".!!.trim
                    else "which cargo".!!.trim
                } catch {
                    case _: Exception => "cargo"
                }

                val buildResult = Process(Seq(cargoPath, "build", "--release"), rustProjectDir).!
                if (buildResult != 0) {
                    sys.error("Failed to build native library with cargo")
                }

                if (sourceLib.exists()) {
                    IO.copyFile(sourceLib, targetLib)
                    if (!osName.contains("win")) {
                        s"chmod +x ${targetLib.absolutePath}".!
                    }
                    println(s"Native library built: ${targetLib.absolutePath}")
                } else {
                    sys.error(s"Native library not found at: ${sourceLib.absolutePath}")
                }
            } else {
                println(s"Native library already exists: ${targetLib.absolutePath}")
            }
        },
        // Run buildNativeLib before compile
        Compile / compile := (Compile / compile).dependsOn(buildNativeLib).value,
        Test / fork := true,
        Test / baseDirectory := (ThisBuild / baseDirectory).value,
        Test / javaOptions ++= Seq(
            s"-Djava.library.path=${(ThisBuild / baseDirectory).value}/modules/crypto/native/build/$osDirName:${(ThisBuild / baseDirectory).value}/modules/crypto/src/main/resources",
            s"-Djam.base.dir=${(ThisBuild / baseDirectory).value}"
        )
    )

lazy val pvm = (project in file("modules/pvm"))
    .dependsOn(core)
    .settings(
        name := "jam-pvm",
        libraryDependencies ++= Seq(
            "org.typelevel" %% "spire" % "0.18.0",
            "org.scalatest" %% "scalatest" % "3.2.17" % Test,
            "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
            "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
            "io.circe" %% "circe-core" % "0.14.6" % Test,
            "io.circe" %% "circe-generic" % "0.14.6" % Test,
            "io.circe" %% "circe-parser" % "0.14.6" % Test
        ),
        scalacOptions ++= Seq(
            "-deprecation",
            "-feature",
            "-unchecked",
            "-language:implicitConversions",
            "-language:higherKinds"
        )
    )

lazy val protocol = (project in file("modules/protocol"))
    .dependsOn(core, crypto, pvm)
    .settings(
        name := "jam-protocol",
        libraryDependencies ++= Seq(
            "org.typelevel" %% "spire" % "0.18.0",
            "org.bouncycastle" % "bcprov-jdk18on" % "1.77",
            // circe for JSON parsing - needed for STF types and test loading
            "io.circe" %% "circe-core" % "0.14.6",
            "io.circe" %% "circe-generic" % "0.14.6",
            "io.circe" %% "circe-parser" % "0.14.6",
            "org.scalatest" %% "scalatest" % "3.2.17" % Test
        ),
        scalacOptions ++= Seq(
            "-deprecation",
            "-feature",
            "-unchecked",
            "-language:implicitConversions",
            "-language:higherKinds"
        ),
        Test / fork := true,
        Test / baseDirectory := (ThisBuild / baseDirectory).value,
        Test / javaOptions ++= Seq(
            s"-Djava.library.path=${(ThisBuild / baseDirectory).value}/modules/crypto/native/build/$osDirName:${(ThisBuild / baseDirectory).value}/modules/crypto/src/main/resources",
            s"-Djam.base.dir=${(ThisBuild / baseDirectory).value}"
        )
    )
