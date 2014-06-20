package io.helium.persistence.mapdb;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.helium.authorization.Authorizator;
import io.helium.authorization.Operation;
import io.helium.common.EndpointConstants;
import io.helium.common.Path;
import io.helium.event.changelog.*;
import io.helium.persistence.visitor.ChildDeletedSubTreeVisitor;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import sun.nio.ch.ThreadPool;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Christoph Grotz on 20.06.14.
 */
public class PersistenceExecutor extends Verticle {
    public static final String PERSIST_CHANGE_LOG = "io.helium.changelog.persist";

    private ExecutorService executor = Executors.newCachedThreadPool();
    private int count = 0;
    @Override
    public void start() {
        NodeFactory.get().dir(new File(container.config().getString("directory")));
        vertx.eventBus().registerHandler(PERSIST_CHANGE_LOG, this::applyChangeLog );

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL demo = cl.getResource("demo.json");
            if(demo != null) {
                String demoData = Resources.toString(demo, Charsets.UTF_8);
                loadJsonObject(Path.of("/"), new JsonObject(demoData));
            }
            else {
                throw new IllegalStateException("demo.json not found");
            }
        }
        catch(Exception e){
        }
    }

    private void loadJsonObject(Path path, JsonObject data) {
        for(String key : data.getFieldNames()) {
            Object value = data.getField(key);
            if(value instanceof JsonObject) {
                loadJsonObject(path.append(key), (JsonObject)value);
            }
            else {
                Node node = Node.of(path);
                node.put(key, value);
            }
        }
    }

    /**
     * TODO - make Persistence a 3 Step process:
     * 1. Compile Changelog
     * 2. Distribute Changelog
     * 3. Adapt changes from Changelog
     */
    private void applyChangeLog(Message<JsonArray> message) {
        container.logger().info("Persisting changelog: "+message.body());
        ChangeLog changeLog = ChangeLog.of(message.body());
        changeLog.forEach(obj -> {
            JsonObject logEvent = (JsonObject) obj;
            if (logEvent.getString("type").equals(ChildAdded.class.getSimpleName())) {
                childAdded(ChildAdded.of(logEvent));
            }
            if (logEvent.getString("type").equals(ChildChanged.class.getSimpleName())) {
                childChanged(ChildChanged.of(logEvent));
            }
            if (logEvent.getString("type").equals(ValueChanged.class.getSimpleName())) {
                valueChanged(ValueChanged.of(logEvent));
            }
            if (logEvent.getString("type").equals(ChildDeleted.class.getSimpleName())) {
                childDeleted(ChildDeleted.of(logEvent));
            }
        });
        if(count > 5) {
            long start = System.currentTimeMillis();
            NodeFactory.get().getDb().commit();
            container.logger().info("Commiting took: "+(System.currentTimeMillis()-start)+"ms");
            count = 0;
        }
        count++;
    }

    private void childAdded(ChildAdded logEvent) {
        Object value = logEvent.value();
        Node parent = Node.of(logEvent.path());
        parent.put(logEvent.name(), value);
    }

    private void childChanged(ChildChanged logEvent) {
        Object value = logEvent.value();
        Node parent = Node.of(logEvent.path());
        parent.put(logEvent.name(), value);
    }

    private void childDeleted(ChildDeleted logEvent) {
        Node parent = Node.of(logEvent.path().parent());
        parent.delete(logEvent.path().lastElement());
        NodeFactory.get().getDb().commit();
    }

    private void valueChanged(ValueChanged logEvent) {
        Node parent = Node.of(logEvent.path().parent());
        Object value = logEvent.value();
        parent.put(logEvent.name(), value);
    }
}
