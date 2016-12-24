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

package kamon.agent.api.instrumentation.listener;

import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import utils.AnsiColor;

import static java.text.MessageFormat.format;

@Value(staticConstructor = "instance")
@EqualsAndHashCode(callSuper = false)
public class DebugInstrumentationListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
        LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Transformed => {0} and loaded from {1}", typeDescription, classLoader)));
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        LazyLogger.debug(() -> AnsiColor.ParseColors(format(":red,n:Ignored => {0} and loaded from {1}", typeDescription, classLoader)));
    }
}
