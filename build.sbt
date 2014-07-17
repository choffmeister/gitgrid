import de.johoop.jacoco4sbt._
import JacocoPlugin._

name := "gitgrid"

version := "0.0.3-SNAPSHOT"

organization := "com.gitgrid"

scalaVersion := "2.10.4"

scalacOptions := Seq("-encoding", "utf8")

resolvers += "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= {
  val akkaVersion = "2.2.3"
  val sprayVersion = "1.2.0"
  val dependencies = Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.jcraft" % "jsch" % "0.1.50",
    "com.typesafe" % "config" % "1.2.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.5",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "3.2.0.201312181205-r",
    "org.reactivemongo" %% "reactivemongo" % "0.10.0"
  )
  val testDependencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "io.spray" % "spray-testkit" % sprayVersion,
    "org.specs2" %% "specs2" % "2.3.8"
  ).map(_ % "test")
  dependencies ++ testDependencies
}

webAppSettings

packSettings

packMain := Map("gitgrid" -> "com.gitgrid.Application")

packExtraClasspath := Map("gitgrid" -> Seq("${PROG_HOME}/config"))

packResourceDir += (target.value / "web" -> "web")

pack <<= pack.dependsOn(webAppBuild)

jacoco.settings

jacoco.reportFormats in jacoco.Config := Seq(
  XMLReport(encoding = "utf-8"),
  ScalaHTMLReport(withBranchCoverage = true)
)
