package kamon.agent.util;

import kamon.agent.util.log.LazyLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static kamon.agent.util.AgentUtil.withTimeLogging;

/**
 * The credit is for @sundararajan
 * @see 'https://blogs.oracle.com/sundararajan/entry/retrieving_class_files_from_a'
 */
public class ClassDumper {

    // directory where we would write .class files
    private static String dumpDir;

    // classes with name matching this pattern will be dumped
    private static Pattern classesRegex;

    public static void process(Instrumentation inst, String dumpDirArg, String classesRegexArg) {
        withTimeLogging(() -> {

            dumpDir = dumpDirArg;
            classesRegex = Pattern.compile(classesRegexArg);

            LazyLogger.info(ClassDumper.class,
                    () -> String.format("Add Transformer to retrieve bytecode of instrumented classes [dumpDir = %s, classes = %s]", dumpDir, classesRegexArg));

            inst.addTransformer(new ClassDumper.ClassDumperTransformer(), true);

            // by the time we are attached, the classes to be dumped may have been loaded already. So, check
            // for candidates in the loaded classes.
            Class[] classes = inst.getAllLoadedClasses();
            List<Class> candidates = new ArrayList<>();
            for (Class c : classes) {
                if (isCandidate(c.getName())) {
                    candidates.add(c);
                }
            }
            try {
                // if we have matching candidates, then retransform those classes so that we
                // will get callback to transform.
                if (! candidates.isEmpty()) {
                    Class[] candidateClasses = candidates.toArray(new Class[0]);
                    inst.retransformClasses(candidateClasses);
                }
            } catch (UnmodifiableClassException exp) {
                LazyLogger.error(ClassDumper.class, () -> "Error re-transforming classes", exp);
            }

        }, "Class Dumper complete in");
    }

    private static boolean isCandidate(String className) {
        // ignore array classes
        if (className.charAt(0) == '[') {
            return false;
        }

        // convert the class name to external name
        className = className.replace('/', '.');
        // check for name pattern match
        return classesRegex.matcher(className).matches();
    }

    private static void dumpClass(String className, byte[] classBuf) {
        try {
            // create package directories if needed
            className = className.replace("/", File.separator);
            StringBuilder buf = new StringBuilder();
            buf.append(dumpDir);
            buf.append(File.separatorChar);
            int index = className.lastIndexOf(File.separatorChar);
            if (index != -1) {
                buf.append(className.substring(0, index));
            }
            String dir = buf.toString();
            new File(dir).mkdirs();

            // write .class file
            String fileName = dumpDir + File.separator + className + ".class";
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(classBuf);
            fos.close();

            final String message = String.format("Dump %s", fileName);
            LazyLogger.info(ClassDumper.class, () -> message);
        } catch (Exception exp) {
            String message = "Error creating dump file for " + className;
            LazyLogger.error(ClassDumper.class, () -> message, exp);
        }
    }

    private static class ClassDumperTransformer implements ClassFileTransformer {

        public byte[] transform(ClassLoader loader, String className,
                                Class redefinedClass, ProtectionDomain protDomain,
                                byte[] classBytes) {
            // check and dump .class file
            if (isCandidate(className)) {
                dumpClass(className, classBytes);
            }

            // we don't mess with .class file, just return null
            return null;
        }
    }
}
