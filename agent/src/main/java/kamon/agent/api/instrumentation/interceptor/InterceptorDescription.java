package kamon.agent.api.instrumentation.interceptor;
import kamon.agent.api.instrumentation.after;
import kamon.agent.api.instrumentation.before;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.jar.asm.commons.Method;
import net.bytebuddy.matcher.ElementMatcher;
import java.util.Arrays;

public class InterceptorDescription {

    private final ElementMatcher<? super MethodDescription.InDefinedShape> methodMatcher;
    private final Class<?> interceptorClass;
    private final Interception.InterceptionBuilder builder = Interception.builder();

    public InterceptorDescription(ElementMatcher<? super MethodDescription.InDefinedShape> methodMatcher, Class<?> interceptorClass) {
        this.interceptorClass = interceptorClass;
        this.methodMatcher = methodMatcher;
    }

    public ElementMatcher<? super MethodDescription.InDefinedShape> getMethodMatcher() {
        return methodMatcher;
    }

    public Interception describe() {
        builder.interceptorType(Type.getType(interceptorClass));

        Arrays.asList(interceptorClass.getMethods()).forEach(method ->{
            if(method.isAnnotationPresent(before.class)){
                registerBefore(interceptorClass, method);
            }

            if(method.isAnnotationPresent(after.class)){
                registerAfter(interceptorClass, method);
            }
        });
        return builder.build();
    }

    private void registerBefore(Class<?> interceptorClass, java.lang.reflect.Method method) {
        Method methodDescriptor = Method.getMethod(method);
        builder.beforeMethod(methodDescriptor);
        if(methodDescriptor.getReturnType().getSort() != Type.VOID){
            builder.travelerType(methodDescriptor.getReturnType());
        }
    }

    private void registerAfter(Class<?> interceptorClass, java.lang.reflect.Method method) {
        Method methodDescriptor = Method.getMethod(method);
        assertState(methodDescriptor.getReturnType().getSort() == Type.VOID, "@after method must return void");
        builder.afterMethod(methodDescriptor);
    }

    private void assertState(boolean condition, String message) {
        if(!condition) throw new RuntimeException(message);
    }
}
