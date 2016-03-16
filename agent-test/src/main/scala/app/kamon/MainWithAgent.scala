package app.kamon

import app.kamon.instrumentation.Pepe
import org.slf4j.LoggerFactory

object MainWithAgent {

  val logger = LoggerFactory.getLogger(MainWithAgent.getClass)

  def main(args: Array[String]) {
    logger.info("Start Run Agent Test")
    val pepe = new Pepe()
    pepe.hello()
    pepe.bye()
    logger.info("Exit Run Agent Test")
  }

}

