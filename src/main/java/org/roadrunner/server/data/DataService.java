package org.roadrunner.server.data;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.version.VersionException;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

public class DataService implements EventListener {

	private Session commonRepo;
	private Session dataRepo;
	private Node rootNode;

	private DataListener listener;

	public DataService(Session commonRepo, Session dataRepo)
			throws UnsupportedRepositoryOperationException, RepositoryException {
		this.dataRepo = dataRepo;
		this.commonRepo = commonRepo;

		int EVENT_MASK = Event.NODE_ADDED | Event.NODE_MOVED
				| Event.NODE_REMOVED | Event.PROPERTY_ADDED
				| Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED;
		dataRepo.getWorkspace()
				.getObservationManager()
				.addEventListener(this, EVENT_MASK, null, true, null, null,
						false);

		rootNode = dataRepo.getRootNode();
	}

	public Node addNode(Node node2, String path2) throws ItemExistsException,
			PathNotFoundException, VersionException,
			ConstraintViolationException, LockException, RepositoryException {
		if (path2.indexOf("/") > -1) {
			Node newNode = node2
					.addNode(path2.substring(0, path2.indexOf("/")));
			dataRepo.save();
			return addNode(newNode, path2.substring(path2.indexOf("/") + 1));
		} else {
			Node childNode = node2.addNode(path2);
			dataRepo.save();
			return childNode;
		}
	}

	@Override
	public void onEvent(EventIterator eventIterator) {
		try {
			while (eventIterator.hasNext()) {
				Event event = eventIterator.nextEvent();
				if (event.getType() == Event.NODE_ADDED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					listener.child_added(node.getName(), node.getPath(), node
							.getParent().getName(), transformToJSON(node),
							getPrevChildName(node));
				} else if (event.getType() == Event.NODE_MOVED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					JSONObject childSnapshot = transformToJSON(node);
					listener.child_moved(childSnapshot, getPrevChildName(node));
				} else if (event.getType() == Event.NODE_REMOVED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					JSONObject childSnapshot = transformToJSON(node);
					listener.child_removed(childSnapshot);
				} else if (event.getType() == Event.PROPERTY_ADDED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					JSONObject childSnapshot = transformToJSON(node);
					listener.child_moved(childSnapshot, getPrevChildName(node));
				} else if (event.getType() == Event.PROPERTY_CHANGED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					JSONObject childSnapshot = transformToJSON(node);
					listener.child_moved(childSnapshot, getPrevChildName(node));
				} else if (event.getType() == Event.PROPERTY_REMOVED) {
					Node node = dataRepo.getNodeByIdentifier(event
							.getIdentifier());
					JSONObject childSnapshot = transformToJSON(node);
					listener.child_moved(childSnapshot, getPrevChildName(node));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private JSONObject transformToJSON(Node node) throws ValueFormatException,
			RepositoryException, JSONException {
		JSONObject object = new JSONObject();
		PropertyIterator itr = node.getProperties();
		while (itr.hasNext()) {
			Property property = itr.nextProperty();
			if (property.getValue().getType() == PropertyType.BINARY) {
				// object.put(property, property.getBinary().);
			} else if (property.getValue().getType() == PropertyType.BOOLEAN) {
				object.put(property.getName(), property.getBoolean());
			} else if (property.getValue().getType() == PropertyType.DATE) {
				object.put(property.getName(), new Date(property.getDate()
						.getTimeInMillis()));
			} else if (property.getValue().getType() == PropertyType.DECIMAL) {
				object.put(property.getName(), property.getDecimal());
			} else if (property.getValue().getType() == PropertyType.DOUBLE) {
				object.put(property.getName(), property.getDouble());
			} else if (property.getValue().getType() == PropertyType.LONG) {
				object.put(property.getName(), property.getLong());
			} else if (property.getValue().getType() == PropertyType.NAME) {
			} else if (property.getValue().getType() == PropertyType.PATH) {
			} else if (property.getValue().getType() == PropertyType.REFERENCE) {
			} else if (property.getValue().getType() == PropertyType.STRING) {
				object.put(property.getName(), property.getString());
			} else if (property.getValue().getType() == PropertyType.UNDEFINED) {
			} else if (property.getValue().getType() == PropertyType.URI) {
			} else if (property.getValue().getType() == PropertyType.WEAKREFERENCE) {
			}
		}
		return object;
	}

	private void updateNode(JSONObject object, Node node)
			throws ValueFormatException, RepositoryException, JSONException {
		Iterator<?> itr = object.keys();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			Object value = object.get(key);
			node.setProperty(key, "" + value);
		}
	}

	private String getPrevChildName(Node node) throws RepositoryException,
			PathNotFoundException, ItemNotFoundException, AccessDeniedException {
		String prevChildName;
		if (node.getIndex() == 1) {
			prevChildName = null;
		} else {
			prevChildName = node.getParent()
					.getNode("*[" + (node.getIndex() - 1) + "]").getName();
		}
		return prevChildName;
	}

	public void sync() {
		// TODO Auto-generated method stub

	}

	private void sendNode(Node node) throws RepositoryException, JSONException,
			IOException {
		NodeIterator itr = node.getNodes();
		while (itr.hasNext()) {
			Node childNode = itr.nextNode();
			if (!childNode.getName().equals("jcr:system")) {
				listener.child_added(childNode.getName(), childNode.getPath(),
						childNode.getParent().getName(),
						transformToJSON(childNode), null);
			}
		}
	}

	public void logout() {
		dataRepo.logout();
		commonRepo.logout();
	}

	public void update(String path, JSONObject payload)
			throws AccessDeniedException, ItemExistsException,
			ReferentialIntegrityException, ConstraintViolationException,
			InvalidItemStateException, VersionException, LockException,
			NoSuchNodeTypeException, RepositoryException, JSONException {
		Node node;
		if (!Strings.isNullOrEmpty(path)) {
			if (rootNode.hasNode(path)) {
				node = rootNode.getNode(path);
			} else {
				node = rootNode.addNode(path);
			}
		}
		else
		{
			node = rootNode;
		}
		updateNode(payload, node);
		dataRepo.save();
	}

	public void setListener(DataListener listener) {
		this.listener = listener;
	}
}
