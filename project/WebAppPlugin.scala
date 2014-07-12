import sbt._
import Keys._

case class WebAppToolsVersions(
  nodeVersion: Option[VersionString],
  npmVersion: Option[VersionString],
  bowerVersion: Option[VersionString],
  gulpVersion: Option[VersionString])

object WebAppPlugin extends Plugin {
  val webAppTest = taskKey[Unit]("executes gulp task 'test'")
  val webAppBuild = taskKey[Unit]("executes gulp task 'build --dist'")
  val webAppStart = taskKey[Unit]("starts a gulp development server as background process")
  val webAppStop = taskKey[Unit]("stops the running gulp development server backgrund process")

  val webAppToolsVersions = taskKey[WebAppToolsVersions]("retrieves the versions of node, npm, bower and gulp")
  val webAppToolsInit = taskKey[Unit]("checks for node, npm, bower and gulp and installs node modules and bower components")
  val webAppDir = settingKey[File]("the path to the wep app root directory")

  lazy val webAppSettings = Seq[Def.Setting[_]](
    webAppTest := { runGulp(webAppDir.value, "test", dist = false) },
    webAppBuild := { runGulp(webAppDir.value, "build", dist = true) },
    webAppStart := { startGulp(webAppDir.value, "default", dist = false) },
    webAppStop := { stopGulp() },

    webAppToolsVersions := {
      def getVersion(name: String): Option[VersionString] = {
        try {
          VersionString(s"$name --version" !!)
        } catch {
          case e: Throwable => None
        }
      }

      val node = getVersion("node")
      val npm = getVersion("npm")
      val bower = getVersion("bower")
      val gulp = getVersion("gulp")

      WebAppToolsVersions(node, npm, bower, gulp)
    },

    webAppToolsInit := {
      webAppToolsVersions.value match {
        case WebAppToolsVersions(Some(node), Some(npm), Some(bower), Some(gulp)) =>
          println("Versions:")
          println("- NodeJS " + node)
          println("- NPM " + npm)
          println("- Bower " + bower)
          println("- Gulp " + gulp)
        case WebAppToolsVersions(None, _, _, _) =>
          throw new Exception("NodeJS is not installed. Please refer to http://nodejs.org/ for installation instructions.")
        case WebAppToolsVersions(_, None, _, _) =>
          throw new Exception("NPM is not installed. Please refer to http://nodejs.org/ for installation instructions.")
        case WebAppToolsVersions(_, _, None, _) =>
          throw new Exception("Bower is not installed. Please execute 'npm install -g bower'.")
        case WebAppToolsVersions(_, _, _, None) =>
          throw new Exception("Gulp is not installed. Please execute 'npm install -g gulp'.")
      }

      val webDir: File = webAppDir.value
      npmInstall(webDir)
      bowerInstall(webDir)
    },

    webAppDir := baseDirectory.value / "src/web"
  )

  private def npmInstall(cwd: File) {
    val command = "npm" :: "install" :: Nil
    val returnValue = Process(command, cwd) !

    if (returnValue != 0) {
      throw new Exception("Installing Node modules failed")
    }
  }

  private def bowerInstall(cwd: File) {
    val command = "bower" :: "install" :: Nil
    val returnValue = Process(command, cwd) !

    if (returnValue != 0) {
      throw new Exception("Installing Bower components failed")
    }
  }

  private def runGulp(cwd: File, task: String, dist: Boolean) {
    val command = dist match {
      case false => "gulp" :: task :: Nil
      case true => "gulp" :: task :: "--dist" :: Nil
    }
    val returnValue = Process(command, cwd) !

    if (returnValue != 0) {
      throw new Exception(s"Gulp task $task failed")
    }
  }

  private def startGulp(cwd: File, task: String, dist: Boolean) {
    if (running) {
      stopGulp()
    }

    process = dist match {
      case false => Process("gulp" :: task :: Nil, cwd).run()
      case true => Process("gulp" :: task :: "--dist" :: Nil, cwd).run()
    }
    running = true
  }

  private def stopGulp() {
    process.destroy()
    running = true
  }

  private var running: Boolean = false
  private var process: Process = _
}

case class VersionString(major: Int, minor: Int, patch: Int, pre: Option[String]) extends Ordered[VersionString] {
  def compare(that: VersionString): Int = {
    if (major < that.major) -1
    else if (major > that.major) 1
    else {
      if (minor < that.minor) -1
      else if (minor > that.minor) 1
      else {
        if (patch < that.patch) -1
        else if (patch > that.patch) 1
        else {
          (pre, that.pre) match {
            case (None, None) => 0
            case (Some(_), None) => -1
            case (None, Some(_)) => 1
            case (Some(pre1), Some(pre2)) => pre1.compare(pre2)
          }
        }
      }
    }
  }

  override def toString(): String = this match {
    case VersionString(major, minor, patch, Some(pre)) => s"$major.$minor.$patch-$pre"
    case _ => s"$major.$minor.$patch"
  }
}

object VersionString {
  def apply(str: String): Option[VersionString] = {
    val regexString = """(\d+)\.(\d+).(\d+)(\-(.+))?"""
    val regex = regexString.r

    regex findFirstIn str match {
      case Some(regex(major, minor, patch, _, pre)) =>
        Option(pre) match {
          case Some(pre) => Some(VersionString(major.toInt, minor.toInt, patch.toInt, Some(pre)))
          case None => Some(VersionString(major.toInt, minor.toInt, patch.toInt, None))
        }
      case _ => None
    }
  }
}
