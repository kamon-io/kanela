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

package app.kamon;

import app.kamon.java.FakeWorker;
import app.kamon.java.instrumentation.mixin.MonitorAware;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

public class JavaMainWithAgent {

    private static Logger logger = LoggerFactory.getLogger(JavaMainWithAgent.class);

    public static void main(String[] args) {
        logger.info("Start Run Agent Test with Java instrumentation version");
        val worker = FakeWorker.newInstance();
        IntStream.rangeClosed(1, 8)
                .forEach((int value) -> {
                    worker.heavyTask();
                    worker.lightTask();
                });
        logMetrics((MonitorAware) (Object) worker);
        logger.info("Exit Run Agent Test");
    }

    private static void logMetrics(MonitorAware monitor) {
        monitor.execTimings().forEach((methodName, samples) ->
                MetricsReporter.report(methodName, samples.map(Long::doubleValue).toJavaList()));
    }
}
