ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val http4sVersion = "1.0.0-M31"
val circeVersion  = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "steam-library-export",
    idePackagePrefix := Some("volk.steam.libraryexport"),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
      "org.http4s"    %% "http4s-blaze-client" % http4sVersion,
      "org.http4s"    %% "http4s-circe"        % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "io.circe"      %% "circe-generic"       % circeVersion,
      "io.circe"      %% "circe-parser"        % circeVersion,
      "com.outr"      %% "scribe-slf4j"        % "3.8.1",
      "com.norbitltd" %% "spoiwo"              % "2.1.0"
    )
  )
