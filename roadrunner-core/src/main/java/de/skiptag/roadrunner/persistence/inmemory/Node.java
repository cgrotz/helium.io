package de.skiptag.roadrunner.persistence.inmemory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.collect.Sets;

import de.skiptag.roadrunner.persistence.Path;

public class Node extends JSONObject {

    public Node() {
	super();
    }

    public Node(JSONObject jo, String[] names) {
	super(jo, names);
    }

    public Node(JSONTokener x) {
	super(x);
    }

    public Node(String source) {
	super(source);
    }

    public Object getObjectForPath(Path path) {
	Object node;
	if (has(path.getFirstElement())) {
	    Object object = get(path.getFirstElement());
	    node = object;
	} else {
	    if (path.getFirstElement() == null) {
		return this;
	    }
	    node = new Node();
	    put(path.getFirstElement(), node);
	}

	if (node instanceof Node) {
	    return ((Node) node).getObjectForPath(path.getSubpath(1));
	}
	return null;
    }

    public Node getNodeForPath(Path path) {
	Node node;
	if (has(path.getFirstElement())) {
	    Object object = get(path.getFirstElement());
	    if (object instanceof Node) {
		node = (Node) object;
	    } else {
		node = new Node();
		put(path.getFirstElement(), node);
	    }
	} else {
	    node = new Node();
	    put(path.getFirstElement(), node);
	}
	if (path.isSimple()) {
	    if (has(path.getFirstElement())) {
		Object object = get(path.getFirstElement());
		if (object instanceof Node) {
		    node = (Node) object;
		} else {
		    node = new Node();
		    put(path.getFirstElement(), node);
		}
	    } else {
		node = new Node();
		put(path.getFirstElement(), node);
	    }
	    return node;
	}
	return node.getNodeForPath(path.getSubpath(1));
    }

    public void populate(JSONObject payload) {
	Iterator<?> itr = payload.keyIterator();
	while (itr.hasNext()) {
	    Object key = (Object) itr.next();
	    Object value = payload.get((String) key);
	    if (value instanceof JSONObject) {
		Node value2 = new Node();
		value2.populate((JSONObject) value);
		put((String) key, value2);
	    } else {
		put((String) key, value);
	    }
	}
    }

    public Collection<Node> getChildren() {
	Set<Node> nodes = Sets.newHashSet();
	Iterator<?> itr = keyIterator();
	while (itr.hasNext()) {
	    Object key = itr.next();
	    if (get((String) key) instanceof Node) {
		nodes.add((Node) get((String) key));
	    }
	}
	return nodes;
    }

    public boolean hasChildren() {
	return !getChildren().isEmpty();
    }

    public boolean pathExists(Path path) {
	if (has(path.getFirstElement())) {
	    Object object = get(path.getFirstElement());
	    if (object instanceof Node) {
		Node node = (Node) object;
		return node.pathExists(path.getSubpath(1));
	    } else if (path.isSimple()) {
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }
}
