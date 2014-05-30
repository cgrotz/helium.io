package io.helium.json;

import io.helium.common.Path;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.event.changelog.ChangeLog;
import io.helium.event.changelog.ChangeLogBuilder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Christoph Grotz on 30.05.14.
 */
public interface Node {
    HeliumEventBuilder complete();

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
}
