import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "mongo-app"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
    //"play.modules.reactivemongo" %% "play2-reactivemongo" % "0.1-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    requireJs += "main.js"
  )
}