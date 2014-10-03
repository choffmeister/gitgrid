name := "gitgrid-server"

packSettings

packMain := Map("server" -> "com.gitgrid.Server")

packExtraClasspath := Map("server" -> Seq("${PROG_HOME}/config"))

libraryDependencies ++= {
  val akkaVersion = "2.2.3"
  val sprayVersion = "1.2.0"
  val dependencies = Seq(
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.5"
  )
  val testDependencies = Seq(
    "io.spray" % "spray-testkit" % sprayVersion
  ).map(_ % "test")
  dependencies ++ testDependencies
}
