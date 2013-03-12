package org.roadrunner.server;

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
import org.apache.catalina.websocket.WsOutbound;
import org.infinispan.schematic.document.ParsingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.jcr.ConfigurationException;
import org.roadrunner.core.DataListener;
import org.roadrunner.core.DataService;
import org.roadrunner.core.DataServiceCreationException;
import org.roadrunner.core.DataServiceFactory;
import org.roadrunner.core.dtos.InitMessage;
import org.roadrunner.core.dtos.PushedMessage;

import com.google.common.base.Strings;

public class RoadrunnerMessageInbound extends MessageInbound implements
		DataListener {
	private static Set<RoadrunnerMessageInbound> connections = new HashSet<RoadrunnerMessageInbound>();

	private String servletPath;

	private String repositoryName;

	private DataService dataService;

	public RoadrunnerMessageInbound(String servletPath, String path,
			DataServiceFactory dataServiceFactory) throws ParsingException,
			ConfigurationException, LoginException, RepositoryException,
			FileNotFoundException, DataServiceCreationException {
		this.servletPath = servletPath;

		this.repositoryName = path.indexOf("/") > -1 ? path.substring(0,
				path.indexOf("/")) : path;

		connections.add(this);
		dataService = dataServiceFactory.getDataService(repositoryName);
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
			String path = ((String) message.get("path"))
					.substring((((String) message.get("path"))
							.indexOf(servletPath))
							+ servletPath.length()
							+ repositoryName.length() + 1);
			if ("init".equalsIgnoreCase(messageType)) {
				
				InitMessage init = dataService.init(path);
				
				JSONObject broadcast = new JSONObject();
				broadcast.put("type", "init");
				broadcast.put("name", init.getName());
				broadcast.put("parentPath", init.getParentPath());
				broadcast.put("rootPath", init.getRootPath());
				getWsOutbound().writeTextMessage(
						CharBuffer.wrap(broadcast.toString()));
			}
			else if ("push".equalsIgnoreCase(messageType)) {
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
					broadcast.put("path", path+"/"+nodeName);
					broadcast.put("parent", pushed.getParent());
					broadcast.put("payload", pushed.getPayload());
					broadcast.put("prevChildName", pushed.getPrevChildName());
					broadcast.put("hasChildren", pushed.getHasChildren());
					broadcast.put("numChildren", pushed.getNumChildren());
					getWsOutbound().writeTextMessage(
							CharBuffer.wrap(broadcast.toString()));
				}
			} else if ("set".equalsIgnoreCase(messageType)) {
				JSONObject payload;
				if (message.has("payload")) {
					payload = (JSONObject) message.get("payload");
				} else {
					payload = new JSONObject();
				}
				if (payload instanceof JSONObject) {
					if (Strings.isNullOrEmpty(path)) {
						dataService.update(null, (JSONObject) payload);
					} else {
						dataService.update(path, (JSONObject) payload);
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

	public void child_added(String name, String path, String parent,
			JSONObject node, String prevChildName, boolean hasChildren, long numChildren) {
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

			getWsOutbound().writeTextMessage(
					CharBuffer.wrap(broadcast.toString()));
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	public void child_removed(JSONObject oldChildSnapshot) {
		try {
			JSONObject broadcast = new JSONObject();
			broadcast.put("type", "child_removed");
			broadcast.put("payload", oldChildSnapshot);
			getWsOutbound().writeTextMessage(
					CharBuffer.wrap(broadcast.toString()));
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	public void child_changed(String name, String path, String parent, JSONObject node, String prevChildName, boolean hasChildren, long numChildren) {
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
			getWsOutbound().writeTextMessage(
					CharBuffer.wrap(broadcast.toString()));
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	public void child_moved(JSONObject childSnapshot, String prevChildName, boolean hasChildren, long numChildren) {
		try {
			JSONObject broadcast = new JSONObject();
			broadcast.put("type", "child_moved");
			broadcast.put("payload", childSnapshot);
			broadcast.put("prevChildName", prevChildName);
			broadcast.put("hasChildren", hasChildren);
			broadcast.put("numChildren", numChildren);
			getWsOutbound().writeTextMessage(
					CharBuffer.wrap(broadcast.toString()));
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
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

	@Override
	protected void onOpen(WsOutbound outbound) {
		super.onOpen(outbound);
		try {
			dataService.sync();
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}
}
