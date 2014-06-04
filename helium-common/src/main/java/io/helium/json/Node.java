package io.helium.json;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Christoph Grotz on 30.05.14.
 */
public interface Node {

    Object get(String key);

    boolean getBoolean(String key);

    boolean getBoolean(String key, boolean def);

    double getDouble(String key);

    int getInt(String key);

    Node getNode(String key);

    long getLong(String key);

    String getString(String key);

    boolean has(String key);

    Node increment(String key);

    boolean isNull(String key);

    Iterator<String> keyIterator();

    List<String> keys();

    int length();

    Object opt(String key);

    boolean optBoolean(String key);

    boolean optBoolean(String key, boolean defaultValue);

    double optDouble(String key);

    double optDouble(String key, double defaultValue);

    int optInt(String key);

    int optInt(String key, int defaultValue);

    Node optNode(String key);

    long optLong(String key);

    long optLong(String key, long defaultValue);

    String optString(String key);

    String optString(String key, String defaultValue);

    Node put(String key, boolean value);

    Node put(String key, double value);

    Node put(String key, int value);

    Node put(String key, long value);

    Node put(String key, Node node);

    Node put(String key, Object value);

    Node putWithIndex(String key, Object value, int index);

    Node putOnce(String key, Object value);

    Object remove(String key);

    @Override
    String toString();

    String toString(int indentFactor);

    int indexOf(String name);

    void setIndexOf(String name, int index);

    Object getObjectForPath(Path path);

    Node getNodeForPath(ChangeLog log, Path path);

    void populate(ChangeLogBuilder logBuilder, Node payload);

    Collection<Node> getChildren();

    boolean hasChildren();

    boolean pathExists(Path path);

    Node getLastLeafNode(Path path);

    void accept(Path path, NodeVisitor visitor);

    void clear();

    Collection<Object> values();

    Object get(String key, Object defaultValue);

    /**
     * Node.NULL is equivalent to the value that JavaScript calls null, whilst Java's null is
     * equivalent to the value that JavaScript calls undefined.
     */
    public static final class Null {

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


    public static String prevChildName(Node parent, int priority) {
        if (priority <= 0) {
            return null;
        }
        return parent.keys().get(priority - 1);
    }

    public static long childCount(Object node) {
        return (node instanceof Node) ? ((Node) node).getChildren().size() : 0;
    }

    public static int priority(Node parentNode, String name) {
        return parentNode.indexOf(name);
    }

    public static boolean hasChildren(Object node) {
        return (node instanceof Node) ? ((Node) node).hasChildren() : false;
    }
}
