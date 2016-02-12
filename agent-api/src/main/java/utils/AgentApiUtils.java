package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cristian on 12/02/16.
 */
public class AgentApiUtils {

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
}
