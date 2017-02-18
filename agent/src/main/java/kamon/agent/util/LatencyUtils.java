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

package kamon.agent.util;

import lombok.SneakyThrows;
import lombok.val;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;


public class LatencyUtils {

    public static long timed(final Runnable thunk) {
        val startMillis = System.nanoTime();
        thunk.run();
        return MILLISECONDS.convert((System.nanoTime() - startMillis), NANOSECONDS);
    }

    public static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) { timeSpent.accept(timed(thunk));}

    @SneakyThrows
    public static <T> T withTimeSpent(final Callable<T> thunk, Consumer<Long> timeSpent) {
        val startMillis = System.nanoTime();
        try { return thunk.call();}
        finally { timeSpent.accept(MILLISECONDS.convert((System.nanoTime() - startMillis), NANOSECONDS));}
    }
}
