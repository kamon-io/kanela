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

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.pool.TypePool;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassFileVersionValidatorWrapper extends AsmVisitorWrapper.AbstractBase {

    public static final ClassFileVersionValidatorWrapper Instance = new ClassFileVersionValidatorWrapper();

    @Override
    public ClassVisitor wrap(TypeDescription typeDescription,
                             ClassVisitor classVisitor,
                             Implementation.Context context,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fieldList, MethodList<?> methodList,
                             int writerFlags,
                             int readerFlags) {

        return ClassFileVersionValidatorClassVisitor.from(classVisitor);
    }
}
