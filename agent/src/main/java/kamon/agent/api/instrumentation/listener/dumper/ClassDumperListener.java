package kamon.agent.api.instrumentation.listener.dumper;

import javaslang.control.Try;
import kamon.agent.KamonAgentConfig;
import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder.Listener;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.File;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassDumperListener extends Listener.Adapter {

    final KamonAgentConfig.DumpConfig config;
    final File dumpDir;
    final File jarFile;

    private ClassDumperListener(KamonAgentConfig.DumpConfig config){
        this.config = config;
        this.dumpDir = new File(config.getDumpDir());
        this.jarFile = new File(config.getDumpDir() + File.separator + config.getJarName() + ".jar");
    }

    public static ClassDumperListener instance(KamonAgentConfig.DumpConfig config) {
        return new ClassDumperListener(config);
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {
        addClassToDump(dynamicType);
    }

    private void addClassToDump(DynamicType dynamicType) {
        if(!dumpDir.exists()){
            Try.of(dumpDir::mkdirs).onFailure((cause) -> LazyLogger.error(() -> "Error creating directory...", cause));
        }

        if(config.getCreateJar()) {
            if(!jarFile.exists()) {
                Try.of(jarFile::createNewFile).onFailure((cause) -> LazyLogger.error(() -> "Error creating a new file...", cause));
                Try.of(() -> dynamicType.toJar(jarFile)).onFailure((cause) -> LazyLogger.error(() -> "Error trying to add transformed class to jar...", cause));
            }
            Try.of(() -> dynamicType.inject(jarFile)).onFailure((cause) -> LazyLogger.error(() -> "Error trying to add transformed class to jar...", cause));
        } else {
            Try.of(() -> dynamicType.saveIn(dumpDir)).onFailure((cause) -> LazyLogger.error(() -> "Error trying to save transformed class into directory...", cause));
        }
    }
}
