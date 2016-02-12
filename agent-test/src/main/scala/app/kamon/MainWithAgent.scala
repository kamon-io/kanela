package app.kamon

import app.kamon.instrumentation.Pepe

object MainWithAgent {

  def main(args: Array[String]) {
    println("Start Run Agent Test")
    new Pepe().hello()
    println("Exit Run Agent Test")
  }

}
