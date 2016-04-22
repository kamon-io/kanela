package kamon.agent.dump;

import kamon.agent.api.banner.AnsiColor;
import kamon.agent.util.log.LazyLogger;
import lombok.Getter;

import java.io.*;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.text.MessageFormat.format;

/**
 * @see 'http://stackoverflow.com/a/1281295/3392786'
 */
public class JarCreator {

    public static void createJar(String jarName, String inputPath) {
        try {
            final String jarPath = new PathString(jarName).withExtension(".jar").withBase(inputPath).getValue();
            final String sourcePath = new PathString(inputPath).withFinalSlash().withCorrectlySlash().getValue();

            LazyLogger.debug(() -> format("Creating JAR [jar path = '{0}', input path = '{1}']", jarPath, sourcePath));

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            JarOutputStream target = new JarOutputStream(new FileOutputStream(jarPath), manifest);
            add(new File(sourcePath), target, jarPath, sourcePath);
            target.close();

            LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Created Jar with dump classes on {0}", jarPath)));
        } catch (IOException exc) {
            LazyLogger.error(() -> AnsiColor.ParseColors(":red,n:Failed to create the jar file."), exc);
        }
    }

    private static void add(File source, JarOutputStream target, String jarPath, String sourcePath) throws IOException {
        if (source.isDirectory()) {
            addDirectoryEntry(source, target, jarPath, sourcePath);
        } else if (source.isFile() && !Paths.get(jarPath).equals(Paths.get(source.getPath()))) {
            addFileEntry(source, target, sourcePath);
        }
    }

    private static void addDirectoryEntry(File source, JarOutputStream target, String jarPath, String sourcePath) throws IOException {
        String rawSubdirPath = source.getPath();
        if (!rawSubdirPath.isEmpty()) {
            final String subdirPath = new PathString(rawSubdirPath).withCorrectlySlash().withFinalSlash().withoutBase(sourcePath).getValue();
            LazyLogger.debug(() -> format("jar source directory: {0}", subdirPath));

            JarEntry entry = new JarEntry(subdirPath);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            target.closeEntry();
        }
        for (File nestedFile: source.listFiles())
            add(nestedFile, target, jarPath, sourcePath);
    }

    private static void addFileEntry(File source, JarOutputStream target, String sourcePath) throws IOException {
        BufferedInputStream in = null;

        try {

            final String filePath = new PathString(source.getPath()).withCorrectlySlash().withoutBase(sourcePath).getValue();
            LazyLogger.debug(() -> format("jar source file: {0}", filePath));

            JarEntry entry = new JarEntry(filePath);
            entry.setTime(source.lastModified());

            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();

        } finally {
            if (in != null)
                in.close();
        }
    }

    private static class PathString {

        @Getter
        final String value;

        public PathString(String value) {
            this.value = value;
        }

        private PathString withFinalSlash() {
            return value.endsWith("/") || value.endsWith("\\") ? new PathString(value) : new PathString(value + "/");
        }

        private PathString withBase(String base) {
            return new PathString(new PathString(base).withFinalSlash().getValue() + this.value);
        }

        private PathString withoutBase(String base) {
            return new PathString(value.replaceFirst(base, ""));
        }

        private PathString withCorrectlySlash() {
            return new PathString(value.replace("\\", "/"));
        }

        private PathString withExtension(String extension) {
            return this.value.endsWith(extension) ? this : new PathString(this.value + ".jar");
        }

    }
}
