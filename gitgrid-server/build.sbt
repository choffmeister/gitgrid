name := "gitgrid-server"

packSettings

packMain := Map("server" -> "com.gitgrid.Server")

packExtraClasspath := Map("server" -> Seq("${PROG_HOME}/config"))

libraryDependencies ++= {
  val akkaVersion = "2.2.3"
  val sprayVersion = "1.2.0"
  val dependencies = Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.jcraft" % "jsch" % "0.1.50",
    "com.typesafe" % "config" % "1.2.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "commons-codec" % "commons-codec" % "1.9",
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
