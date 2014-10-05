import sbt._
import sbt.Keys._
import xerial.sbt.Pack._
import WebAppPlugin._

object Build extends sbt.Build {
  lazy val dist = TaskKey[File]("dist", "Builds the distribution packages")

  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.gitgrid",
    version := "0.0.6-SNAPSHOT",
    scalaVersion := "2.11.2",
    scalacOptions <<= baseDirectory.map(bd =>
      Seq("-encoding", "utf8") ++
      Seq("-sourcepath", bd.getAbsolutePath)),
    testOptions in Test += Tests.Argument("junitxml", "console")
  )

  lazy val commonProjectSettings = commonSettings

  lazy val core = (project in file("gitgrid-core"))
    .settings(commonProjectSettings: _*)

  lazy val server = (project in file("gitgrid-server"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core % "compile->compile;test->test")
    .dependsOn(worker % "compile->compile;test->test")

  lazy val worker = (project in file("gitgrid-worker"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core % "compile->compile;test->test")

  lazy val web = (project in file("gitgrid-web"))
    .settings(commonProjectSettings: _*)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(name := "gitgrid")
    .settings(resolvers in ThisBuild ++= Seq(
      "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
    ))
    .settings(dist <<= (streams, target, pack in server, pack in worker, webAppBuild in web) map { (s, target, server, worker, web) =>
      val distDir = target / "dist"
      s.log(s"Composing all parts to $distDir" )

      val serverDir = distDir / "server"
      val serverBinDir = serverDir / "bin"
      val serverWebDir = serverDir / "web"
      IO.copyDirectory(server, serverDir)
      IO.copyDirectory(web, serverWebDir)
      serverBinDir.listFiles.foreach(_.setExecutable(true, false))

      val workerDir = distDir / "worker"
      val workerBinDir = workerDir / "bin"
      IO.copyDirectory(worker, workerDir)
      workerBinDir.listFiles.foreach(_.setExecutable(true, false))

      distDir
    })
    .aggregate(core, server, worker, web)
}
