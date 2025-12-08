// Scala 3 PVM module for JAM
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "io.forge.jam"
ThisBuild / version := "0.1.0-SNAPSHOT"

// Reference to the core module
lazy val core = ProjectRef(file("../core"), "root")

lazy val root = (project in file("."))
  .dependsOn(core)
  .settings(
    name := "jam-pvm",
    
    // Spire for unsigned types (UByte, UShort, UInt, ULong)
    libraryDependencies ++= Seq(
      "org.typelevel" %% "spire" % "0.18.0",
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test
    ),
    
    // Scala 3 compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds"
    )
  )
