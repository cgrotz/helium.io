package io.helium.persistence.graph;

import com.google.common.collect.Lists;
import io.helium.Helium;
import io.helium.common.Path;
import io.helium.connectivity.messaging.HeliumEndpoint;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.rulebased.RulesDataSnapshot;
import io.helium.persistence.queries.QueryEvaluator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by balu on 23.05.14.
 */
public class GraphPersistence implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(GraphPersistence.class);
    private final GraphDatabaseService graphDb;
    private final Helium helium;

    public GraphPersistence(Helium helium) {
        this.helium = helium;

        this.graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder("db")
                .newGraphDatabase();
    }

    public static void main(String args[]) {
        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder("db")
                .newGraphDatabase();
        Transaction tx = graphDb.beginTx();
        org.neo4j.graphdb.Node root = graphDb.createNode(StringLabel.from("root"));

        org.neo4j.graphdb.Node node2 = graphDb.createNode();
        node2.setProperty("name", "hello");
        root.createRelationshipTo(node2, StringRelationship.from("hello"));

        org.neo4j.graphdb.Node node3 = graphDb.createNode();
        node3.setProperty("name", "world");
        node2.createRelationshipTo(node3, StringRelationship.from("world"));
        tx.success();

        tx = graphDb.beginTx();

        TraversalDescription td = graphDb.traversalDescription()
                .breadthFirst()
                .relationships(StringRelationship.from("hello"))
                .relationships(StringRelationship.from("world"))
                .evaluator(Evaluators.excludeStartPosition());

        td.traverse(root).nodes().forEach(node -> {
            System.out.println(node);
        });
        tx.success();
    }

    @Override
    public Object get(Path path) {
        Transaction tx = graphDb.beginTx();

        org.neo4j.graphdb.Node[] rootNodes = transformToArray(GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(StringLabel.from("Root")));

        TraversalDescription td = graphDb.traversalDescription().breadthFirst();
        for (String element : path.toArray()) {
            td = td.relationships(StringRelationship.from(element));
        }

        td.evaluator(Evaluators.excludeStartPosition())
                .traverse(rootNodes).nodes().forEach(node -> {
            System.out.println(node);
        });
        tx.success();

        return null;
    }

    @Override
    public Node getNode(Path path) {
        return null;
    }

    @Override
    public void remove(ChangeLog log, Node auth, Path path) {

    }

    @Override
    public void applyNewValue(ChangeLog log, Node auth, Path path, int priority, Object payload) {

    }

    @Override
    public void updateValue(ChangeLog log, Node auth, Path path, int priority, Object payload) {

    }

    @Override
    public void setPriority(ChangeLog log, Node auth, Path path, int priority) {

    }

    @Override
    public void syncPath(Path path, HeliumEndpoint handler) {

    }

    @Override
    public void syncPropertyValue(Path path, HeliumEndpoint heliumEventHandler) {

    }

    @Override
    public RulesDataSnapshot getRoot() {
        return null;
    }

    @Override
    public void syncPathWithQuery(Path path, HeliumEndpoint handler, QueryEvaluator queryEvaluator, String query) {

    }

    private org.neo4j.graphdb.Node[] transformToArray(ResourceIterable<org.neo4j.graphdb.Node> root) {
        List<org.neo4j.graphdb.Node> nodes = Lists.newArrayList();
        root.forEach(consumer -> nodes.add(consumer));
        return nodes.toArray(new org.neo4j.graphdb.Node[0]);
    }
}
