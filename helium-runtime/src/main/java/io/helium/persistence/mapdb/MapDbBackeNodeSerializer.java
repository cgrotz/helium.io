package io.helium.persistence.mapdb;

import io.helium.common.Path;
import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public class MapDbBackeNodeSerializer implements Serializer<MapDbBackedNode>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public void serialize(DataOutput out, MapDbBackedNode value) throws IOException {
        out.writeUTF(value.getPathToNode().toString());
    }

    @Override
    public MapDbBackedNode deserialize(DataInput in, int available) throws IOException {
        String pathToNode = in.readUTF();
        try {
            Path path = Path.of(pathToNode);
            if (path.root()) {
                return MapDbBackedNode.root();
            } else {
                return new MapDbBackedNode(path);
            }
        } catch (java.lang.StackOverflowError e) {
            System.out.println(pathToNode);
            throw e;
        }
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}