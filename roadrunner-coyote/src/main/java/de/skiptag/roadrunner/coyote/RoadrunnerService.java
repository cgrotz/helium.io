package de.skiptag.roadrunner.coyote;

import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.roadrunner.core.DataListener;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataService.QueryCallback;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public class RoadrunnerService implements DataListener {

	private static final Logger logger = LoggerFactory
			.getLogger(RoadrunnerService.class);
	private AuthorizationService authorizationService;
	private DataService dataService;

	private String path;
	private Map<String, Function> once = Maps.newHashMap();
	private Map<String, Function> on = Maps.newHashMap();
	private String contextName;
	private Map<Function, Coyote> coyoteHolder = Maps.newHashMap();

	public RoadrunnerService(AuthorizationService authorizationService,
			DataService dataService, String contextName, String path) {
		this.authorizationService = authorizationService;
		this.dataService = dataService;
		dataService.addListener(this);
		this.contextName = contextName;
		this.path = path.replaceFirst("^[^#]*?://.*?(/.*)$", "");
		if (contextName != null) {
			this.path = this.path.substring(contextName.length());
		}
	}

	public RoadrunnerService child(String childname) {
		return new RoadrunnerService(authorizationService, dataService,
				contextName, path + "/" + childname);
	}

	@Override
	public void child_added(String name, String path, String parent,
			JSONObject node, String prevChildName, boolean hasChildren,
			long numChildren) {
		try {
			if (on.containsKey("child_added")) {
				Function func = on.get("child_added");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, node, parent, numChildren, prevChildName,
								hasChildren, 0) });

			}
			if (once.containsKey("child_added")) {
				Function func = on.get("child_added");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, node, parent, numChildren, prevChildName,
								hasChildren, 0) });
				once.remove("child_added");
			}
		} catch (Exception exp) {
			logger.error("", exp);
		}
	}

	@Override
	public void child_changed(String name, String path, String parent,
			JSONObject node, String prevChildName, boolean hasChildren,
			long numChildren) {
		try {
			if (on.containsKey("child_changed")) {
				Function func = on.get("child_changed");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, node, parent, numChildren, prevChildName,
								hasChildren, 0) });

			}
			if (once.containsKey("child_changed")) {
				Function func = on.get("child_changed");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, node, parent, numChildren, prevChildName,
								hasChildren, 0) });
				once.remove("child_changed");
			}
		} catch (Exception exp) {
			logger.error("", exp);
		}
	}

	@Override
	public void child_moved(JSONObject childSnapshot, String prevChildName,
			boolean hasChildren, long numChildren) {
		try {
			if (on.containsKey("child_moved")) {
				Function func = on.get("child_moved");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, childSnapshot, null, numChildren,
								prevChildName, hasChildren, 0) });

			}
			if (once.containsKey("child_moved")) {
				Function func = on.get("child_moved");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, childSnapshot, null, numChildren,
								prevChildName, hasChildren, 0) });
				once.remove("child_moved");
			}
		} catch (Exception exp) {
			logger.error("", exp);
		}
	}

	@Override
	public void child_removed(String path, JSONObject payload) {
		try {
			if (on.containsKey("child_removed")) {
				Function func = on.get("child_removed");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, payload, null, 0, null, false, 0) });

			}
			if (once.containsKey("child_removed")) {
				Function func = on.get("child_removed");
				coyoteHolder.get(func).evalFunction(
						func,
						new Object[] { new RoadrunnerSnapshot(
								authorizationService, dataService, contextName,
								path, payload, null, 0, null, false, 0) });
				once.remove("child_removed");
			}
		} catch (Exception exp) {
			logger.error("", exp);
		}
	}

	public String name() {
		return dataService.getName(path);
	}

	public void off(String eventType, Function function) {
		on.remove(eventType);
		once.remove(eventType);
	}

	public void on(String eventType, Function function) {
		on.put(eventType, function);
		coyoteHolder.put(function, Coyote.get());
	}

	public void once(String eventType, Function function) {
		once.put(eventType, function);
		coyoteHolder.put(function, Coyote.get());
	}

	public RoadrunnerService parent() {
		return new RoadrunnerService(authorizationService, dataService,
				contextName, dataService.getParent(path));
	}

	public RoadrunnerService push(org.mozilla.javascript.NativeObject data) {
		try {
			String name = UUID.randomUUID().toString().replaceAll("-", "");
			dataService.update((path.endsWith("/") ? path : path + "/") + name,
					toJSONObject(data));

			return new RoadrunnerService(authorizationService, dataService,
					contextName, (path.endsWith("/") ? path : path + "/")
							+ name);
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	public void query(String expression, final Function function) {
		dataService.query(expression, new QueryCallback() {
			@Override
			public void change(String path, JSONObject value,
					String parentPath, long numChildren, String name,
					boolean hasChildren, int priority) {
				RoadrunnerSnapshot snap = new RoadrunnerSnapshot(
						authorizationService, dataService, name, path, value,
						parentPath, numChildren, name, hasChildren, priority);
				Coyote.get().evalFunction(function, new Object[] { snap });
			}

		});
	}

	public RoadrunnerService root() {
		return new RoadrunnerService(authorizationService, dataService,
				contextName, "/");

	}

	public RoadrunnerService set(org.mozilla.javascript.NativeObject data) {
		try {
			dataService.update(path, toJSONObject(data));
			return this;
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	private JSONObject toJSONObject(NativeObject data) throws JSONException {
		JSONObject result = new JSONObject();
		for (Object keyObj : data.keySet()) {
			String key = (String) keyObj;
			Object value = data.get(key);
			if (value instanceof NativeObject) {
				result.put(key, toJSONObject((NativeObject) value));
			} else {
				result.put(key, value);
			}
		}
		return result;
	}

	public RoadrunnerService update(org.mozilla.javascript.NativeObject data) {
		try {
			dataService.update(path, toJSONObject(data));
			return this;
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}
}
