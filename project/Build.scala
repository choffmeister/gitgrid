import sbt._
import sbt.Keys._
import xerial.sbt.Pack._
import WebAppPlugin._

object Build extends sbt.Build {
  lazy val dist = TaskKey[Unit]("dist", "Builds the distribution packages")

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

  lazy val web = (project in file("gitgrid-web"))
    .settings(commonProjectSettings: _*)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(name := "gitgrid")
    .settings(resolvers in ThisBuild ++= Seq(
      "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
    ))
    .settings(dist <<= (streams, target, pack in server, webAppBuild in web) map { (s, target, server, web) =>
      val dist = target / "dist"
      val bin = dist / "bin"
      s.log(s"Composing all parts to $dist" )
      IO.copyDirectory(server, dist)
      IO.copyDirectory(web, dist / "web")
      bin.listFiles.foreach(_.setExecutable(true, false))
    })
    .aggregate(core, server, web)
}
