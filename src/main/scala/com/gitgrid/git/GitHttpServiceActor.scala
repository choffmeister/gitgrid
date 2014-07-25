package com.gitgrid.git

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth._
import com.gitgrid.models._
import java.io._
import org.eclipse.jgit.transport.{UploadPack, ReceivePack}
import spray.routing.authentication.Authentication
import scala.concurrent.Future
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
  import GitHttpServiceConstants._
  implicit val executor = context.dispatcher
  val authenticator = new GitGridHttpAuthenticator(cfg, db)

  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case req@GitHttpRequest(_, _, "info/refs", None) =>
      sender ! HttpResponse(status = NotImplemented, entity = "Git dump HTTP protocol is not supported")

    case req@GitHttpRequest(ownerName, projectName, "info/refs", Some("git-upload-pack")) =>
      authorize(sender, req, GitReadAccess, ownerName, projectName) { (sender, user, project) =>
        openRepository(ownerName, projectName, sender) { repo =>
          val in = decodeRequest(req).entity.data.toByteArray
          val out = uploadPack(repo, in, biDirectionalPipe = true) // must be true, since else sendAdvertisedRefs is not invoked
          encodeResponse(HttpResponse(entity = HttpEntity(gitUploadPackAdvertisement, gitUploadPackHeader ++ out), headers = noCacheHeaders), req.acceptedEncodingRanges)
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "info/refs", Some("git-receive-pack")) =>
      authorize(sender, req, GitWriteAccess, ownerName, projectName) { (sender, user, project) =>
        openRepository(ownerName, projectName, sender) { repo =>
          val in = decodeRequest(req).entity.data.toByteArray
          val out = receivePack(repo, in, biDirectionalPipe = true) // must be true, since else sendAdvertisedRefs is not invoked
          encodeResponse(HttpResponse(entity = HttpEntity(gitReceivePackAdvertisement, gitReceivePackHeader ++ out), headers = noCacheHeaders), req.acceptedEncodingRanges)
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "git-upload-pack", None) =>
      authorize(sender, req, GitReadAccess, ownerName, projectName) { (sender, user, project) =>
        openRepository(ownerName, projectName, sender) { repo =>
          val in = decodeRequest(req).entity.data.toByteArray
          val out = uploadPack(repo, in, biDirectionalPipe = false)
          encodeResponse(HttpResponse(entity = HttpEntity(gitUploadPackResult, out), headers = noCacheHeaders), req.acceptedEncodingRanges)
        }
      }

    case req@GitHttpRequest(ownerName, projectName, "git-receive-pack", None) =>
      authorize(sender, req, GitWriteAccess, ownerName, projectName) { (sender, user, project) =>
        openRepository(ownerName, projectName, sender) { repo =>
          val in = decodeRequest(req).entity.data.toByteArray
          val out = receivePack(repo, in, biDirectionalPipe = false)
          encodeResponse(HttpResponse(entity = HttpEntity(gitReceivePackResult, out), headers = noCacheHeaders), req.acceptedEncodingRanges)
        }
      }

    case _ =>
      sender ! HttpResponse(BadRequest)
  }

  private def authorize(sender: ActorRef, req: HttpRequest, accessType: GitAccessType, ownerName: String, projectName: String)(inner: (ActorRef, Option[User], Project) => Any): Unit = {
    val ctx = RequestContext(req, sender, Uri.Path.Empty)
    val f1 = authenticator.basicAuthenticator(ctx)
    val f2 = db.users.findByUserName(ownerName)
    val f3 = db.projects.findByFullQualifiedName(ownerName, projectName)
    val f = f1.zip(f2).zip(f3).map(t => (accessType, t._1._1, t._1._2, t._2))

    f.onSuccess {
      case (_, _, None, _) => sender ! HttpResponse(NotFound)
      case (_, Right(u), _, Some(p)) if p.ownerId == u.id => inner(sender, Some(u), p)
      case (_, Right(u), _, None) if u.userName == ownerName => sender ! HttpResponse(NotFound)
      case (GitReadAccess, Right(u), _, Some(p)) if p.public => inner(sender, Some(u), p)
      case (GitReadAccess, Left(_), _, Some(p)) if p.public => inner(sender, None, p)
      case (_, Right(u), _, _) => sender ! HttpResponse(Forbidden)
      case (_, Left(AuthenticationFailedRejection(_, challengeHeaders)), _, _) => sender ! HttpResponse(Unauthorized, headers = challengeHeaders)
    }
    f.onFailure {
      case _ => sender ! HttpResponse(InternalServerError)
    }
  }

  private def openRepository(userName: String, projectName: String, sender: ActorRef)(inner: GitRepository => HttpResponse) = {
    db.projects.findByFullQualifiedName(userName, projectName).map {
      case Some(project) => GitRepository(new File(cfg.repositoriesDir, project.id.stringify))(inner)
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

  abstract class GitAccessType
  case object GitReadAccess extends GitAccessType
  case object GitWriteAccess extends GitAccessType
}

object GitHttpRequest {
  val pattern = """^/([a-zA-Z0-9\-\_]+)/([a-zA-Z0-9\-\_]+)\.git/(.*)$""".r

  def unapply(req: HttpRequest): Option[(String, String, String, Option[String])] = req.uri.path.toString() match {
    case pattern(ownerName, projectName, action) =>
      Some((ownerName, projectName, action, req.uri.query.get("service")))
    case _ => None
  }
}
