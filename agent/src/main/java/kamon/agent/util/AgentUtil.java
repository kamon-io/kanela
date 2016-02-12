package kamon.agent.util;

import java.io.*;

import static java.text.MessageFormat.format;
import static utils.AgentApiUtils.streamToByteArray;

public class AgentUtil {

    public static File load(String jarName) throws IOException {
        InputStream jarStream = ClassLoader.getSystemClassLoader().getResourceAsStream(format("{0}.jar", jarName));
        if (jarStream == null) {
            throw new FileNotFoundException(format("{0}.jar", jarName));
        }
        File file = File.createTempFile(jarName, ".jar");
        file.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(streamToByteArray(jarStream));
            return file;
        }
    }
}
