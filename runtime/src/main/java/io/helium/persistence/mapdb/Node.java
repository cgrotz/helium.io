package io.helium.persistence.mapdb;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.helium.common.Path;
import io.helium.persistence.mapdb.visitor.NodeVisitor;
import org.mapdb.BTreeMap;
import org.vertx.java.core.json.JsonObject;

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
public class Node {

    private static final long serialVersionUID = 1L;

    private BTreeMap<String, Object> attributes;
    private BTreeMap<String, Node> nodes;
    private Path pathToNode;

    Node() {
        pathToNode = Path.of("/");
        attributes = MapDbService.get().getTreeMap("rootAttributes");
        nodes = MapDbService.get().createTreeMap("rootNodes").valueSerializer(new MapDbBackeNodeSerializer()).makeOrGet();
    }

    protected Node(Path pathToNode) {
        this.pathToNode = pathToNode;
        attributes = MapDbService.get().getTreeMap(pathToNode + "Attributes");
        nodes = MapDbService.get().createTreeMap(pathToNode + "Nodes").valueSerializer(new MapDbBackeNodeSerializer()).makeOrGet();
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
     * Get the MapDbBackedNode value associated with a pathToNode.
     *
     * @param key A pathToNode string.
     * @return A MapDbBackedNode which is the value.
     * @throws RuntimeException if the pathToNode is not found or if the value is not a MapDbBackedNode.
     */
    public Node getNode(String key) {
        if (pathToNode.append(key).root()) {
            return MapDbService.get().root();
        } else if (has(key)) {
            Object value = get(key);
            return (Node) get(key);
        } else {
            Node node = new Node(pathToNode.append(key));
            nodes.put(key, node);
            return node;
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
     * Put a pathToNode/value pair in the MapDbBackedNode. If the value is null, then the pathToNode will be deleted from the
     * MapDbBackedNode if it is present.
     *
     * @param key   A pathToNode string.
     * @param value An object which is the value. It should be of one of these types: Boolean, Double,
     *              Integer, JSONArray, MapDbBackedNode, Long, String, or the null object.
     * @return this.
     * @throws RuntimeException If the value is non-finite number or if the pathToNode is null.
     */

    Node put(String key, Object value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        return putWithIndex(key, value);
    }


    Node put(String key, Node node) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        return putWithIndex(key, node);
    }


    Node putWithIndex(String key, Object value) {
        if (Strings.isNullOrEmpty(key)) {
            return this;
        }
        if (key == null) {
            throw new NullPointerException("Null pathToNode.");
        }
        if (value != null) {
            testValidity(value);
            if (value instanceof Node) {
                this.nodes.put(key, (Node) value);
            } else if (value instanceof JsonObject) {
                JsonObject node = (JsonObject) value;
                node.getFieldNames().forEach(valueKey -> {
                    Node nodeToFill = MapDbService.get().of(pathToNode.append(key));
                    nodeToFill.put(valueKey, node.getValue(valueKey));
                });
            } else {
                this.attributes.put(key, value);
            }
        } else {
            this.delete(key);
        }
        return this;
    }

    /**
     * Delete a name and its value, if present.
     *
     * @param key The name to be deleted.
     * @return The value that was associated with the name, or null if there was no value.
     */

    Object delete(String key) {
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
                writer.write(quote(key));
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
                        writer.write(quote(key));
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
        Node parent = getNodeForPath(path.parent());
        return parent.get(path.lastElement());
    }


    public Node getNodeForPath(Path path) {
        Path fullPath = pathToNode.append(path);
        Node lastNode = null;
        for (int i = 0; i < fullPath.toArray().length; i++) {
            Path nodePath = fullPath.prefix(0);
            Node node = MapDbService.get().of(nodePath);
            if (lastNode == null) {
                lastNode = node;
            } else {
                lastNode.put(nodePath.lastElement(), node);
            }
        }
        return MapDbService.get().of(fullPath);
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

    public Node getLastLeafNode(Path path) {
        if (has(path.firstElement())) {
            if (path.isSimple()) {
                if (get(path.firstElement()) instanceof Node) {
                    return getNode(path.firstElement());
                } else {
                    return this;
                }
            } else {
                return getNode(path.firstElement()).getLastLeafNode(path.subpath(1));
            }
        } else if( has("+") && !path.isSimple()) {
            return getNode("+").getLastLeafNode(path.subpath(1));
        } else if( has("*") && !path.isSimple()) {
            return getNode("*");
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
        this.attributes.clear();
        this.nodes.clear();
    }

    public static long childCount(Object node) {
        return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
    }

    public static boolean hasChildren(Object node) {
        return (node instanceof Node) && ((Node) node).hasChildren();
    }

    public JsonObject toJsonObject() {
        return new JsonObject(toString());
    }

    public Path getPathToNode() {
        return pathToNode;
    }
}
