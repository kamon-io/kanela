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

package app.kamon.java.instrumentation.mixin;

import io.vavr.collection.List;
import io.vavr.control.Option;
import kamon.agent.api.instrumentation.mixin.Initializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonitorMixin implements MonitorAware {

    private Map<String, List<Long>> _execTimings;

    @Override
    public List<Long> execTimings(String methodName) {
        return _execTimings.getOrDefault(methodName, List.empty());
    }

    @Override
    public Map<String, List<Long>> execTimings() {
        return _execTimings;
    }

    @Override
    public List<Long> addExecTimings(String methodName, long time) {
        return this._execTimings.compute(methodName, (key, oldValues) -> Option.of(oldValues).map(vs -> vs.append(time)).getOrElse(List.of(time)));
    }

    @Initializer
    public void init() {
        this._execTimings = new ConcurrentHashMap<>();
    }
}
