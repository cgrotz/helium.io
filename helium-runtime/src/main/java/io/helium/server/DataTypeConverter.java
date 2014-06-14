package io.helium.server;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class DataTypeConverter {

    public static Object convert(Buffer content) {
        return convert(content.toString());
    }

    public static Object convert(String content) {
        if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("true")) {
            return Boolean.parseBoolean(content);
        }

        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException e) {

        }

        try {
            return Float.parseFloat(content);
        } catch (NumberFormatException e) {

        }

        try {
            return Long.parseLong(content);
        } catch (NumberFormatException e) {

        }

        try {
            return new JsonObject(content);
        } catch (Exception e) {

        }

        return content;
    }

    public static Object convert(byte[] array) {
        return convert(new String(array));
    }
}
