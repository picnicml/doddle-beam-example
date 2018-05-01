
lazy val root = (project in file("."))
  .settings(
    name := "doddle-beam-example",
    organization := "com.picnicml",
    version := "0.0.0",
    scalaVersion := "2.12.5",
    libraryDependencies ++= "com.picnicml" %% "doddle-model" % "0.0.0" ::
      "com.spotify" %% "scio-core" % "0.5.2" ::
      "org.apache.beam" % "beam-runners-direct-java" % "2.4.0" :: Nil
  )
