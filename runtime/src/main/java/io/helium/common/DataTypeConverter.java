package io.helium.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * Simple Converter for supported DataTypes
 *
 * Created by Christoph Grotz on 29.05.14.
 */
public class DataTypeConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTypeConverter.class);


    public static Object convert(Buffer content) {
        return convert(content.toString());
    }

    public static Object convert(String content) {
        LOGGER.trace("Converting "+content);
        if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("true")) {
            return Boolean.parseBoolean(content);
        }

        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException e) {
            LOGGER.error("Could convert ("+content+") to Double",e);
        }

        try {
            return Float.parseFloat(content);
        } catch (NumberFormatException e) {
            LOGGER.error("Could convert ("+content+") to Float",e);
        }

        try {
            return Long.parseLong(content);
        } catch (NumberFormatException e) {
            LOGGER.error("Could convert ("+content+") to Long",e);
        }

        try {
            return new JsonObject(content);
        } catch (Exception e) {
            LOGGER.error("Could convert ("+content+") to JsonObject",e);
        }

        return content;
    }

    public static Object convert(byte[] array) {
        return convert(new String(array));
    }
}
