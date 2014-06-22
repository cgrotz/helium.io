package io.helium.persistence.mapdb;

import io.helium.common.Path;
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

    private Optional<DB> db = Optional.empty();

    private MapDbService() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            try {
                System.out.println("Shutdown MapDB Data Store");
                if(!db.isPresent()) {
                    if(!db.get().isClosed()) {
                        db.get().commit();
                        db.get().compact();
                        db.get().close();
                    }
                }
            }
            catch( Exception e) {
            }
            }
        });
    }

    public DB getDb() {
        if(directory.isPresent()) {
            db = db.of(db.orElseGet(() -> DBMaker.newFileDB(directory.get())
                    .closeOnJvmShutdown()
                    .make()));
            return db.get();
        }
        return null;
    }


    public boolean exists(Path path) {
        return db.get().exists(path.toString() + "Attributes");
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

    private Optional<File> directory = Optional.empty();
    public void dir(File directory) {
        directory.getParentFile().mkdirs();
        this.directory = Optional.of(directory);
    }
}
