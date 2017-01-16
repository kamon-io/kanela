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

package kamon.agent.builder;

import kamon.agent.api.instrumentation.TypeTransformation;
import kamon.agent.util.conf.AgentConfiguration;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;

@Value
public class Agents {
    AgentConfiguration config;
    KamonAgentBuilder builder;
    AgentConfiguration.AgentModuleDescription moduleDescription;
    Instrumentation instrumentation;

    private Agents(AgentConfiguration config, AgentConfiguration.AgentModuleDescription moduleDescription, Instrumentation instrumentation) {
        this.config = config;
        this.moduleDescription = moduleDescription;
        this.builder = DefaultAgentBuilder.instance(moduleDescription.getName());
        this.instrumentation = instrumentation;
    }

    public static Agents from(AgentConfiguration config, AgentConfiguration.AgentModuleDescription moduleDescription, Instrumentation instrumentation) {
        return new Agents(config, moduleDescription, instrumentation);
    }

    public KamonAgentFileTransformer install() {
        val agentBuilder = builder.build(config, moduleDescription, instrumentation);
        val classFileTransformer = agentBuilder.installOn(instrumentation);
        return KamonAgentFileTransformer.from(agentBuilder, classFileTransformer, moduleDescription.isStoppable());
    }

    public Agents addTypeTransformation(TypeTransformation typeTransformation) {
        builder.addTypeTransformation(typeTransformation);
        return this;
    }
}