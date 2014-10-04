package com.gitgrid

import com.gitgrid.git._

object Tasks {
  case class BuildFromCommit(projectId: String, commit: GitCommit, ownerName: String, projectName: String)
}
