/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.json;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.helium.common.Path;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * A Node is an unordered collection of name/value pairs. Its external form is a string wrapped in
 * curly braces with colons between the names and values, and commas between the values and names.
 * The internal form is an object having <code>get</code> and <code>opt</code> methods for accessing
 * the values by name, and <code>put</code> methods for adding or replacing values by name. The
 * values can be any of these types: <code>Boolean</code>, <code>JSONArray</code>, <code>Node</code>
 * , <code>Number</code>, <code>String</code>, or the <code>Node.NULL</code> object. A Node
 * constructor can be used to convert an external form JSON text into an internal form whose values
 * can be retrieved with the <code>get</code> and <code>opt</code> methods, or to convert values
 * into a JSON text using the <code>put</code> and <code>toString</code> methods. A <code>get</code>
 * method returns a value if one can be found, and throws an exception if one cannot be found. An
 * <code>opt</code> method returns a default value instead of throwing an exception, and so is
 * useful for obtaining optional values.
 * <p/>
 * The generic <code>get()</code> and <code>opt()</code> methods return an object, which you can
 * cast or query for type. There are also typed <code>get</code> and <code>opt</code> methods that
 * do type checking and type coercion for you. The opt methods differ from the get methods in that
 * they do not throw. Instead, they return a specified value, such as null.
 * <p/>
 * The <code>put</code> methods add or replace values in an object. For example,
 * <p/>
 * <pre>
 * myString = new Node().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
 * <p/>
 * produces the string <code>{"JSON": "Hello, World"}</code>.
 * <p/>
 * The texts produced by the <code>toString</code> methods strictly conform to the JSON syntax
 * rules. The constructors are more forgiving in the texts they will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the closing brace.
 * </li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote or single quote,
 * and if they do not contain leading or trailing spaces, and if they do not contain any of these
 * characters: <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and if they are
 * not the reserved words <code>true</code>, <code>false</code>, or <code>null</code>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2013-06-17
 */
public class Node {
    /**
     * It is sometimes more convenient and less ambiguous to have a <code>NULL</code> object than to
     * use Java's <code>null</code> value. <code>Node.NULL.equals(null)</code> returns
     * <code>true</code>. <code>Node.NULL.toString()</code> returns <code>"null"</code>.
     */
    public static final Object NULL = new Null();
    /**
     * The map where the Node's properties are kept.
     */
    private final Map<String, Object> map;

    private final List<String> key_order = Lists.newArrayList();

    private HeliumEventBuilder builder;

    /**
     * Construct an empty Node.
     */
    public Node() {
        this.map = Maps.newHashMapWithExpectedSize(10000);
    }

    /**
     * Construct a Node from a JSONTokener.
     *
     * @param x A JSONTokener object containing the source string.
     * @throws RuntimeException If there is a syntax error in the source string or a duplicated key.
     */
    public Node(JSONTokener x) {
        this();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A Node text must begin with '{'");
        }
        for (; ; ) {
            c = x.nextClean();
            switch (c) {
                case 0:
                    throw x.syntaxError("A Node text must end with '}'");
                case '}':
                    return;
                default:
                    x.back();
                    key = x.nextValue().toString();
            }

            // The key is followed by ':'.

            c = x.nextClean();
            if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            this.putOnce(key, x.nextValue());

            // Pairs are separated by ','.

            switch (x.nextClean()) {
                case ';':
                case ',':
                    if (x.nextClean() == '}') {
                        return;
                    }
                    x.back();
                    break;
                case '}':
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    /**
     * Construct a Node from a source JSON text string. This is the most commonly used Node
     * constructor.
     *
     * @param source A string beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *               with <code>}</code> &nbsp;<small>(right brace)</small>.
     * @throws RuntimeException If there is a syntax error in the source string or a duplicated key.
     */
    public Node(String source) {
        this(new JSONTokener(source));
    }

    public Node(HeliumEventBuilder heliumEventBuilder) {
        this();
        this.builder = heliumEventBuilder;
    }

    /**
     * Get an array of field names from a Node.
     *
     * @return An array of field names, or null if there are no names.
     */
    public static String[] getNames(Node jo) {
        int length = jo.length();
        if (length == 0) {
            return null;
        }
        Iterator<String> iterator = jo.keyIterator();
        String[] names = new String[length];
        int i = 0;
        while (iterator.hasNext()) {
            names[i] = iterator.next();
            i += 1;
        }
        return names;
    }

    /**
     * Produce a string from a Number.
     *
     * @param number A Number
     * @return A String.
     * @throws RuntimeException If n is a non-finite number.
     */
    public static String numberToString(Number number) {
        if (number == null) {
            throw new RuntimeException("Null pointer");
        }
        testValidity(number);

        // Shave off trailing zeros and decimal point, if possible.

        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the right places. A backslash
     * will be inserted within </, producing <\/, allowing JSON text to be delivered in HTML. In JSON
     * text, a string cannot contain a control character or an unescaped quote or backslash.
     *
     * @param string A String
     * @return A String correctly formatted for insertion in a JSON text.
     */
    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException ignored) {
                // will never happen - we are writing to a string writer
                return "";
            }
        }
    }

    public static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        w.write("\\u");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
        return w;
    }

    /**
     * Try to convert a string into a number, boolean, or null. If the string can't be converted,
     * return the string.
     *
     * @param string A String.
     * @return A simple JSON value.
     */
    public static Object stringToValue(String string) {
        Double d;
        if (string.equals("")) {
            return string;
        }
        if (string.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (string.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (string.equalsIgnoreCase("null")) {
            return Node.NULL;
        }

		/*
         * If it might be a number, try converting it. If a number cannot be produced, then the value
		 * will just be a string.
		 */

        char b = string.charAt(0);
        if ((b >= '0' && b <= '9') || b == '-') {
            try {
                if (string.indexOf('.') > -1 || string.indexOf('e') > -1 || string.indexOf('E') > -1) {
                    d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = new Long(string);
                    if (string.equals(myLong.toString())) {
                        if (myLong.longValue() == myLong.intValue()) {
                            return new Integer(myLong.intValue());
                        } else {
                            return myLong;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return string;
    }

    /**
     * Throw an exception if the object is a NaN or infinite number.
     *
     * @param o The object to test.
     * @throws RuntimeException If o is a non-finite number.
     */
    public static void testValidity(Object o) {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                    throw new RuntimeException("JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                    throw new RuntimeException("JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    /**
     * Make a JSON text of an Object value. If the object has an value.toJSONString() method, then
     * that method will be used to produce the JSON text. The method is required to produce a strictly
     * conforming text. If the object does not contain a toJSONString method (which is the most common
     * case), then a text will be produced by other means. If the value is an array or Collection,
     * then a JSONArray will be made from it and its toJSONString method will be called. If the value
     * is a MAP, then a Node will be made from it and its toJSONString method will be called.
     * Otherwise, the value's toString method will be called, and the result will be quoted.
     * <p/>
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable representation of the object, beginning with
     * <code>{</code>&nbsp;<small>(left brace)</small> and ending with <code>}</code>
     * &nbsp;<small>(right brace)</small>.
     * @throws RuntimeException If the value is or contains an invalid number.
     */
    public static String valueToString(Object value) {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        return quote(value.toString());
    }

    static final Writer writeValue(Writer writer, Object value, int indentFactor, int indent)
            throws RuntimeException, IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof Node) {
            ((Node) value).write(writer, indentFactor, indent);
        } else if (value instanceof Number) {
            writer.write(numberToString((Number) value));
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    public HeliumEventBuilder complete() {
        return this.builder;
    }

    /**
     * Get the value object associated with a key.
     *
     * @param key A key string.
     * @return The object associated with the key.
     * @throws RuntimeException if the key is not found.
     */
    public Object get(String key) {
        if (key == null) {
            throw new RuntimeException("Null key.");
        }
        Object object = this.opt(key);
        if (object == null) {
            throw new RuntimeException("Node[" + quote(key) + "] not found.");
        }
        return object;
    }

    /**
     * Get the boolean value associated with a key.
     *
     * @param key A key string.
     * @return The truth.
     * @throws RuntimeException if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) {
        Object object = this.get(key);
        if (object.equals(Boolean.FALSE)
                || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
                || (object instanceof String && ((String) object).equalsIgnoreCase("true"))) {
            return true;
        }
        throw new RuntimeException("Node[" + quote(key) + "] is not a Boolean.");
    }

    /**
     * Get the double value associated with a key.
     *
     * @param key A key string.
     * @return The numeric value.
     * @throws RuntimeException if the key is not found or if the value is not a Number object and cannot be
     *                          converted to a number.
     */
    public double getDouble(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).doubleValue() : Double
                    .parseDouble((String) object);
        } catch (Exception e) {
            throw new RuntimeException("Node[" + quote(key) + "] is not a number.");
        }
    }

    /**
     * Get the int value associated with a key.
     *
     * @param key A key string.
     * @return The integer value.
     * @throws RuntimeException if the key is not found or if the value cannot be converted to an integer.
     */
    public int getInt(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).intValue() : Integer
                    .parseInt((String) object);
        } catch (Exception e) {
            throw new RuntimeException("Node[" + quote(key) + "] is not an int.");
        }
    }

    /**
     * Get the Node value associated with a key.
     *
     * @param key A key string.
     * @return A Node which is the value.
     * @throws RuntimeException if the key is not found or if the value is not a Node.
     */
    public Node getNode(String key) {
        Object object = this.get(key);
        if (object instanceof Node) {
            return (Node) object;
        }
        throw new RuntimeException("Node[" + quote(key) + "] is not a Node. " + toString(2));
    }

    /**
     * Get the long value associated with a key.
     *
     * @param key A key string.
     * @return The long value.
     * @throws RuntimeException if the key is not found or if the value cannot be converted to a long.
     */
    public long getLong(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).longValue() : Long
                    .parseLong((String) object);
        } catch (Exception e) {
            throw new RuntimeException("Node[" + quote(key) + "] is not a long. " + toString(2));
        }
    }

    /**
     * Get the string associated with a key.
     *
     * @param key A key string.
     * @return A string which is the value.
     * @throws RuntimeException if there is no string value for the key.
     */
    public String getString(String key) {
        Object object = this.get(key);
        if (object instanceof String) {
            return (String) object;
        }
        throw new RuntimeException("Node[" + quote(key) + "] not a string. " + toString(2));
    }

    /**
     * Determine if the Node contains a specific key.
     *
     * @param key A key string.
     * @return true if the key exists in the Node.
     */
    public boolean has(String key) {
        return this.map.containsKey(key) && (this.map.get(key) != null && this.map.get(key) != NULL);
    }

    /**
     * Increment a property of a Node. If there is no such property, create one with a value of 1. If
     * there is such a property, and if it is an Integer, Long, Double, or Float, then add one to it.
     *
     * @param key A key string.
     * @return this.
     * @throws RuntimeException If there is already a property with this name that is not an Integer, Long, Double,
     *                          or Float.
     */
    public Node increment(String key) {
        Object value = this.opt(key);
        if (value == null) {
            this.put(key, 1);
        } else if (value instanceof Integer) {
            this.put(key, ((Integer) value).intValue() + 1);
        } else if (value instanceof Long) {
            this.put(key, ((Long) value).longValue() + 1);
        } else if (value instanceof Double) {
            this.put(key, ((Double) value).doubleValue() + 1);
        } else if (value instanceof Float) {
            this.put(key, ((Float) value).floatValue() + 1);
        } else {
            throw new RuntimeException("Unable to increment [" + quote(key) + "]. " + toString(2));
        }
        return this;
    }

    /**
     * Determine if the value associated with the key is null or if there is no value.
     *
     * @param key A key string.
     * @return true if there is no value associated with the key or if the value is the Node.NULL
     * object.
     */
    public boolean isNull(String key) {
        return Node.NULL.equals(this.opt(key));
    }

    /**
     * Get an enumeration of the keys of the Node.
     *
     * @return An iterator of the keys.
     */
    public Iterator<String> keyIterator() {
        return (Lists.newArrayList(this.key_order)).iterator();
    }

    /**
     * Get a set of keys of the Node.
     *
     * @return A keySet.
     */
    public List<String> keys() {
        return Lists.newArrayList(this.key_order);
    }

    /**
     * Get the number of keys stored in the Node.
     *
     * @return The number of keys in the Node.
     */
    public int length() {
        return this.map.size();
    }

    /**
     * Get an optional value associated with a key.
     *
     * @param key A key string.
     * @return An object which is the value, or null if there is no value.
     */
    public Object opt(String key) {
        return key == null ? null : this.map.get(key);
    }

    /**
     * Get an optional boolean associated with a key. It returns false if there is no such key, or if
     * the value is not Boolean.TRUE or the String "true".
     *
     * @param key A key string.
     * @return The truth.
     */
    public boolean optBoolean(String key) {
        return this.optBoolean(key, false);
    }

    /**
     * Get an optional boolean associated with a key. It returns the defaultValue if there is no such
     * key, or if it is not a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return The truth.
     */
    public boolean optBoolean(String key, boolean defaultValue) {
        try {
            return this.getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional double associated with a key, or NaN if there is no such key or if its value is
     * not a number. If the value is a string, an attempt will be made to evaluate it as a number.
     *
     * @param key A string which is the key.
     * @return An object which is the value.
     */
    public double optDouble(String key) {
        return this.optDouble(key, Double.NaN);
    }

    /**
     * Get an optional double associated with a key, or the defaultValue if there is no such key or if
     * its value is not a number. If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public double optDouble(String key, double defaultValue) {
        try {
            return this.getDouble(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional int value associated with a key, or zero if there is no such key or if the
     * value is not a number. If the value is a string, an attempt will be made to evaluate it as a
     * number.
     *
     * @param key A key string.
     * @return An object which is the value.
     */
    public int optInt(String key) {
        return this.optInt(key, 0);
    }

    /**
     * Get an optional int value associated with a key, or the default if there is no such key or if
     * the value is not a number. If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public int optInt(String key, int defaultValue) {
        try {
            return this.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional Node associated with a key. It returns null if there is no such key, or if its
     * value is not a Node.
     *
     * @param key A key string.
     * @return A Node which is the value.
     */
    public Node optNode(String key) {
        Object object = this.opt(key);
        return object instanceof Node ? (Node) object : null;
    }

    /**
     * Get an optional long value associated with a key, or zero if there is no such key or if the
     * value is not a number. If the value is a string, an attempt will be made to evaluate it as a
     * number.
     *
     * @param key A key string.
     * @return An object which is the value.
     */
    public long optLong(String key) {
        return this.optLong(key, 0);
    }

    /**
     * Get an optional long value associated with a key, or the default if there is no such key or if
     * the value is not a number. If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public long optLong(String key, long defaultValue) {
        try {
            return this.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional string associated with a key. It returns an empty string if there is no such
     * key. If the value is not a string and is not null, then it is converted to a string.
     *
     * @param key A key string.
     * @return A string which is the value.
     */
    public String optString(String key) {
        return this.optString(key, "");
    }

    /**
     * Get an optional string associated with a key. It returns the defaultValue if there is no such
     * key.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return A string which is the value.
     */
    public String optString(String key, String defaultValue) {
        Object object = this.opt(key);
        return NULL.equals(object) ? defaultValue : object.toString();
    }

    /**
     * Put a key/boolean pair in the Node.
     *
     * @param key   A key string.
     * @param value A boolean which is the value.
     * @return this.
     * @throws RuntimeException If the key is null.
     */
    public Node put(String key, boolean value) {
        this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    /**
     * Put a key/double pair in the Node.
     *
     * @param key   A key string.
     * @param value A double which is the value.
     * @return this.
     * @throws RuntimeException If the key is null or if the number is invalid.
     */
    public Node put(String key, double value) {
        this.put(key, new Double(value));
        return this;
    }

    /**
     * Put a key/int pair in the Node.
     *
     * @param key   A key string.
     * @param value An int which is the value.
     * @return this.
     * @throws RuntimeException If the key is null.
     */
    public Node put(String key, int value) {
        this.put(key, new Integer(value));
        return this;
    }

    /**
     * Put a key/long pair in the Node.
     *
     * @param key   A key string.
     * @param value A long which is the value.
     * @return this.
     * @throws RuntimeException If the key is null.
     */
    public Node put(String key, long value) {
        this.put(key, new Long(value));
        return this;
    }

    /**
     * Put a key/value pair in the Node. If the value is null, then the key will be removed from the
     * Node if it is present.
     *
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these types: Boolean, Double,
     *              Integer, JSONArray, Node, Long, String, or the Node.NULL object.
     * @return this.
     * @throws RuntimeException If the value is non-finite number or if the key is null.
     */
    public Node put(String key, Object value) {
        return putWithIndex(key, value, -1);
    }

    public Node putWithIndex(String key, Object value, int index) {
        if (key == null) {
            throw new NullPointerException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.map.put(key, value);
            if (key_order.contains(key)) {
                key_order.remove(key);
            }
            if (index < 0 || index > key_order.size()) {
                this.key_order.add(key);
            } else {
                this.key_order.add(index, key);
            }
        } else {
            this.remove(key);
        }
        return this;
    }

    /**
     * Put a key/value pair in the Node, but only if the key and the value are both non-null, and only
     * if there is not already a member with that name.
     *
     * @param key
     * @param value
     * @return his.
     * @throws RuntimeException if the key is a duplicate
     */
    public Node putOnce(String key, Object value) {
        if (key != null && value != null) {
            if (this.opt(key) != null) {
                throw new RuntimeException("Duplicate key \"" + key + "\"");
            }
            this.put(key, value);
        }
        return this;
    }

    /**
     * Remove a name and its value, if present.
     *
     * @param key The name to be removed.
     * @return The value that was associated with the name, or null if there was no value.
     */
    public Object remove(String key) {
        this.key_order.remove(key);
        return this.map.remove(key);
    }

    /**
     * Make a JSON text of this Node. For compactness, no whitespace is added. If this would not
     * result in a syntactically correct JSON text, then null will be returned instead.
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable representation of the object,
     * beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     * <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    @Override
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Make a prettyprinted JSON text of this Node.
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @param indentFactor The number of spaces to add to each level of indentation.
     * @return a printable, displayable, portable, transmittable representation of the object,
     * beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     * <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws RuntimeException If the object contains an invalid number.
     */
    public String toString(int indentFactor) {
        StringWriter w = new StringWriter();
        synchronized (w.getBuffer()) {
            return this.write(w, indentFactor, 0).toString();
        }
    }

    /**
     * Write the contents of the Node as JSON text to a writer. For compactness, no whitespace is
     * added.
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws RuntimeException
     */
    Writer write(Writer writer, int indentFactor, int indent) {
        try {
            boolean commanate = false;
            final int length = this.length();
            Iterator<String> keys = this.keyIterator();
            writer.write('{');

            if (length == 1) {
                Object key = keys.next();
                writer.write(quote(key.toString()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeValue(writer, this.map.get(key), indentFactor, indent);
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                while (keys.hasNext()) {
                    Object key = keys.next();
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    writer.write(quote(key.toString()));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    writeValue(writer, this.map.get(key), indentFactor, newindent);
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public int indexOf(String name) {
        return key_order.indexOf(name);
    }

    public void setIndexOf(String name, int index) {
        if (index <= 0) {
            return;
        }
        key_order.remove(name);
        if (index < key_order.size()) {
            key_order.add(index, name);
        } else {
            key_order.add(name);
        }
    }

    public Object getObjectForPath(Path path) {
        Object node;
        if (has(path.getFirstElement())) {
            Object object = get(path.getFirstElement());
            node = object;
        } else {
            if (path.getFirstElement() == null) {
                return this;
            }
            node = new Node();
            put(path.getFirstElement(), node);
        }

        if (node instanceof Node) {
            return ((Node) node).getObjectForPath(path.getSubpath(1));
        }
        return node;
    }

    public Node getNodeForPath(ChangeLog log, Path path) {
        Node node;
        String firstElement = path.getFirstElement();
        if (firstElement == null) {
            return this;
        }
        if (has(firstElement)) {
            Object object = get(firstElement);
            if (object instanceof Node) {
                node = (Node) object;
            } else {
                node = new Node();
                log.addChildAddedLogEntry(firstElement, path, path.getParent(), node, false, 0, null, -1);
                put(firstElement, node);
            }
        } else {
            node = new Node();
            log.addChildAddedLogEntry(firstElement, path, path.getParent(), node, false, 0, null, -1);
            put(firstElement, node);
        }
        if (path.isSimple()) {
            if (has(firstElement)) {
                Object object = get(firstElement);
                if (object instanceof Node) {
                    node = (Node) object;
                } else {
                    node = new Node();
                    log.addChildAddedLogEntry(firstElement, path, path.getParent(), node, false, 0, null, -1);
                    put(firstElement, node);
                }
            } else {
                node = new Node();
                log.addChildAddedLogEntry(firstElement, path, path.getParent(), node, false, 0, null, -1);
                put(firstElement, node);
            }
            return node;
        }
        Path subpath = path.getSubpath(1);
        if (subpath.isEmtpy()) {
            return this;
        } else {
            return node.getNodeForPath(log, subpath);
        }
    }

    public void populate(ChangeLogBuilder logBuilder, Node payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof Node) {
                Node childNode = new Node();
                childNode.populate(logBuilder.getChildLogBuilder(key), (Node) value);
                if (has(key)) {
                    put(key, childNode);
                    logBuilder.addNew(key, childNode);
                } else {
                    put(key, childNode);
                    logBuilder.addChangedNode(key, childNode);
                }
            } else {
                if (has(key)) {
                    logBuilder.addChange(key, value);
                } else {
                    logBuilder.addNew(key, value);
                }
                if (value == null) {
                    logBuilder.addRemoved(key, get(key));
                }
                put(key, value);
            }
        }
    }

    public Collection<Node> getChildren() {
        Set<Node> nodes = Sets.newHashSet();
        Iterator<?> itr = keyIterator();
        while (itr.hasNext()) {
            Object key = itr.next();
            if (get((String) key) instanceof Node) {
                nodes.add((Node) get((String) key));
            }
        }
        return nodes;
    }

    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    public boolean pathExists(Path path) {
        if (path.isEmtpy()) {
            return true;
        } else if (has(path.getFirstElement())) {
            Object object = get(path.getFirstElement());
            if (object instanceof Node) {
                Node node = (Node) object;
                return node.pathExists(path.getSubpath(1));
            } else if (path.isSimple()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public Node getLastLeafNode(Path path) {
        if (has(path.getFirstElement())) {
            if (path.isSimple()) {
                if (get(path.getFirstElement()) instanceof Node) {
                    return getNode(path.getFirstElement());
                } else {
                    return this;
                }
            } else {
                return getNode(path.getFirstElement()).getLastLeafNode(path.getSubpath(1));
            }
        } else {
            return this;
        }
    }

    public void accept(Path path, NodeVisitor visitor) {
        visitor.visitNode(path, this);
        for (String key : keys()) {
            Object value = get(key);
            if (value instanceof Node) {
                ((Node) value).accept(path.append(key), visitor);
            } else {
                visitor.visitProperty(path, this, key, value);
            }
        }
    }

    public void clear() {
        map.clear();
        key_order.clear();
    }

    /**
     * Node.NULL is equivalent to the value that JavaScript calls null, whilst Java's null is
     * equivalent to the value that JavaScript calls undefined.
     */
    private static final class Null {

        /**
         * There is only intended to be a single instance of the NULL object, so the clone method
         * returns itself.
         *
         * @return NULL.
         */
        @Override
        protected final Object clone() {
            return this;
        }

        /**
         * A Null object is equal to the null value and to itself.
         *
         * @param object An object to test for nullness.
         * @return true if the object parameter is the Node.NULL object or null.
         */
        @Override
        public boolean equals(Object object) {
            return object == null || object == this;
        }

        /**
         * Get the "null" string value.
         *
         * @return The string "null".
         */
        @Override
        public String toString() {
            return "null";
        }
    }
}