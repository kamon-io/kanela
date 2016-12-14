package kamon.agent.api.instrumentation.listener.dumper;

import kamon.agent.KamonAgentConfig;
import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder.Listener;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static java.text.MessageFormat.format;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassDumperListener extends Listener.Adapter {

    static final LinkedHashMap<String,byte[]> classNameToBytes = new LinkedHashMap<>();
    final KamonAgentConfig.DumpConfig config;

    ClassDumperListener(KamonAgentConfig.DumpConfig config) {
        this.config = config;
        onShutdown(this.config);
    }

    public static ClassDumperListener instance(KamonAgentConfig.DumpConfig config) {
        return new ClassDumperListener(config);
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
        addClassToDump(typeDescription.getSimpleName(), dynamicType.getBytes());
    }

    private void onShutdown(KamonAgentConfig.DumpConfig config) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dumpClassesToDisk(classNameToBytes);
                if (config.getCreateJar()) {
                    JarCreator.createJar(config.getJarName(), ClassDumperListener.this.config.getDumpDir());
                }
            } catch (Exception exc) {
                LazyLogger.error(() -> "The class dumping on shutdown failed", exc);
            }
        }));
    }


    private void addClassToDump(String className, byte[] classBytes) {
        byte[] oldBytes = classNameToBytes.put(className, classBytes);
        if(oldBytes != null && !Arrays.equals(classBytes, oldBytes)) {
            LazyLogger.warn(() -> AnsiColor.ParseColors(format(":yellow,n:There exist two different classes with name {0}", className)));
        }
    }

    private synchronized void dumpClassesToDisk(LinkedHashMap<String, byte[]> classNameToBytes) {
        classNameToBytes.forEach(this::dumpClass);
    }

    synchronized private void dumpClass(String className, byte[] classBuf) {
        try {
            // create package directories if needed
            className = className.replace("/", File.separator);
            StringBuilder buf = new StringBuilder();
            String dumpDir = this.config.getDumpDir();
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
            LazyLogger.debug(() -> message);
        } catch (Exception exc) {
            String message = "Error creating dump file for " + className;
            LazyLogger.error(() -> message, exc);
        }
    }
}
