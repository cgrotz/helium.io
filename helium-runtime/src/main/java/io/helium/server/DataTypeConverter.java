package io.helium.server;

import io.helium.json.HashMapBackedNode;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class DataTypeConverter {

    public static Object convert(String content) {
        if ( content.equalsIgnoreCase("true") || content.equalsIgnoreCase("true") ) {
            return Boolean.parseBoolean(content);
        }

        try {
            return Double.parseDouble(content);
        }
        catch(NumberFormatException e) {

        }

        try {
            return Float.parseFloat(content);
        }
        catch(NumberFormatException e) {

        }

        try {
            return Long.parseLong(content);
        }
        catch(NumberFormatException e) {

        }

        try {
            return new HashMapBackedNode(content);
        }
        catch(Exception e) {

        }

        return content;
    }

    public static Object convert(byte[] array) {
        return convert(new String(array));
    }
}
