name := "gitgrid-server"

packSettings

packMain := Map("server" -> "com.gitgrid.Server")

packExtraClasspath := Map("server" -> Seq("${PROG_HOME}/config"))

libraryDependencies ++= {
  val sprayVersion = "1.3.1"
  val dependencies = Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.6"
  )
  val testDependencies = Seq(
    "io.spray" %% "spray-testkit" % sprayVersion
  ).map(_ % "test")
  dependencies ++ testDependencies
}
