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

package kamon.agent.util.matcher;

import kamon.agent.util.AgentUtil;
import kamon.agent.util.conf.AgentConfiguration;
import lombok.Value;
import lombok.val;
import net.bytebuddy.matcher.ElementMatcher;
import utils.AnsiColor;

import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Value
public class TimedMatcher<T> implements ElementMatcher<T> {

   String type;
   String transformer;
   String agentName;
   ElementMatcher<T> underlyingMatcher;
   ConcurrentHashMap<String, TypeMatcherMetrics> accumulatedTimeByType;

   private TimedMatcher(String agentName, String type, String transformer, ElementMatcher<T> underlyingMatcher) {
      this.type = type;
      this.transformer = transformer;
      this.agentName = agentName;
      this.underlyingMatcher = underlyingMatcher;
      this.accumulatedTimeByType = new ConcurrentHashMap<>();

       Runtime.getRuntime().addShutdownHook(new Thread(this::logMetrics));
   }

   public static <T> ElementMatcher<T> withTimeSpent(String agentName, String type, String transformer, ElementMatcher<T> underlying) {
       if(!AgentConfiguration.instance().isDebugMode()) return underlying;
       return new TimedMatcher<>(agentName, type, transformer, underlying);
   }

    @Override
    public boolean matches(T target) {
        return AgentUtil.withTimeSpent(() -> underlyingMatcher.matches(target), (timeSpentInNanoseconds) -> {
            val key = type + "-" + transformer;
            accumulatedTimeByType.merge(key, new TypeMatcherMetrics(timeSpentInNanoseconds, 1), (acc, current) -> acc.merge(current.getTime()));
        });
    }

    private  void logMetrics() {
       accumulatedTimeByType.forEach((key, value) -> System.out.println(AnsiColor.ParseColors(":green,n: The time spent to match a type: " + key + " for agent: " + agentName + " with value:" + value)));
    }

    @Value
    private class TypeMatcherMetrics {
        long time;
        long count;

        TypeMatcherMetrics merge(long time) {
            return new TypeMatcherMetrics(this.time + time, this.count + 1);
        }

        @Override
        public String toString() {
            return "TypeMatcherMetrics{ time=" + MILLISECONDS.convert(time, NANOSECONDS) +" ms, count=" + count +'}';
        }
    }
}
