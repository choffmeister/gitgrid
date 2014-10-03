name := "gitgrid-core"

libraryDependencies ++= {
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.2.0"
  val dependencies = Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.jcraft" % "jsch" % "0.1.50",
    "com.typesafe" % "config" % "1.2.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "commons-codec" % "commons-codec" % "1.9",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "3.5.0.201409260305-r",
    "org.reactivemongo" %% "reactivemongo" % "0.10.5.akka23-SNAPSHOT"
  )
  val testDependencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "org.specs2" %% "specs2" % "2.4.1"
  ).map(_ % "test")
  dependencies ++ testDependencies
}
