package app.kanela.cases.replacer.app.kanela.instrumentation;

import kanela.agent.libs.net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.util.concurrent.Callable;

public class MySuperInsterceptor {


//    @RuntimeType
//    public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
//        System.out.println("Intercepted!");
//        return zuper.call();
//    }

    @Advice.OnMethodEnter
    public static void greet() {
        System.out.println("intercepted!!!");
    }
}
