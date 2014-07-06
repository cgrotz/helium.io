package io.helium.persistence.mapdb;

import io.helium.common.Path;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Optional;

/**
 * Created by Christoph Grotz on 15.06.14.
 */
public class MapDbService {

    private static Optional<MapDbService> instance = Optional.empty();

    public static MapDbService get() {
        instance = instance.of(instance.orElseGet(() -> new MapDbService()));
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
                    e.printStackTrace();
                }
            }
        });
    }

    private DB createDb() {
        //return DBMaker.newMemoryDB().make();
        return DBMaker.newFileDB(new File("helium/nodes"))
                .closeOnJvmShutdown()
                .make();
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
            currentPath = currentPath.subpath(1);
        }
        return node;
    }

    public BTreeMap<String, Object> getTreeMap(String key) {
        return db.getTreeMap(key);
    }

    public DB.BTreeMapMaker createTreeMap(String key) {
        return db.createTreeMap(key);
    }

    public void commit() {
        db.commit();
    }
}
