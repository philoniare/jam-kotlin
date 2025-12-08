// Scala 3 Core module for JAM - shared primitive types
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "io.forge.jam"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "jam-core",
    
    // Spire for unsigned types (UByte, UShort, UInt, ULong)
    libraryDependencies ++= Seq(
      "org.typelevel" %% "spire" % "0.18.0",
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
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
