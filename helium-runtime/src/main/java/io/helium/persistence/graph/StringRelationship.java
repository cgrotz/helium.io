package io.helium.persistence.graph;

import org.neo4j.graphdb.RelationshipType;

/**
 * Created by Christoph Grotz on 23.05.14.
 */
public class StringRelationship implements RelationshipType {
    private final String label;

    private StringRelationship(String label) {
        this.label = label;
    }

    public static StringRelationship from(String label) {
        return new StringRelationship(label);
    }

    @Override
    public String name() {
        return label;
    }
}
