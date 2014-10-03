name := "gitgrid-server"

packSettings

packMain := Map("server" -> "com.gitgrid.Server")

packExtraClasspath := Map("server" -> Seq("${PROG_HOME}/config"))
