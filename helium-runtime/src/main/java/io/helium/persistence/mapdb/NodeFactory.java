package io.helium.persistence.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by Christoph Grotz on 15.06.14.
 */
public class NodeFactory {

    private static Optional<NodeFactory> instance = Optional.empty();

    public static NodeFactory get() {
        instance = instance.of(instance.orElseGet(new Supplier<NodeFactory>() {
            @Override
            public NodeFactory get() {
                return new NodeFactory();
            }
        }));
        return instance.get();
    }

    private Optional<DB> db = Optional.empty();

    private NodeFactory() {
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
            db = db.of(db.orElseGet(new Supplier<DB>() {
                @Override
                public DB get() {
                    return DBMaker.newFileDB(directory.get())
                            .asyncWriteEnable()
                            .asyncWriteQueueSize(10)
                            .closeOnJvmShutdown()
                            .make();
                }
            }));
            return db.get();
        }
        return null;
    }

    private Optional<File> directory = Optional.empty();
    public void dir(File directory) {
        directory.getParentFile().mkdirs();
        this.directory = Optional.of(directory);
    }
}
