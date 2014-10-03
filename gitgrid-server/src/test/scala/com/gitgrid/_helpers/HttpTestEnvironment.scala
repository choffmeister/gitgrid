package com.gitgrid

trait HttpEmptyTestEnvironment extends EmptyTestEnvironment {
  val httpConf = HttpConfig.load()
}

trait HttpTestEnvironment extends TestEnvironment {
  val httpConf = HttpConfig.load()
}
