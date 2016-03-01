package kamon.agent.util;

import kamon.agent.KamonAgent;
import kamon.agent.util.log.LazyLogger;

import java.util.function.Consumer;

import static java.text.MessageFormat.format;

public class AgentUtil {

    public static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) {
        long startMillis = System.currentTimeMillis();
        thunk.run();
        timeSpent.accept(System.currentTimeMillis() - startMillis);
    }

    public static void withTimeLogging(final Runnable thunk, String message) {
        withTimeSpent(thunk::run, (timeSpent) -> LazyLogger.info(KamonAgent.class, () -> format("{0} {1} ms",message, timeSpent)));
    }

//    private void printTransformedClass(byte[] b) {
//        ClassReader cr = new ClassReader(b);
//        cr.accept(new TraceClassVisitor(new PrintWriter(System.out)),
//                TraceClassVisitor.getDefaultAttributes(),
//                0);
//    }

//    public void printTransformedClassToFile(byte[] b, File f) {
//        try {
//            OutputStream out = new FileOutputStream(f);
//            out.write(b);
//            out.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
