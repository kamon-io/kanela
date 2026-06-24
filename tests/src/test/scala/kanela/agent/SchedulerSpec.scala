/*
 *  ==========================================================================================
 *  Copyright © 2013-2025 The Kamon Project <https://kamon.io/>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *  ==========================================================================================
 */

package kanela.agent

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

class SchedulerSpec extends munit.FunSuite {

  test("should not keep a short-lived JVM running after the main method exits") {
    val result = runProbe("kanela.agent.AgentProcessExitProbe", timeoutSeconds = 5)

    assert(
      !result.timedOut,
      s"Probe JVM did not exit naturally. Output:\n${result.output}"
    )
    assertEquals(result.exitCode, 0, result.output)
  }

  test("should release scheduler threads after reloading the agent") {
    val result = runProbe("kanela.agent.AgentReloadProbe", timeoutSeconds = 10)

    assert(
      !result.timedOut,
      s"Reload probe JVM timed out. Output:\n${result.output}"
    )
    assertEquals(result.exitCode, 0, result.output)
  }

  private def runProbe(mainClass: String, timeoutSeconds: Long): ProbeResult = {
    val process = new ProcessBuilder(
      Seq(
        javaExecutable,
        "-javaagent:" + agentJar,
        "-cp",
        System.getProperty("java.class.path"),
        mainClass
      ).asJava
    ).redirectErrorStream(true).start()

    val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
    if (finished)
      ProbeResult(timedOut = false, exitCode = process.exitValue(), output = readOutput(process.getInputStream))
    else {
      process.destroyForcibly()
      process.waitFor(5, TimeUnit.SECONDS)
      ProbeResult(timedOut = true, exitCode = -1, output = readOutput(process.getInputStream))
    }
  }

  private def agentJar: String =
    sys.props.getOrElse("kanela.test.agent.jar", fail("Missing kanela.test.agent.jar system property"))

  private def javaExecutable: String = {
    val executable = if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
    new java.io.File(new java.io.File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath
  }

  private def readOutput(inputStream: InputStream): String = {
    val buffer = new Array[Byte](1024)
    val output = new ByteArrayOutputStream()
    try {
      var read = inputStream.read(buffer)
      while (read != -1) {
        output.write(buffer, 0, read)
        read = inputStream.read(buffer)
      }
      output.toString("UTF-8")
    } catch {
      case e: java.io.IOException =>
        output.toString("UTF-8") + s"\n[failed to read process output: ${e.getMessage}]"
    }
  }
}

private final case class ProbeResult(timedOut: Boolean, exitCode: Int, output: String)

object AgentProcessExitProbe {
  def main(args: Array[String]): Unit =
    println("agent-process-exit-probe-finished")
}

object AgentReloadProbe {
  def main(args: Array[String]): Unit = {
    Kanela.reload()

    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    var lastObserved = schedulerThreads

    while (System.nanoTime() < deadline) {
      lastObserved = schedulerThreads
      if (lastObserved.nonEmpty && lastObserved.size <= 1 && lastObserved.forall(_.isDaemon)) {
        println(describe(lastObserved))
        sys.exit(0)
      }

      Thread.sleep(100)
    }

    Console.err.println(describe(lastObserved))
    sys.exit(1)
  }

  private def schedulerThreads: Seq[Thread] =
    Thread.getAllStackTraces.keySet.asScala.toSeq.filter(_.getName == "kanela-scheduled-executor")

  private def describe(threads: Seq[Thread]): String =
    threads.map(thread => s"${thread.getName}(daemon=${thread.isDaemon}, state=${thread.getState})").mkString(
      "scheduler-threads=[",
      ", ",
      "]"
    )
}
