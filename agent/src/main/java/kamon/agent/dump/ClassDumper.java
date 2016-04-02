package kamon.agent.dump;

import kamon.agent.util.log.LazyLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static kamon.agent.util.AgentUtil.withTimeLogging;

public abstract class ClassDumper {

    private static final LazyLogger log = LazyLogger.create(ClassDumper.class);

    // directory where we would write .class files
    protected String dumpDir;

    // classes with name matching this pattern will be dumped
    protected Pattern classesRegex;

    protected ClassDumper(String dumpDirArg, String classesRegexArg) {
        dumpDir = dumpDirArg;
        classesRegex = Pattern.compile(classesRegexArg);
    }

    public void install(Instrumentation instrumentation) {
        withTimeLogging(() -> {

            installTransformer(instrumentation);

            log.info(() -> format("Add Transformer to retrieve bytecode of instrumented classes [dumpDir = %s, classes = %s]", dumpDir, classesRegex.pattern()));

            // by the time we are attached, the classes to be dumped may have been loaded already. So, check
            // for candidates in the loaded classes.
            Class[] classes = instrumentation.getAllLoadedClasses();
            List<Class> candidates = new ArrayList<>();
            for (Class c : classes) {
                if (this.isCandidate(c.getName())) {
                    candidates.add(c);
                }
            }
            try {
                // if we have matching candidates, then retransform those classes so that we
                // will get callback to transform.
                if (! candidates.isEmpty()) {
                    Class[] candidateClasses = candidates.toArray(new Class[0]);
                    instrumentation.retransformClasses(candidateClasses);
                }
            } catch (UnmodifiableClassException exp) {
                log.error(() -> "Error re-transforming classes", exp);
            }

        }, "Class Dumper installed in"); // TODO: log this in debug level
    }

    protected abstract void installTransformer(Instrumentation instrumentation);

    public abstract void addClassToDump(String className, byte[] classBytes);

    protected boolean isCandidate(String className) {
        // ignore array classes
        if (className.charAt(0) == '[') {
            return false;
        }

        // convert the class name to external name
        className = className.replace('/', '.');
        // check for name pattern match
        return classesRegex.matcher(className).matches();
    }

    synchronized protected void dumpClass(String className, byte[] classBuf) {
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

            final String message = format("Dump %s", fileName);
            log.debug(() -> message);
        } catch (Exception exc) {
            String message = "Error creating dump file for " + className;
            log.error(() -> message, exc);
        }
    }
}
