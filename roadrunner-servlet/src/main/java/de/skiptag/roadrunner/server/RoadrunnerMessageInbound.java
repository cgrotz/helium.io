package de.skiptag.roadrunner.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.apache.catalina.websocket.MessageInbound;
import org.infinispan.schematic.document.ParsingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.jcr.ConfigurationException;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.core.DataListener;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataServiceCreationException;
import de.skiptag.roadrunner.core.DataServiceFactory;
import de.skiptag.roadrunner.core.authorization.AuthenticationServiceFactory;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.dtos.PushedMessage;

public class RoadrunnerMessageInbound extends MessageInbound implements
	DataListener {
    private static Set<RoadrunnerMessageInbound> connections = new HashSet<RoadrunnerMessageInbound>();

    private String servletPath;

    private String repositoryName;

    private DataService dataService;

    private AuthorizationService authorizationService;

    private Set<String> attached_listeners = Sets.newHashSet();

    public RoadrunnerMessageInbound(String servletPath, String path,
	    DataServiceFactory dataServiceFactory,
	    AuthenticationServiceFactory authenticationServiceFactory)
	    throws ParsingException, ConfigurationException, LoginException,
	    RepositoryException, FileNotFoundException,
	    DataServiceCreationException {
	this.servletPath = servletPath;

	this.repositoryName = path.indexOf("/") > -1 ? path.substring(0, path.indexOf("/"))
		: path;

	connections.add(this);
	authorizationService = authenticationServiceFactory.getAuthorizationService(new JSONObject());
	dataService = dataServiceFactory.getDataService(authorizationService, repositoryName);
	dataService.addListener(this);
    }

    private void broadcast(String message) throws IOException {
	for (RoadrunnerMessageInbound connection : connections) {
	    CharBuffer buffer = CharBuffer.wrap(message);
	    connection.getWsOutbound().writeTextMessage(buffer);
	}
    }

    @Override
    public void child_added(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_added");
	    broadcast.put("name", name);
	    broadcast.put("path", path);
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    if (listenerAttached(path)) {
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	    }
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_changed(String name, String path, String parent,
	    Object node, String prevChildName, boolean hasChildren,
	    long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_changed");
	    broadcast.put("name", name);
	    broadcast.put("path", path);
	    broadcast.put("parent", parent);
	    broadcast.put("payload", node);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_moved(JSONObject childSnapshot, String prevChildName,
	    boolean hasChildren, long numChildren) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_moved");
	    broadcast.put("payload", childSnapshot);
	    broadcast.put("prevChildName", prevChildName);
	    broadcast.put("hasChildren", hasChildren);
	    broadcast.put("numChildren", numChildren);
	    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override
    public void child_removed(String path, Object payload) {
	try {
	    JSONObject broadcast = new JSONObject();
	    broadcast.put("type", "child_removed");
	    broadcast.put("path", path);
	    broadcast.put("payload", payload);
	    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    private String extractPath(JSONObject message) throws JSONException {
	int servletPathLength = servletPath.length();
	int repositoryNameLength = repositoryName.length();
	String path = (String) message.get("path");
	int indexOfServletPath = path.indexOf(servletPath);
	if (indexOfServletPath > -1) {
	    int substringIndex = indexOfServletPath + servletPathLength
		    + repositoryNameLength + 1;
	    return path.substring(substringIndex);
	} else {
	    return path;
	}
    }

    private boolean listenerAttached(String path) {
	for (String listenerPath : attached_listeners) {
	    if (path.startsWith(listenerPath)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    protected void onBinaryMessage(ByteBuffer message) throws IOException {
	throw new UnsupportedOperationException("Binary message not supported.");
    }

    @Override
    protected void onClose(int status) {
	super.onClose(status);
	try {
	    dataService.shutdown();
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override()
    protected void onTextMessage(CharBuffer msg) throws IOException {
	try {
	    JSONObject message = new JSONObject(msg.toString());
	    String messageType = (String) message.get("type");
	    String path = extractPath(message);

	    if ("attached_listener".equalsIgnoreCase(messageType)) {
		attached_listeners.add(path);
		dataService.sync(path);
	    } else if ("detached_listener".equalsIgnoreCase(messageType)) {
		attached_listeners.remove(path);
	    } else if ("push".equalsIgnoreCase(messageType)) {
		JSONObject payload;
		if (message.has("payload")) {
		    payload = (JSONObject) message.get("payload");
		} else {
		    payload = new JSONObject();
		}
		String nodeName;
		if (message.has("name")) {
		    nodeName = message.getString("name");
		} else {
		    nodeName = UUID.randomUUID().toString().replaceAll("-", "");
		}
		PushedMessage pushed;
		if (Strings.isNullOrEmpty(path)) {
		    pushed = dataService.update(nodeName, payload);
		} else {
		    pushed = dataService.update(path + "/" + nodeName, payload);
		}
		{
		    JSONObject broadcast = new JSONObject();
		    broadcast.put("type", "pushed");
		    broadcast.put("name", nodeName);
		    broadcast.put("path", path + "/" + nodeName);
		    broadcast.put("parent", pushed.getParent());
		    broadcast.put("payload", pushed.getPayload());
		    broadcast.put("prevChildName", pushed.getPrevChildName());
		    broadcast.put("hasChildren", pushed.getHasChildren());
		    broadcast.put("numChildren", pushed.getNumChildren());
		    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
		}
	    } else if ("set".equalsIgnoreCase(messageType)) {
		JSONObject payload;
		if (message.has("payload")) {
		    Object obj = message.get("payload");
		    if (obj instanceof JSONObject) {
			payload = (JSONObject) obj;
			if (payload instanceof JSONObject) {
			    if (Strings.isNullOrEmpty(path)) {
				dataService.update(null, payload);
			    } else {
				dataService.update(path, payload);
			    }
			}
		    } else if (obj == null) {
			if (!Strings.isNullOrEmpty(path)) {
			    message.put("oldValue", dataService.get(path));
			    dataService.remove(path);
			}
		    } else {
			if (!Strings.isNullOrEmpty(path)) {
			    dataService.updateSimpleValue(path, obj);
			}
		    }
		} else {
		    if (!Strings.isNullOrEmpty(path)) {
			message.put("oldValue", dataService.get(path));
			dataService.remove(path);
		    }
		}

	    }
	} catch (Exception e) {
	    throw new RuntimeException(msg.toString(), e);
	}
    }
}
