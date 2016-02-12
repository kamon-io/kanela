package kamon.agent.util;

import java.io.*;

import static java.text.MessageFormat.format;

public class Util {

    /**
    * Method converts {@link InputStream} Object into byte[] array.
    *
    * @param stream the {@link InputStream} Object.
    * @return the byte[] array representation of received {@link InputStream} Object.
    * @throws IOException if an error occurs.
    */
    public static byte[] streamToByteArray(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int line = 0;
        // read bytes from stream, and store them in buffer
        while ((line = stream.read(buffer)) != -1) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, line);
        }
        stream.close();
        os.flush();
        os.close();
        return os.toByteArray();
    }

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
