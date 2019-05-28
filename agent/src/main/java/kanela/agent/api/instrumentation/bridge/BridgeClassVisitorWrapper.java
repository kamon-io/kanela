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

import kanela.agent.util.asm.ClassWriterFlags;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.pool.TypePool;

@Value(staticConstructor = "of")
@EqualsAndHashCode(callSuper = false)
public class BridgeClassVisitorWrapper extends AsmVisitorWrapper.AbstractBase {

    BridgeDescription bridge;
    TypeDescription typeDescription;
    ClassLoader classLoader;

    @Override
    public int mergeWriter(int flags) {
        return flags | ClassWriterFlags.resolve(typeDescription, classLoader);
    }

    @Override
    public int mergeReader(int flags) {
        return flags | ClassReader.EXPAND_FRAMES;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {

        return  BridgeClassVisitor.from(bridge, instrumentedType.getInternalName(), classVisitor);
    }
}

