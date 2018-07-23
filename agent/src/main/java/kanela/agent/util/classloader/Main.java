package kanela.agent.util.classloader;

import io.vavr.Tuple;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import kanela.agent.api.instrumentation.KanelaInstrumentation;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {
    public static void main(String... args) throws IOException {
        AnalyzedClass analyzedClass = AnalyzedClass.from(KanelaInstrumentation.class.getName(), KanelaInstrumentation.class.getClassLoader());
        System.out.println(analyzedClass.containsFields("instrumentationDescriptions", "instrumentationDescriptions"));
        System.out.println(analyzedClass.containsMethod("buildTransformations"));
        System.out.println(analyzedClass.containsMethod("buildTransformations", "kanela.agent.api.instrumentation.InstrumentationDescription"));
        System.out.println(analyzedClass.containsMethod("buildTransformations", "kanela.agent.api.instrumentation.InstrumentationDescription", "kanela.agent.util.conf.KanelaConfiguration$ModuleConfiguration", "java.lang.instrument.Instrumentation"));
    }

    @Value
    public static class AnalyzedClass {
        private String target;
        private Set<String> fields;
        private Map<String, Set<String>> methodsWithArguments;

        @SneakyThrows
        public static AnalyzedClass from(String target, ClassLoader loader)  {
             val resourceName = target.replace('.', '/') + ".class";
             try(InputStream in = loader.getResourceAsStream(resourceName)) {
                 val classNode = convertToClassNode(in);
                 return new AnalyzedClass(target, extractFields(classNode), extractMethods(classNode));
             }
        }

        public Boolean containsFields(String... fields) {
            boolean b = this.fields.containsAll(Arrays.asList(fields));
            System.out.println("containsFields " + b);
            return b;
        }

        public Boolean containsMethod(String methodName, String... parameters) {
            if(methodsWithArguments.containsKey(methodName)) {
                val parameterSet = methodsWithArguments.get(methodName);
                if(parameters.length > 0) return Arrays.asList(parameters).containsAll(parameterSet);
                return true;
            }
            return false;
        }

        public Predicate<Boolean> buildPredicate(String target, Set<String> fields, Map<String, Set<String>> methods) {
            java.util.List<Predicate<Boolean>> allPredicates = Arrays.asList(
                    p -> target.equalsIgnoreCase(this.target),
                    p -> containsFields(fields.toArray(new String[0])),
                    p -> containsMethodWithParameters(methods)
            );
            return allPredicates.stream().reduce(p -> true, Predicate::and);
        }

        public Boolean containsMethodWithParameters(Map<String, Set<String>> methods) {
            if (methods.isEmpty()) return true;
            return methods.entrySet()
                   .stream()
                   .map((entry) -> containsMethod(entry.getKey(), entry.getValue().toArray(new String[0])))
                   .collect(Collectors.toSet())
                   .contains(true);
        }

        private static Set<String> extractFields(ClassNode classNode) {
            return List.ofAll(classNode.fields)
                    .map(fieldNode -> fieldNode.name)
                    .toJavaSet();
        }

        private static Map<String, Set<String>> extractMethods(ClassNode classNode) {
            return List.ofAll(classNode.methods)
                    .filter(methodNode -> (methodNode.access & Opcodes.ACC_SYNTHETIC) == 0)
                    .toJavaMap(methodNode -> Tuple.of(methodNode.name, Array.of(Type.getArgumentTypes(methodNode.desc)).map(AnalyzedClass::getType).toJavaSet()));
        }

        private static String getType(Type methodDescription) {
            return methodDescription
                    .getInternalName()
                    .replace('/', '.');
        }

        private static ClassNode convertToClassNode(InputStream classBytes) throws IOException {
            val result = new ClassNode(Opcodes.ASM6);
            val reader =  new ClassReader(classBytes);
            reader.accept(result, ClassReader.SKIP_FRAMES);
            return result;
        }
    }
}
