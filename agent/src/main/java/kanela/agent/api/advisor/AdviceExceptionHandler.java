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

package kanela.agent.api.advisor;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

import static net.bytebuddy.jar.asm.Opcodes.GOTO;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;

@Value
@EqualsAndHashCode(callSuper = false)
public class AdviceExceptionHandler extends Advice.ExceptionHandler.Simple {

    private static final AdviceExceptionHandler Instance = new AdviceExceptionHandler(getStackManipulation());

    private AdviceExceptionHandler(StackManipulation getStackForErrorManipulation) {
        super(getStackForErrorManipulation);
    }

    public static AdviceExceptionHandler instance() {
        return Instance;
    }

    /**
     * Produces the following bytecode:
     *
     * <pre>
     * } catch(Throwable throwable) {
     *   try{
     *     kanela.agent.bootstrap.log.LoggerHandler.error("An error occurred while trying to apply an advisor", throwable)
     *   } catch(Throwable ignored) {}
     * }
     * </pre>
     */
    private static StackManipulation getStackManipulation() {
        return new StackManipulation() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                val startTryBlock = new Label();
                val endTryBlock = new Label();
                val startCatchBlock = new Label();
                val endCatchBlock = new Label();

                // Initialization try-catch block
                methodVisitor.visitTryCatchBlock(startTryBlock, endTryBlock, startCatchBlock, "java/lang/Throwable");
                // starting try block
                methodVisitor.visitLabel(startTryBlock);
                //message
                methodVisitor.visitLdcInsn("An error occurred while trying to apply an advisor");
                // logger, message, throwable => throwable, message, logger
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "kanela/agent/bootstrap/log/LoggerHandler", "error", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
                // ending try block
                methodVisitor.visitLabel(endTryBlock);
                methodVisitor.visitJumpInsn(GOTO, endCatchBlock);
                // starting catch block
                methodVisitor.visitLabel(startCatchBlock);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
                methodVisitor.visitInsn(Opcodes.POP);
                // ending catch block
                methodVisitor.visitLabel(endCatchBlock);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                // pops a Throwable off the stack
                return new StackManipulation.Size(-1, 3);
            }
        };
    }
}
