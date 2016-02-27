package kamon.agent.util;

import java.util.function.Consumer;

public class AgentUtil {

    public static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) {
        long startMillis = System.currentTimeMillis();
        thunk.run();
        timeSpent.accept(System.currentTimeMillis() - startMillis);
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
