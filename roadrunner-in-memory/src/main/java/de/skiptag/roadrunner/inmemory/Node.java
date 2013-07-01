package de.skiptag.roadrunner.inmemory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.collect.Sets;

import de.skiptag.roadrunner.core.Path;

public class Node extends JSONObject {

    public Node getNodeForPath(Path path) throws JSONException {
	Node node;
	if (has(path.getFirstElement())) {
	    node = (Node) get(path.getFirstElement());
	} else {
	    node = new Node();
	    put(path.getFirstElement(), node);
	}

	if (path.isSimple()) {
	    return node;
	} else {
	    return node.getNodeForPath(path.getSubpath(1));
	}
    }

    public void populate(JSONObject payload) throws JSONException {
	populate(payload.toString());
    }

    public void populate(String obj) throws JSONException {
	JSONTokener x = new JSONTokener(obj);
	char c;
	String key;

	if (x.nextClean() != '{') {
	    throw x.syntaxError("A JSONObject text must begin with '{'");
	}
	for (;;) {
	    c = x.nextClean();
	    switch (c) {
	    case 0:
		throw x.syntaxError("A JSONObject text must end with '}'");
	    case '}':
		return;
	    default:
		x.back();
		key = x.nextValue().toString();
	    }

	    // The key is followed by ':'.

	    c = x.nextClean();
	    if (c != ':') {
		throw x.syntaxError("Expected a ':' after a key");
	    }
	    this.putOnce(key, x.nextValue());

	    // Pairs are separated by ','.

	    switch (x.nextClean()) {
	    case ';':
	    case ',':
		if (x.nextClean() == '}') {
		    return;
		}
		x.back();
		break;
	    case '}':
		return;
	    default:
		throw x.syntaxError("Expected a ',' or '}'");
	    }
	}
    }

    public Collection<Node> getChildren() throws JSONException {
	Set<Node> nodes = Sets.newHashSet();
	Iterator<?> itr = keys();
	while (itr.hasNext()) {
	    Object key = itr.next();
	    if (get((String) key) instanceof Node) {
		nodes.add((Node) get((String) key));
	    }
	}
	return nodes;
    }

    public boolean hasChildren() throws JSONException {
	return !getChildren().isEmpty();
    }
}
