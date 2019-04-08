/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent.util;

import io.vavr.control.Try;
import kanela.agent.util.log.Logger;
import lombok.val;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;


public class Execution {

    public static void timed(final Runnable thunk) {
        val startMillis = System.nanoTime();
        try {
            thunk.run();
        } finally {
            val timeSpent = MILLISECONDS.convert((System.nanoTime() - startMillis), NANOSECONDS);
            Logger.info(() -> "Startup completed in " + timeSpent + " ms");
        }
    }

    public static void runWithTimeSpent(final Runnable thunk) {
        try {
            timed(thunk);
        } catch (Throwable cause) {
            Logger.error(() -> "Unable to start Kanela Agent. Please remove -javaagent from your startup arguments and contact Kanela support." + cause);
            Try.run(() -> Thread.sleep(100)); //sleep before exit;
            System.exit(1);
        }
    }
}
