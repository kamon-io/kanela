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
 *
 * @credits to @peter-lawrey
 */


package kanela.agent.broker;

import lombok.Value;
import lombok.val;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
public class EventBroker {
    Map<Class, List<SubscriberInfo>> map = new LinkedHashMap<>();

    private static class Holder {
        private static final EventBroker Instance = new EventBroker();
    }

    public static EventBroker instance() {
        return Holder.Instance;
    }

    public int publish(Object o) {
        val subscriberInfos = map.get(o.getClass());
        if (subscriberInfos == null) return 0;
        int count = 0;
        for (SubscriberInfo subscriberInfo : subscriberInfos) {
            subscriberInfo.invoke(o);
            count++;
        }
        return count;
    }

    public void add(Object o) {
        for (Method method : o.getClass().getMethods()) {
            val parameterTypes = method.getParameterTypes();
            if (method.getAnnotation(Subscribe.class) == null || parameterTypes.length != 1) continue;
            val subscribeTo = parameterTypes[0];
            List<SubscriberInfo> subscribers = map.computeIfAbsent(subscribeTo, k -> new ArrayList<>());
            subscribers.add(new SubscriberInfo(method, o));
        }
    }

    public void remove(Object o) {
        for (List<SubscriberInfo> subscriberInfos : map.values()) {
            for (int i = subscriberInfos.size() - 1; i >= 0; i--)
                if (subscriberInfos.get(i).object == o)
                    subscriberInfos.remove(i);
        }
    }

    @Value
    static class SubscriberInfo {
        Method method;
        Object object;

        void invoke(Object o) {
            try {
                method.invoke(object, o);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }
}