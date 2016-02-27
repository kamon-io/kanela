package app.kamon

import app.kamon.instrumentation.Pepe
import org.slf4j.LoggerFactory

object MainWithAgent {

  def logger = LoggerFactory.getLogger(MainWithAgent.getClass)

  def main(args: Array[String]) {
    logger.info("Start Run Agent Test")
    new Pepe().hello()
    logger.info("Exit Run Agent Test")
  }

}

