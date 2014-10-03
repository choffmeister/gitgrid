package com.gitgrid.utils

import org.specs2.mutable._
import reactivemongo.bson.BSONObjectID

class NonceGeneratorSpec extends Specification {
  def newId = BSONObjectID.generate

  "NonceGenerator" should {
    "generate random bytes" in {
      val b1 = NonceGenerator.generateBytes(16).toSeq
      val b2 = NonceGenerator.generateBytes(16).toSeq
      val b3 = NonceGenerator.generateBytes(16).toSeq

      b1 must haveSize(16)
      b1 !== b2
      b1 !== b3
      b2 !== b3
    }

    "generate random strings" in {
      val s1 = NonceGenerator.generateString(16)
      val s2 = NonceGenerator.generateString(16)
      val s3 = NonceGenerator.generateString(16)

      s1 must haveSize(32)
      s1 !== s2
      s1 !== s3
      s2 !== s3
    }
  }
}
