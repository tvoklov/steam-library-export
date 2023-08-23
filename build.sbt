ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

val http4sVersion = "0.23.19"
val circeVersion  = "0.14.5"

lazy val root = (project in file(".")).settings(
  name := "steam-library-export",
  libraryDependencies ++=
    Seq(
      "org.http4s"    %% "http4s-blaze-server" % "0.23.15",
      "org.http4s"    %% "http4s-blaze-client" % "0.23.15",
      "org.http4s"    %% "http4s-circe"        % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "io.circe"      %% "circe-generic"       % circeVersion,
      "io.circe"      %% "circe-parser"        % circeVersion,
      "com.outr"      %% "scribe-slf4j"        % "3.11.2",
      "com.norbitltd" %% "spoiwo"              % "2.2.1",
    ),
)
