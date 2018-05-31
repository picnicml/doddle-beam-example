lazy val root = (project in file("."))
  .settings(
    name := "doddle-beam-example",
    organization := "com.picnicml",
    version := Version(),
    scalaVersion := "2.12.6",
    libraryDependencies ++= Dependencies.settings
  )
