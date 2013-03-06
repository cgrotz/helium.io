package org.roadrunner.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.version.VersionException;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.infinispan.schematic.document.ParsingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.jcr.ConfigurationException;
import org.roadrunner.server.data.DataListener;
import org.roadrunner.server.data.DataService;
import org.roadrunner.server.data.RepositoryService;

import com.google.common.base.Strings;

public class RoadrunnerMessageInbound extends MessageInbound implements DataListener {
	private static Set<RoadrunnerMessageInbound> connections = new HashSet<RoadrunnerMessageInbound>();

	private String repositoryName;

	private String servletPath;

	private DataService dataService;;

	public RoadrunnerMessageInbound(String servletPath, String path)
			throws ParsingException, ConfigurationException, LoginException,
			RepositoryException, FileNotFoundException {
		this.servletPath = servletPath;

		this.repositoryName = path.indexOf("/") > -1 ? path.substring(0,
				path.indexOf("/")) : path;

		connections.add(this);
		dataService = RepositoryService.getInstance().getDataService(repositoryName);
		dataService.setListener(this);
	}

	@Override
	protected void onBinaryMessage(ByteBuffer message) throws IOException {
		throw new UnsupportedOperationException("Binary message not supported.");
	}

	@Override()
	protected void onTextMessage(CharBuffer msg) throws IOException {
		try {
			JSONObject message = new JSONObject(msg.toString());
			String messageType = (String) message.get("type");
			String path = ((String) message.get("path")).substring((((String) message.get("path")).indexOf(servletPath)) + servletPath.length()+repositoryName.length() + 1);
			if ("push".equalsIgnoreCase(messageType)) {
				JSONObject payload = (JSONObject) message.get("payload");
				String nodeName = UUID.randomUUID().toString().replaceAll("-", "");
				if(Strings.isNullOrEmpty(path))
				{
					dataService.update(nodeName,payload);
				}
				else
				{
					dataService.update(path+"/"+nodeName, payload);
				}
			} else if ("set".equalsIgnoreCase(messageType)) {
				Object payload = message.get("payload");
				if (payload instanceof JSONObject) {
					if(Strings.isNullOrEmpty(path))
					{
						dataService.update(null,(JSONObject)payload);
					}
					else
					{
						dataService.update(path, (JSONObject)payload);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void broadcast(String message) throws IOException {
		for (RoadrunnerMessageInbound connection : connections) {
			CharBuffer buffer = CharBuffer.wrap(message);
			connection.getWsOutbound().writeTextMessage(buffer);
		}
	}

	public void value(JSONObject dataSnapshot) throws JSONException,
			IOException {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "value");
		broadcast.put("payload", dataSnapshot);
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	}

	public void child_added(String name, String path, String parent, JSONObject node, String prevChildName)
			throws JSONException, IOException, RepositoryException {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_added");
		broadcast.put("name", name);
		broadcast.put("path", path);
		broadcast.put("parent", parent);
		broadcast.put("payload", node);
		broadcast.put("prevChildName", prevChildName);
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	}

	public void child_removed(JSONObject oldChildSnapshot)
			throws JSONException, IOException {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_removed");
		broadcast.put("payload", oldChildSnapshot);
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	}

	public void child_changed(JSONObject childSnapshot, String prevChildName)
			throws JSONException, IOException {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_changed");
		broadcast.put("payload", childSnapshot);
		broadcast.put("prevChildName", prevChildName);
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	}

	public void child_moved(JSONObject childSnapshot, String prevChildName)
			throws JSONException, IOException {
		JSONObject broadcast = new JSONObject();
		broadcast.put("type", "child_moved");
		broadcast.put("payload", childSnapshot);
		broadcast.put("prevChildName", prevChildName);
		getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
	}

	@Override
	protected void onClose(int status) {
		super.onClose(status);
		dataService.logout();
	}

	@Override
	protected void onOpen(WsOutbound outbound) {
		super.onOpen(outbound);
		dataService.sync();
	}
}
