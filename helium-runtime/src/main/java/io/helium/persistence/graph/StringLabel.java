package io.helium.persistence.graph;

import org.neo4j.graphdb.Label;

/**
 * Created by Christoph Grotz on 23.05.14.
 */
public class StringLabel implements Label {
    private final String label;

    private StringLabel(String label) {
        this.label = label;
    }

    public static StringLabel from(String label) {
        return new StringLabel(label);
    }

    @Override
    public String name() {
        return label;
    }
}
