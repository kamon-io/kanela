package app.kamon

import app.kamon.instrumentation.CustomInstrumentation

object MainWithAgent {

  def main(args: Array[String]) {
    println("Start Run Agent Test")
    new CustomInstrumentation()
    println("Exit Run Agent Test")
  }

}
