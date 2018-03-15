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

package app.kamon.java.instrumentation.advisor;

import app.kamon.java.instrumentation.mixin.MonitorAware;
import kanela.agent.libs.net.bytebuddy.asm.Advice.*;
import lombok.val;

public class FakeWorkerAdvisor {

    @OnMethodEnter
    public static long onMethodEnter() {
        return System.nanoTime(); // Return current time, entering as parameter in the onMethodExist
    }

    @OnMethodExit
    public static void onMethodExit(@This MonitorAware instance, @Enter long start, @Origin String origin) {
        val timing = System.nanoTime() - start;
        instance.addExecTimings(origin, timing);
        System.out.println(String.format("Method %s was executed in %10.2f ns.", origin, (float) timing));
    }
}
