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


import lombok.Value;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Value(staticConstructor = "instance")
public class NamedThreadFactory implements ThreadFactory {
    AtomicInteger threadNumber = new AtomicInteger(1);
    String name;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, String.format("kamon-agent-" + name + "-%s", threadNumber.getAndIncrement()));
        thread.setDaemon(true);
        return thread;
    }
}