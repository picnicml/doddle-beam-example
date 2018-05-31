import sbt._

object Dependencies {

  object V {
    val doodleVersion = "0.0.0-SNAPSHOT"
    val scioVersion = "0.5.4"
    val beamRunnerVersion = "2.4.0"
    val slf4jVersion = "1.7.25"
  }

  val compileDependencies: Seq[ModuleID] = Seq(
    "com.picnicml" %% "doddle-model" % V.doodleVersion,
    "com.spotify" %% "scio-core" % V.scioVersion,
    "org.apache.beam" % "beam-runners-direct-java" % V.beamRunnerVersion,
    "org.slf4j" % "slf4j-nop" % V.slf4jVersion
  )

  def settings: Seq[ModuleID] = {
    compileDependencies
  }
}
