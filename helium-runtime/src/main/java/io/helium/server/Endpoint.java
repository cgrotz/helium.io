package io.helium.server;

import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Christoph Grotz on 26.05.14.
 */
public interface Endpoint {

    public static final String DISTRIBUTE_HELIUM_EVENT = "io.helium.distribute.helium.event";
    public static final String DISTRIBUTE_EVENT = "io.helium.distribute.event";
    public static final String DISTRIBUTE_CHANGE_LOG = "io.helium.distribute.change.log";

    public static final String QUERY_CHILD_REMOVED = "query_child_removed";

    public static final String QUERY_CHILD_CHANGED = "query_child_changed";

    public static final String QUERY_CHILD_ADDED = "query_child_added";

    public static final String CHILD_REMOVED = "child_removed";

    public static final String VALUE = "value";

    public static final String CHILD_CHANGED = "child_changed";

    public static final String CHILD_ADDED = "child_added";

    void fireChildAdded(String name, Path path, Path parent, Object value, boolean hasChildren, long numChildren);

    void fireValue(String name, Path path, Path parent, Object value);
}
