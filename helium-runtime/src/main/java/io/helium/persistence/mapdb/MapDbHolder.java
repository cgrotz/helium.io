package io.helium.persistence.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

/**
 * Created by Christoph Grotz on 02.06.14.
 */
public class MapDbHolder {
    private static DB db;
    public static DB get() {
        if(db == null) {
            db = DBMaker.newFileDB(new File("helium/heliumData"))
                    .closeOnJvmShutdown()
                    .make();
        }
        return db;
    }


}
