package com.gitgrid.git

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.models._
import java.io._
import org.eclipse.jgit.transport.{UploadPack, ReceivePack}
import scala.util.Failure
import scala.util.Success
import spray.can._
import spray.http.CacheDirectives._
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.encoding._
import spray.routing.AuthenticationFailedRejection
import spray.routing.RequestContext

class GitHttpServiceActor(cfg: Config, db: Database) extends Actor with ActorLogging {
  implicit val executor = context.dispatcher
  val authenticator = new GitGridHttpAuthenticator(db)
  val authorizer = new GitGridAuthorizer(db)

  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case req@GitHttpRequest(_, _, "info/refs", None) =>
      sender ! HttpResponse(status = Forbidden, entity = "Git dump HTTP protocol is not supported")

    case req@GitHttpRequest(ownerName, projectName, "info/refs", Some("git-upload-pack")) =>
      project(sender, ownerName, projectName) { (sender, project) =>
        authorize(req, sender, ProjectRepositoryReadWrite(project)) { sender =>
          openRepository(ownerName, projectName, sender) { repo =>
            val in = decodeRequest(req).entity.data.toByteArray
            val out = uploadPack(repo, in, biDirectionalPipe = true) // must be true, since else sendAdvertisedRefs is not invoked
            encodeResponse(HttpResponse(entity = HttpEntity(GitHttpServiceConstants.gitUploadPackAdvertisement, GitHttpServiceConstants.gitUploadPackHeader ++ out), headers = GitHttpServiceConstants.noCacheHeaders), req.acceptedEncodingRanges)
          }
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "info/refs", Some("git-receive-pack")) =>
      project(sender, ownerName, projectName) { (sender, project) =>
        authorize(req, sender, ProjectRepositoryReadWrite(project)) { sender =>
          openRepository(ownerName, projectName, sender) { repo =>
            val in = decodeRequest(req).entity.data.toByteArray
            val out = receivePack(repo, in, biDirectionalPipe = true) // must be true, since else sendAdvertisedRefs is not invoked
            encodeResponse(HttpResponse(entity = HttpEntity(GitHttpServiceConstants.gitReceivePackAdvertisement, GitHttpServiceConstants.gitReceivePackHeader ++ out), headers = GitHttpServiceConstants.noCacheHeaders), req.acceptedEncodingRanges)
          }
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "git-upload-pack", None) =>
      project(sender, ownerName, projectName) { (sender, project) =>
        authorize(req, sender, ProjectRepositoryReadWrite(project)) { sender =>
          openRepository(ownerName, projectName, sender) { repo =>
            val in = decodeRequest(req).entity.data.toByteArray
            val out = uploadPack(repo, in, biDirectionalPipe = false)
            encodeResponse(HttpResponse(entity = HttpEntity(GitHttpServiceConstants.gitUploadPackResult, out), headers = GitHttpServiceConstants.noCacheHeaders), req.acceptedEncodingRanges)
          }
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "git-receive-pack", None) =>
      project(sender, ownerName, projectName) { (sender, project) =>
        authorize(req, sender, ProjectRepositoryReadWrite(project)) { sender =>
          openRepository(ownerName, projectName, sender) { repo =>
            val in = decodeRequest(req).entity.data.toByteArray
            val out = receivePack(repo, in, biDirectionalPipe = false)
            encodeResponse(HttpResponse(entity = HttpEntity(GitHttpServiceConstants.gitUploadPackResult, out), headers = GitHttpServiceConstants.noCacheHeaders), req.acceptedEncodingRanges)
          }
        }
      }

    case _ =>
      sender ! HttpResponse(BadRequest)
  }

  private def authorize(req: HttpRequest, sender: ActorRef, action: Any)(inner: ActorRef => Any): Unit = {
    val ctx = RequestContext(req, sender, Uri.Path(""))

    authenticator.authenticateByBasicHttp(ctx).map {
      case Right(user) =>
        authorize(sender, Some(user), action)(inner)
      case Left(AuthenticationFailedRejection(_, challengeHeaders)) =>
        sender ! HttpResponse(Unauthorized, headers = challengeHeaders)
      case _ =>
        sender ! HttpResponse(InternalServerError)
    }
  }

  private def authorize(sender: ActorRef, user: Option[User], action: Any)(inner: ActorRef => Any): Unit = {
    authorizer.authorize(user, action).onComplete {
      case Success(true) =>
        inner(sender)
      case Success(false) =>
        sender ! HttpResponse(Unauthorized, "You are not allow to access this repository")
      case Failure(ex) =>
        sender ! HttpResponse(InternalServerError)
    }
  }

  private def project(sender: ActorRef, ownerName: String, projectName: String)(inner: (ActorRef, Project) => Any): Unit = {
    db.projects.findByFullQualifiedName(ownerName, projectName).onComplete {
      case Success(Some(project)) => inner(sender, project)
      case Success(None) => sender ! HttpResponse(NotFound)
      case Failure(_) => sender ! HttpResponse(InternalServerError)
    }
  }

  private def openRepository(userName: String, projectName: String, sender: ActorRef)(inner: GitRepository => HttpResponse) = {
    db.projects.findByFullQualifiedName(userName, projectName).map {
      case Some(project) => GitRepository(new File(cfg.repositoriesDir, project.id.get.stringify))(inner)
      case _ => HttpResponse(NotFound)
    }.onComplete {
      case Success(res: HttpResponse) =>
        sender ! res
      case Failure(ex) =>
        log.error(ex, "Error while accessing git repository")
        sender ! HttpResponse(InternalServerError)
    }
  }

  private def uploadPack(repo: GitRepository, in: Array[Byte], biDirectionalPipe: Boolean): Array[Byte] = {
    val up = new UploadPack(repo.jgit)
    up.setBiDirectionalPipe(biDirectionalPipe)
    val is = new ByteArrayInputStream(in)
    val os = new ByteArrayOutputStream()
    up.upload(is, os, null)
    os.toByteArray
  }

  private def receivePack(repo: GitRepository, in: Array[Byte], biDirectionalPipe: Boolean): Array[Byte] = {
    val rp = new ReceivePack(repo.jgit)
    rp.setBiDirectionalPipe(biDirectionalPipe)
    val is = new ByteArrayInputStream(in)
    val os = new ByteArrayOutputStream()
    rp.receive(is, os, null)
    os.toByteArray
  }

  private def encodeResponse(res: HttpResponse, acceptedEncodingRanges: List[HttpEncodingRange]): HttpResponse = {
    @scala.annotation.tailrec
    def encode(res: HttpResponse, encoders: List[Encoder]): HttpResponse = encoders match {
      case first :: more if acceptedEncodingRanges.exists(_.matches(first.encoding)) => first.encode(res)
      case first :: more => encode(res, more)
      case Nil => res
    }

    encode(res, List(Gzip, Deflate))
  }

  private def decodeRequest(req: HttpRequest): HttpRequest = {
    @scala.annotation.tailrec
    def decode(req: HttpRequest, decoders: List[Decoder]): HttpRequest = decoders match {
      case first :: more if first.encoding == req.encoding => first.decode(req)
      case first :: more => decode(req, more)
      case Nil => throw new Exception(s"Encoding '${req.encoding}' is not supported")
    }

    decode(req, List(Gzip, Deflate, NoEncoding))
  }
}

object GitHttpServiceConstants {
  val noCacheHeaders = List(`Cache-Control`(`no-cache`, `max-age`(0), `must-revalidate`))
  val gitUploadPackHeader = "001e# service=git-upload-pack\n0000".getBytes("ASCII")
  val gitReceivePackHeader = "001f# service=git-receive-pack\n0000".getBytes("ASCII")
  val gitUploadPackAdvertisement = spray.http.MediaTypes.register(
    MediaType.custom(
      mainType = "application",
      subType = "x-git-upload-pack-advertisement",
      compressible = false,
      binary = true,
      fileExtensions = Seq()))
  val gitUploadPackResult = spray.http.MediaTypes.register(
    MediaType.custom(
      mainType = "application",
      subType = "x-git-upload-pack-result",
      compressible = false,
      binary = true,
      fileExtensions = Seq()))
  val gitReceivePackAdvertisement = spray.http.MediaTypes.register(
    MediaType.custom(
      mainType = "application",
      subType = "x-git-receive-pack-advertisement",
      compressible = false,
      binary = true,
      fileExtensions = Seq()))
  val gitReceivePackResult = spray.http.MediaTypes.register(
    MediaType.custom(
      mainType = "application",
      subType = "x-git-receive-pack-result",
      compressible = false,
      binary = true,
      fileExtensions = Seq()))
}

object GitHttpRequest {
  val pattern = """^/([a-zA-Z0-9\-\_]+)/([a-zA-Z0-9\-\_]+)\.git/(.*)$""".r

  def unapply(req: HttpRequest): Option[(String, String, String, Option[String])] = req.uri.path.toString() match {
    case pattern(ownerName, projectName, action) =>
      Some((ownerName, projectName, action, req.uri.query.get("service")))
    case _ => None
  }
}
