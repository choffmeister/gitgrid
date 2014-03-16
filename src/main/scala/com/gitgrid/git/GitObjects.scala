package com.gitgrid.git

import java.io._
import java.util.Date
import org.eclipse.jgit.lib._

abstract class GitObject {
  val id: String
}

sealed abstract class GitObjectType
case object GitCommitObjectType extends GitObjectType
case object GitTreeObjectType extends GitObjectType
case object GitBlobObjectType extends GitObjectType
case object GitTagObjectType extends GitObjectType

object GitObjectType {
  def apply(i: Int): GitObjectType = i match {
    case 1 => GitCommitObjectType
    case 2 => GitTreeObjectType
    case 3 => GitBlobObjectType
    case 4 => GitTagObjectType
    case _ => throw new Exception("GitObjectType '$i' is not supported")
  }
}

case class GitRef(name: String, id: String)
case class GitCommitSignature(name: String, email: String, when: Date, timeZone: Int)
case class GitCommit(id: String, parents: List[String], tree: String, author: GitCommitSignature, committer: GitCommitSignature, fullMessage: String, shortMessage: String) extends GitObject
case class GitTree(id: String, entries: List[GitTreeEntry]) extends GitObject
case class GitTreeEntry(id: String, name: String, fileMode: String, objectType: GitObjectType)
case class GitBlob(id: String) extends GitObject {
  def readAsStream[T](repo: GitRepository)(inner: InputStream => T): T = {
    val stream = repo.jgit.open(ObjectId.fromString(id)).openStream()
    try {
      inner(stream)
    } finally {
      stream.close()
    }
  }

  def readAsBytes(repo: GitRepository): Seq[Byte] = repo.jgit.open(ObjectId.fromString(id)).getBytes().toSeq
  def readAsString(repo: GitRepository): String = new String(repo.jgit.open(ObjectId.fromString(id)).getBytes(), "UTF-8")
}
