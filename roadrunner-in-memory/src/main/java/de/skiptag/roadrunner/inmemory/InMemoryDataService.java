package de.skiptag.roadrunner.inmemory;

import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Sets;

import de.skiptag.roadrunner.core.DataListener;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.Path;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.dtos.PushedMessage;

public class InMemoryDataService implements DataService {

	private String repositoryName;
	private AuthorizationService authorizationService;

	private Node model = new Node();
	private Set<DataListener> listeners = Sets.newHashSet();

	public InMemoryDataService(AuthorizationService authorizationService,
			String repositoryName) {
		this.authorizationService = authorizationService;
		this.repositoryName = repositoryName;
	}

	@Override
	public JSONObject get(String path) {
		try {
			return model.getNodeForPath(new Path(path));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getName(String path) {
		return new Path(path).getLastElement();
	}

	@Override
	public String getParent(String path) {
		return new Path(path).getParent().toString();
	}

	@Override
	public void query(String expression, QueryCallback queryCallback) {

	}

	@Override
	public void remove(String path) {
		Path nodePath = new Path(path);
		String nodeName = nodePath.getLastElement();
		Path parentPath = nodePath.getParent();

		try {
			model.getNodeForPath(parentPath).remove(nodeName);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sync(String path) {
		try {
			Path nodePath = new Path(path);
			Node node = model.getNodeForPath(nodePath);
			Iterator<?> itr = node.sortedKeys();
			while (itr.hasNext()) {
				Object childNodeKey = itr.next();
				Object object = node.get(childNodeKey.toString());
				if (object instanceof Node) {
					Node childNode = (Node) object;
					fireChildAdded(nodePath.getLastElement(), path, nodePath.getParent().toString(), node,
							null, node.hasChildren(), node.getChildren().size());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public PushedMessage update(String nodeName, JSONObject payload) {
		Path nodePath = new Path(nodeName);
		try {
			Node node = model.getNodeForPath(nodePath);

			node.populate(payload);
			return new PushedMessage(nodePath.getParent().toString(), null,
					node, node.hasChildren(), node.getChildren().size());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void updateSimpleValue(String path, Object obj) {
		Path nodePath = new Path(path);
		try {
			model.getNodeForPath(nodePath.getParent()).put(
					nodePath.getLastElement(), obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addListener(DataListener dataListener) {
		this.listeners.add(dataListener);
	}

	@Override
	public void removeListener(DataListener dataListener) {
		this.listeners.remove(dataListener);
	}

	@Override
	public void shutdown() {

	}

	@Override
	public void setAuth(JSONObject auth) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fireChildAdded(String name, String path, String parentName,
			JSONObject transformToJSON, String prevChildName, boolean hasNodes,
			long size) {
		for (DataListener listener : listeners) {
			listener.child_added(name, path, parentName, transformToJSON,
					prevChildName, hasNodes, size);
		}
	}

	@Override
	public void fireChildChanged(String name, String path, String parentName,
			JSONObject transformToJSON, String prevChildName, boolean hasNodes,
			long size) {
		for (DataListener listener : listeners) {
			listener.child_changed(name, path, parentName, transformToJSON,
					prevChildName, hasNodes, size);
		}
	}

	@Override
	public void fireChildMoved(JSONObject childSnapshot, String prevChildName,
			boolean hasNodes, long size) {
		for (DataListener listener : listeners) {
			listener.child_moved(childSnapshot, prevChildName, hasNodes, size);
		}
	}

	@Override
	public void fireChildRemoved(String path, JSONObject fromRemovedNodes) {
		for (DataListener listener : listeners) {
			listener.child_removed(path, fromRemovedNodes);
		}
	}

}