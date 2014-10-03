name := "gitgrid-worker"

packSettings

packMain := Map("worker" -> "com.gitgrid.Worker")

packExtraClasspath := Map("worker" -> Seq("${PROG_HOME}/config"))

libraryDependencies ++= {
  val akkaVersion = "2.2.3"
  val dependencies = Seq(
    "com.typesafe.akka" %% "akka-remote" % akkaVersion
  )
  dependencies
}
