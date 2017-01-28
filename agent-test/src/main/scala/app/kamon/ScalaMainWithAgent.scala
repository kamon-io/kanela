/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package app.kamon

import app.kamon.instrumentation.mixin.MonitorAware
import org.slf4j.LoggerFactory

object ScalaMainWithAgent {
  import scala.collection.breakOut

  private val logger = LoggerFactory.getLogger(ScalaMainWithAgent.getClass)

  def main(args: Array[String]) {
    logger.info("Start Run Agent Test")
    logger.info("Greetings from Kamon Team!")
    val worker = FakeWorker()
    (1 to 10) foreach { _ ⇒
      worker.heavyTask()
      worker.lightTask()
    }
    logMetrics(worker.asInstanceOf[MonitorAware])
    logger.info("Exit Run Agent Test")
  }

  private def logMetrics(monitor: MonitorAware): Unit = {
    import collection.JavaConverters._

    monitor.execTimings foreach {
      case (methodName, samples) ⇒
        MetricsReporter.report(methodName, samples.asScala.map(sample ⇒ double2Double(sample.toDouble))(breakOut).toBuffer.asJava)
    }
  }

}

