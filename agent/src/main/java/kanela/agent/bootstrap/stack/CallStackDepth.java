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

package kanela.agent.bootstrap.stack;

import lombok.Value;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for measure the call stack depth
 */
@Value
public class CallStackDepth {
    private static final ThreadLocal<Map<Object, Integer>> StackDepthThreadLocal = ThreadLocal.withInitial(HashMap::new);

    public static int incrementFor(final Object key) {
        val stackDepthMap = StackDepthThreadLocal.get();
        if(stackDepthMap.containsKey(key)) return stackDepthMap.compute(key, (k, v) -> v + 1);
        return stackDepthMap.computeIfAbsent(key, v -> 0);
    }

    public static void resetFor(final Object obj) {
        StackDepthThreadLocal.get().remove(obj);
    }
}


