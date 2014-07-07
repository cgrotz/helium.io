package io.helium.persistence.mapdb;

import io.helium.common.Path;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public class MapDbService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapDbService.class);

    private static Optional<MapDbService> instance = Optional.empty();

    public static MapDbService get() {
        instance = Optional.of(instance.orElseGet(MapDbService::new));
        return instance.get();
    }

    private DB db;

    private MapDbService() {
        this.db = createDb();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("Shutdown MapDB Data Store");
                    if(!db.isClosed()) {
                        db.commit();
                        db.compact();
                        db.close();
                    }
                }
                catch( Exception e) {
                    LOGGER.error("Failed to close DB", e);
                }
            }
        });
    }

    private DB createDb() {
        return DBMaker.newFileDB(new File("helium/nodes"))
                .closeOnJvmShutdown()
                .cacheSize(10)
                .mmapFileEnableIfSupported()
                .make();
    }

    public void commit() {
        //db.commit();
    }

    public void commitAndCompact() {
        db.commit();
        db.compact();
    }

    public boolean exists(Path path) {
        return db.exists(path.toString() + "Attributes");
    }

    public Node root() {
        return new Node();
    }

    public Node of(Path path) {
        if (path.root()) {
            return root();
        }
        Node node = root();
        Path currentPath = Path.copy(path);
        for (int i = 0; i < path.toArray().length; i++) {
            node = node.getNode(currentPath.firstElement());
            currentPath = currentPath.sub(1);
        }
        return node;
    }

    public BTreeMap<String, Object> getTreeMap(String key) {
        return db.createTreeMap(key).nodeSize(6).valuesOutsideNodesEnable().makeOrGet();
    }

    public DB.BTreeMapMaker createTreeMap(String key) {
        return db.createTreeMap(key).nodeSize(6).valuesOutsideNodesEnable();
    }
}
