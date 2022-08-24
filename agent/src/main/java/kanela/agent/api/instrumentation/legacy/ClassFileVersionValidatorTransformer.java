/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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

package kanela.agent.api.instrumentation.legacy;

import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;

@Value
public class ClassFileVersionValidatorTransformer {

    public static final AgentBuilder.Transformer Instance = makeTransformer();

    private static AgentBuilder.Transformer makeTransformer() {
        return (builder, typeDescription, classLoader, module, domain) ->
                builder.visit(ClassFileVersionValidatorWrapper.Instance);
    }
}