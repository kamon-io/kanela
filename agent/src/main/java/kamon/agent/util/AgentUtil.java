/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

import kamon.agent.util.log.LazyLogger;

import java.util.function.Consumer;

import static java.text.MessageFormat.format;

public class AgentUtil {

    public static void withTimeLogging(final Runnable thunk, String message) {
        withTimeSpent(thunk, (timeSpent) -> LazyLogger.info(() -> format("{0} {1} ms", message, timeSpent)));
    }

    private static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) {
        long startMillis = System.currentTimeMillis();
        thunk.run();
        timeSpent.accept(System.currentTimeMillis() - startMillis);
    }
}
