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

package kanela.agent.api.instrumentation.bridge;

import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class BridgeDescription {
    Class<?> bridgeInterface;

    public static BridgeDescription of(Class<?> clazz) {
        return new BridgeDescription(clazz);
    }

    public AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader, module, domain) -> builder
            .implement(new TypeDescription.ForLoadedType(this.bridgeInterface))
            .visit(BridgeClassVisitorWrapper.of(this, typeDescription, classLoader));
    }

    public Set<Method> getMethods() {
        return Arrays.stream(bridgeInterface.getDeclaredMethods())
            .filter(method -> (method.isAnnotationPresent(Bridge.class) || method.isAnnotationPresent(FieldBridge.class)))
            .collect(Collectors.toSet());
    }
}