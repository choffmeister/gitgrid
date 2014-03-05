package com.gitgrid

import akka.actor._
import akka.util._
import org.specs2.specification.Scope
import scala.concurrent.duration._

trait TestActorSystem extends Scope with AsyncUtils {
  implicit val system = ActorSystem("testactorsystem")
  implicit val timeout = Timeout(1.seconds)
}
