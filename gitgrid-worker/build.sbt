name := "gitgrid-worker"

packSettings

packMain := Map("worker" -> "com.gitgrid.Worker")

packExtraClasspath := Map("worker" -> Seq("${PROG_HOME}/config"))
