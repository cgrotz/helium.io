package org.roadrunner.coyote;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.roadrunner.core.DataListener;
import org.roadrunner.core.DataService;
import org.roadrunner.core.DataServiceCreationException;
import org.roadrunner.core.authorization.AuthorizationService;
import org.roadrunner.core.dtos.PushedMessage;
import org.roadrunner.modeshape.ModeShapeServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.http.HttpServerRequest;
import de.skiptag.coyote.api.modules.Module.ServletModule;
import de.skiptag.coyote.api.modules.Module.WebsocketModule;

public class RoadrunnerModule extends WebsocketModule implements ServletModule,
		DataListener {

	private String servletPath;

	private String repositoryName = "";

	private DataService dataService;

	private Set<String> attached_listeners = Sets.newHashSet();

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(RoadrunnerModule.class);

	private AuthorizationService authorizationService;

	private String path;

	public RoadrunnerModule(Coyote coyote, String path, String name) {
		super(path, coyote);
		this.path = path;
		this.servletPath = name;
		try {
			authorizationService = ModeShapeServiceFactory.getInstance()
					.getAuthorizationService(servletPath);
			dataService = ModeShapeServiceFactory.getInstance().getDataService(
					authorizationService, servletPath);
			dataService.addListener(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void child_added(String name, String path, String parent,
			JSONObject node, String prevChildName, boolean hasChildren,
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
				send(broadcast.toString());
			}
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	@Override
	public void child_changed(String name, String path, String parent,
			JSONObject node, String prevChildName, boolean hasChildren,
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
			send(broadcast.toString());
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
			send(broadcast.toString());
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	@Override
	public void child_removed(String path, JSONObject payload) {
		try {
			JSONObject broadcast = new JSONObject();
			broadcast.put("type", "child_removed");
			broadcast.put("path", path);
			broadcast.put("payload", payload);
			send(broadcast.toString());
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	@Override
	public void destroy() {
		ModeShapeServiceFactory.getInstance().destroy();
	}

	private String extractPath(JSONObject message) throws JSONException {
		int servletPathLength = servletPath.length();
		int repositoryNameLength = repositoryName.length();
		String path = (String) message.get("path");
		int indexOfServletPath = path.indexOf(servletPath);
		if (indexOfServletPath > -1) {
			int substringIndex = indexOfServletPath + servletPathLength
					+ repositoryNameLength + 1;
			return path.substring(substringIndex)
					.replaceFirst("roadrunner", "");
		} else {
			return path.replaceFirst("roadrunner", "");
		}
	}

	@Override
	public String getServletPath() {
		return "roadrunner.js";
	}

	@Override
	public String getWebsocketPath() {
		return path;
	}

	@Override
	public void handle(HttpServerRequest req) {
		try {
			req.response.sendFile("roadrunner.js");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handle(String msg) {
		try {
			JSONObject message = new JSONObject(msg);
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
					send(broadcast.toString());
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
					} else if (obj == null || obj == JSONObject.NULL) {
						if (!Strings.isNullOrEmpty(path)) {
							dataService.remove(path);
						}
					} else {
						if (!Strings.isNullOrEmpty(path)) {
							dataService.updateSimpleValue(path, obj);
						}
					}
				} else {
					if (!Strings.isNullOrEmpty(path)) {
						dataService.remove(path);
					}
				}

			}
		} catch (Exception e) {
			throw new RuntimeException(msg.toString(), e);
		}
	}

	@Override
	public void init() {

	}

	private boolean listenerAttached(String path) {
		for (String listenerPath : attached_listeners) {
			if (path.startsWith(listenerPath)) {
				return true;
			}
		}
		return false;
	}

	public RoadrunnerService load() throws DataServiceCreationException {
		return new RoadrunnerService(authorizationService, dataService, null,
				"/");
	}
}
