package app.kamon.cases.stoppable

import scala.util.Random

class Worker {

  private val r = Random.self

  def performTask(): Unit = Thread.sleep((r.nextFloat() * 500) toLong)
}
