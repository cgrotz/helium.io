package io.helium.persistence.mapdb;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Christoph Grotz on 02.06.14.
 */
public class MapDbBackedNode {

    private static final long serialVersionUID = 1L;

    private static final DB db = DBMaker.newFileDB(new File("helium/data"))
            .asyncWriteEnable()
            .asyncWriteQueueSize(10)
            .closeOnJvmShutdown()
            .make();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            try {
                System.out.println("Shutdown MapDB Data Store");
                db.commit();
                db.compact();
                db.close();
            }
            catch( Exception e) {
            }
            }
        });
    }

    public static DB getDb() {
        return db;
    }

    private BTreeMap<String, Object> attributes;
    private BTreeMap<String, MapDbBackedNode> nodes;
    private Path pathToNode;

    private MapDbBackedNode() {
        pathToNode = Path.of("/");
        attributes = db.getTreeMap("rootAttributes");
        nodes = db.createTreeMap("rootNodes").valueSerializer(new MapDbBackeNodeSerializer()).makeOrGet();
    }

    MapDbBackedNode(Path pathToNode) {
        this.pathToNode = pathToNode;
        attributes = db.getTreeMap(pathToNode + "Attributes");
        nodes = db.createTreeMap(pathToNode + "Nodes").valueSerializer(new MapDbBackeNodeSerializer()).makeOrGet();
    }
/*
    public void writeExternal(ObjectOutput out) throws IOException {
        out.write(pathToNode.toString().getBytes());
        attributes = db.getHashMap(pathToNode + "Attributes");
        nodes = db.createHashMap(pathToNode + "Nodes").valueSerializer(new MapDbBackeNodeSerializer()).makeOrGet();
        keyListsMap = db.getHashMap("orderedKeys");
        if (keyListsMap.containsKey(pathToNode + "Keys")) {
            keyOrder = keyListsMap.get(pathToNode + "Keys");
        } else {
            keyOrder = new ArrayList();
            keyListsMap.put(pathToNode + "Keys", keyOrder);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        String source = in.readUTF();
        pathToNode = new Path(source);
        attributes = db.getHashMap(pathToNode + "Attributes");
        nodes = db.getHashMap(pathToNode + "Nodes");
        keyListsMap = db.getHashMap("orderedKeys");
        if (keyListsMap.containsKey(pathToNode + "Keys")) {
            keyOrder = keyListsMap.get(pathToNode + "Keys");
        } else {
            keyOrder = new ArrayList();
            keyListsMap.put(pathToNode + "Keys", keyOrder);
        }
    }*/

    /**
     * Get an array of field names from a MapDbBackedNode.
     *
     * @return An array of field names, or null if there are no names.
     */
    public static String[] getNames(MapDbBackedNode jo) {
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
            return null;
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
     * is a MAP, then a MapDbBackedNode will be made from it and its toJSONString method will be called.
     * Otherwise, the value's toString method will be called, and the result will be quoted.
     * <p>
     * <p>
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
        } else if (value instanceof MapDbBackedNode) {
            ((MapDbBackedNode) value).write(writer, indentFactor, indent);
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


    /**
     * Get the value object associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return The object associated with the pathToNode.
     * @throws RuntimeException if the pathToNode is not found.
     */

    public Object get(String key) {
        if (Strings.isNullOrEmpty(key)) {
            throw new RuntimeException("Null pathToNode.");
        }
        return this.opt(key);
    }


    public Object get(String key, Object defaultValue) {
        if (has(key)) {
            return get(key);
        }
        return defaultValue;
    }

    /**
     * Get an optional value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return An object which is the value, or null if there is no value.
     */
    public Object opt(String key) {
        if (Strings.isNullOrEmpty(key)) {
            return null;
        } else if (this.attributes.containsKey(key)) {
            return this.attributes.get(key);
        } else if (this.nodes.containsKey(key)) {
            return this.nodes.get(key);
        } else {
            return null;
        }
    }

    /**
     * Get the boolean value associated with a pathToNode.
     *
     * @param key A pathToNode string.
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
        return false;
    }


    public boolean getBoolean(String key, boolean def) {
        if (this.has(key)) {
            Object object = this.get(key);
            if (object.equals(Boolean.FALSE)
                    || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {
                return false;
            } else if (object.equals(Boolean.TRUE)
                    || (object instanceof String && ((String) object).equalsIgnoreCase("true"))) {
                return true;
            }
        }
        return def;
    }

    /**
     * Get the double value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return The numeric value.
     * @throws RuntimeException if the pathToNode is not found or if the value is not a Number object and cannot be
     *                          converted to a number.
     */

    public double getDouble(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).doubleValue() : Double
                    .parseDouble((String) object);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Get the int value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return The integer value.
     * @throws RuntimeException if the pathToNode is not found or if the value cannot be converted to an integer.
     */

    public int getInt(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).intValue() : Integer
                    .parseInt((String) object);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the MapDbBackedNode value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return A MapDbBackedNode which is the value.
     * @throws RuntimeException if the pathToNode is not found or if the value is not a MapDbBackedNode.
     */

    public MapDbBackedNode getNode(String key) {
        if (pathToNode.append(key).root()) {
            return root();
        } else if (has(key)) {
            Object value = get(key);
            return (MapDbBackedNode) get(key);
        } else {
            MapDbBackedNode node = new MapDbBackedNode(pathToNode.append(key));
            nodes.put(key, node);
            return node;
        }
    }

    /**
     * Get the long value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return The long value.
     * @throws RuntimeException if the pathToNode is not found or if the value cannot be converted to a long.
     */
    public long getLong(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number ? ((Number) object).longValue() : Long
                    .parseLong((String) object);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the string associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return A string which is the value.
     * @throws RuntimeException if there is no string value for the pathToNode.
     */

    public String getString(String key) {
        Object object = this.get(key);
        if (object instanceof String) {
            return (String) object;
        }
        return null;
    }

    /**
     * Determine if the MapDbBackedNode contains a specific pathToNode.
     *
     * @param key A pathToNode string.
     * @return true if the pathToNode exists in the MapDbBackedNode.
     */

    public boolean has(String key) {
        return !Strings.isNullOrEmpty(key) && (
                this.attributes.containsKey(key) ||
                        this.nodes.keySet().contains(key));
    }

    /**
     * Get an enumeration of the keys of the MapDbBackedNode.
     *
     * @return An iterator of the keys.
     */
    public Iterator<String> keyIterator() {
        return keys().iterator();
    }

    /**
     * Get a set of keys of the MapDbBackedNode.
     *
     * @return A keySet.
     */

    public List<String> keys() {
        List keys = Lists.newArrayList();
        keys.addAll(this.attributes.keySet());
        keys.addAll(this.nodes.keySet());
        return keys;
    }

    /**
     * Get the number of keys stored in the MapDbBackedNode.
     *
     * @return The number of keys in the MapDbBackedNode.
     */

    public int length() {
        return this.nodes.size() + this.attributes.size();
    }

    /**
     * Put a pathToNode/boolean pair in the MapDbBackedNode.
     *
     * @param key   A pathToNode string.
     * @param value A boolean which is the value.
     * @return this.
     * @throws RuntimeException If the pathToNode is null.
     */
    public MapDbBackedNode put(String key, boolean value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    /**
     * Put a pathToNode/double pair in the MapDbBackedNode.
     *
     * @param key   A pathToNode string.
     * @param value A double which is the value.
     * @return this.
     * @throws RuntimeException If the pathToNode is null or if the number is invalid.
     */

    public MapDbBackedNode put(String key, double value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        this.put(key, new Double(value));
        return this;
    }

    /**
     * Put a pathToNode/int pair in the MapDbBackedNode.
     *
     * @param key   A pathToNode string.
     * @param value An int which is the value.
     * @return this.
     * @throws RuntimeException If the pathToNode is null.
     */

    public MapDbBackedNode put(String key, int value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        this.put(key, new Integer(value));
        return this;
    }

    /**
     * Put a pathToNode/long pair in the MapDbBackedNode.
     *
     * @param key   A pathToNode string.
     * @param value A long which is the value.
     * @return this.
     * @throws RuntimeException If the pathToNode is null.
     */

    public MapDbBackedNode put(String key, long value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        this.put(key, new Long(value));
        return this;
    }

    /**
     * Put a pathToNode/value pair in the MapDbBackedNode. If the value is null, then the pathToNode will be removed from the
     * MapDbBackedNode if it is present.
     *
     * @param key   A pathToNode string.
     * @param value An object which is the value. It should be of one of these types: Boolean, Double,
     *              Integer, JSONArray, MapDbBackedNode, Long, String, or the null object.
     * @return this.
     * @throws RuntimeException If the value is non-finite number or if the pathToNode is null.
     */

    public MapDbBackedNode put(String key, Object value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        return putWithIndex(key, value);
    }


    public MapDbBackedNode put(String key, MapDbBackedNode node) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        return putWithIndex(key, node);
    }


    public MapDbBackedNode putWithIndex(String key, Object value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        if (key == null) {
            throw new NullPointerException("Null pathToNode.");
        }
        if (value != null) {
            testValidity(value);
            if (value instanceof MapDbBackedNode) {
                this.nodes.put(key, (MapDbBackedNode) value);
            } else if (value instanceof JsonObject) {
                JsonObject node = (JsonObject) value;
                node.getFieldNames().forEach(valueKey -> {
                    MapDbBackedNode nodeToFill = MapDbBackedNode.of(pathToNode.append(key));
                    nodeToFill.put(valueKey, node.getValue(valueKey));
                });
            } else {
                this.attributes.put(key, value);
            }
        } else {
            this.remove(key);
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
        if (this.nodes.containsKey(key)) {
            return this.nodes.remove(key);
        } else if (this.attributes.containsKey(key)) {
            return this.attributes.remove(key);
        } else {
            return null;
        }
    }

    /**
     * Make a JSON text of this MapDbBackedNode. For compactness, no whitespace is added. If this would not
     * result in a syntactically correct JSON text, then null will be returned instead.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable representation of the object,
     * beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     * <code>}</code>&nbsp;<small>(right brace)</small>.
     */

    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Make a prettyprinted JSON text of this MapDbBackedNode.
     * <p>
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
     * Write the contents of the MapDbBackedNode as JSON text to a writer. For compactness, no whitespace is
     * added.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws RuntimeException
     */

    public Writer write(Writer writer, int indentFactor, int indent) {
        try {
            boolean commanate = false;
            final int length = this.length();
            Iterator<String> keys = this.keyIterator();
            writer.write('{');

            if (length == 1) {
                String key = keys.next();
                writer.write(quote(key.toString()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeValue(writer, get(key), indentFactor, indent);
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!Strings.isNullOrEmpty(key)) {
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
                        writeValue(writer, get(key), indentFactor, newindent);
                        commanate = true;
                    }
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

    public Object getObjectForPath(Path path) {
        MapDbBackedNode parent = getNodeForPath(ChangeLog.of(new JsonArray()), path.parent());
        return parent.get(path.lastElement());
    }


    public MapDbBackedNode getNodeForPath(ChangeLog log, Path path) {
        Path fullPath = pathToNode.append(path);
        MapDbBackedNode lastNode = null;
        for (int i = 0; i < fullPath.toArray().length; i++) {
            Path nodePath = fullPath.prefix(0);
            MapDbBackedNode node = MapDbBackedNode.of(nodePath);
            if (lastNode == null) {
                node = lastNode;
            } else {
                lastNode.put(nodePath.lastElement(), node);
            }
        }
        return MapDbBackedNode.of(fullPath);
    }


    public void populate(ChangeLogBuilder logBuilder, MapDbBackedNode payload) {
        for (String key : payload.keys()) {
            Object value = payload.get(key);
            if (value instanceof MapDbBackedNode) {
                MapDbBackedNode childNode = MapDbBackedNode.of(pathToNode.append(key));
                childNode.populate(logBuilder.getChildLogBuilder(key), (MapDbBackedNode) value);
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


    public Collection<MapDbBackedNode> getChildren() {
        Set<MapDbBackedNode> nodes = Sets.newHashSet();
        Iterator<?> itr = keyIterator();
        while (itr.hasNext()) {
            Object key = itr.next();
            if (get((String) key) instanceof MapDbBackedNode) {
                nodes.add((MapDbBackedNode) get((String) key));
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
        } else if (has(path.firstElement())) {
            Object object = get(path.firstElement());
            if (object instanceof MapDbBackedNode) {
                MapDbBackedNode node = (MapDbBackedNode) object;
                return node.pathExists(path.subpath(1));
            } else if (path.isSimple()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    public MapDbBackedNode getLastLeafNode(Path path) {
        if (has(path.firstElement())) {
            if (path.isSimple()) {
                if (get(path.firstElement()) instanceof MapDbBackedNode) {
                    return getNode(path.firstElement());
                } else {
                    return this;
                }
            } else {
                return getNode(path.firstElement()).getLastLeafNode(path.subpath(1));
            }
        } else {
            return this;
        }
    }


    public void accept(Path path, NodeVisitor visitor) {
        visitor.visitNode(path, this);
        for (String key : keys()) {
            Object value = get(key);
            if (value instanceof MapDbBackedNode) {
                ((MapDbBackedNode) value).accept(path.append(key), visitor);
            } else {
                visitor.visitProperty(path, this, key, value);
            }
        }
    }


    public void clear() {
        nodes.clear();
        attributes.clear();
    }


    public Collection<Object> values() {
        Set<Object> values = Sets.newHashSet();
        nodes.values().forEach(val -> {
            values.add(val);
        });
        attributes.values().forEach(val -> {
            values.add(val);
        });
        return values;
    }

    public static MapDbBackedNode of(Path path) {
        if (path.root()) {
            return root();
        }
        MapDbBackedNode node = root();
        Path currentPath = Path.copy(path);
        for (int i = 0; i < path.toArray().length; i++) {
            node = node.getNode(currentPath.firstElement());
            currentPath = currentPath.subpath(1);
        }
        return node;
    }

    public static long childCount(Object node) {
        return (node instanceof MapDbBackedNode) ? ((MapDbBackedNode) node).getChildren().size() : 0;
    }

    public static boolean hasChildren(Object node) {
        return (node instanceof MapDbBackedNode) ? ((MapDbBackedNode) node).hasChildren() : false;
    }

    public JsonObject toJsonObject() {
        return new JsonObject(toString());
    }

    public static boolean exists(Path path) {
        return db.exists(path.toString() + "Attributes");
    }

    public Path getPathToNode() {
        return pathToNode;
    }

    public static MapDbBackedNode root() {
        MapDbBackedNode root = new MapDbBackedNode();
        return root;
    }
}
