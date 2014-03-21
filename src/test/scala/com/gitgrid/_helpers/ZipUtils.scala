package com.gitgrid

import java.io._
import java.util.zip._

object ZipUtils {
  def unzip(zipStream: InputStream, targetDir: File) {
    val buffer = new Array[Byte](1024)
    val zip = new ZipInputStream(zipStream)
    var pos = Option(zip.getNextEntry)

    while (pos.isDefined) {
      val entry = pos.get
      val fileName = entry.getName
      val file = new File(targetDir, fileName)
      file.getParentFile.mkdirs()

      if (!entry.isDirectory) {
        val outStream = new FileOutputStream(file)
        var done = false
        while (!done) {
          val read = zip.read(buffer)
          if (read > 0) outStream.write(buffer, 0, read)
          else done = true
        }
        outStream.close()
      }

      pos = Option(zip.getNextEntry())
    }

    zip.closeEntry()
    zip.close()
  }
}
