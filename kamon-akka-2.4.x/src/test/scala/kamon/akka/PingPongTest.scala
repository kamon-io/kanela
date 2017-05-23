package kamon.akka

import java.util.concurrent.atomic.LongAdder

import akka.actor.{ Props, ActorSystem, Actor }
import kamon.Kamon

object PingPong extends App {
  Kamon.start()
  val counter = new LongAdder()
  val system = ActorSystem("ping-pong")

  for (i ← 1 to 16) {
    val pinger = system.actorOf(Props[Pinger], s"pinger-$i")
    val ponger = system.actorOf(Props[Ponger], s"ponger-$i")

    for (_ ← 1 to 100) {
      pinger.tell(Pong, ponger)
    }
  }

  import system.dispatcher
  import scala.concurrent.duration._
  system.scheduler.schedule(1 second, 1 second) {
    println(counter.sumThenReset())
  }

  system.scheduler.scheduleOnce(120 seconds) {
    system.shutdown()
  }
}

case object Ping
case object Pong

class Pinger extends Actor {
  def receive = {
    case Pong ⇒
      PingPong.counter.increment()
      sender ! Ping
  }
}

class Ponger extends Actor {
  def receive = {
    case Ping ⇒
      PingPong.counter.increment()
      sender ! Pong
  }
}