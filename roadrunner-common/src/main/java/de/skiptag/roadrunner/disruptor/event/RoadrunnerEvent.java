package de.skiptag.roadrunner.disruptor.event;

import org.json.JSONTokener;
import org.json.Node;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLog;
import de.skiptag.roadrunner.persistence.Path;

public class RoadrunnerEvent extends Node {

	public static final String PAYLOAD = "payload";
	public static final String CREATION_DATE = "creationDate";
	public static final String TYPE = "type";
	public static final String NODE_PATH = "nodePath";
	public static final String PATH = "path";
	public static final String FROM_HISTORY = "fromHistory";
	public static final String AUTH = "auth";
	public static final String NAME = "name";
	public static final String PRIORITY = "priority";
	public static final String PREVCHILDNAME = "prevChildName";

	private ChangeLog changeLog = new ChangeLog();

	public RoadrunnerEvent() {
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(String string) {
		super(string);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(RoadrunnerEventType type, String nodePath, Optional<?> value) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, nodePath);
		if (value.isPresent()) {
			put(RoadrunnerEvent.PAYLOAD, value.get());
		}
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public void populate(String obj) {
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
			this.put(key, x.nextValue());

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

	public RoadrunnerEventType getType() {
		return RoadrunnerEventType.valueOf(((String) get(RoadrunnerEvent.TYPE)).toUpperCase());
	}

	public Path extractNodePath() {
		if (!has(RoadrunnerEvent.PATH)) {
			return null;
		}
		String requestPath = (String) get(RoadrunnerEvent.PATH);
		if (has(RoadrunnerEvent.NAME)) {
			if (get(RoadrunnerEvent.NAME) != Node.NULL) {
				return new Path(extractPath(requestPath, getString(RoadrunnerEvent.NAME)));
			}
		}
		return new Path(extractPath(requestPath, null));
	}

	public String extractParentPath() {
		if (!has(RoadrunnerEvent.PATH)) {
			return null;
		}
		String parentPath;
		String requestPath = (String) get(RoadrunnerEvent.PATH);
		if (has(RoadrunnerEvent.NAME) && get(RoadrunnerEvent.NAME) != Node.NULL) {
			parentPath = extractPath(requestPath, getString(RoadrunnerEvent.NAME));
		} else {
			parentPath = extractPath(requestPath, null);
		}
		return parentPath.substring(0, parentPath.lastIndexOf("/"));

	}

	public static String extractPath(String requestPath, String name) {
		String result = requestPath;
		if (requestPath.startsWith("http://")) {
			result = requestPath.substring(requestPath.indexOf("/", 7));
		} else if (requestPath.startsWith("https://")) {
			result = requestPath.substring(requestPath.indexOf("/", 8));
		}

		String path = result.startsWith("/") ? result : "/" + result;
		if (!Strings.isNullOrEmpty(name)) {
			path = path + "/" + name;
		}
		return path;
	}

	public boolean isFromHistory() {
		if (has(RoadrunnerEvent.FROM_HISTORY)) {
			return getBoolean(RoadrunnerEvent.FROM_HISTORY);
		} else {
			return false;
		}
	}

	public void setFromHistory(boolean fromHistory) {
		put(RoadrunnerEvent.FROM_HISTORY, fromHistory);
	}

	public long getCreationDate() {
		return getLong(RoadrunnerEvent.CREATION_DATE);
	}

	public void setCreationDate(long creationDate) {
		put(RoadrunnerEvent.CREATION_DATE, creationDate);
	}

	public int getPriority() {
		return getInt(RoadrunnerEvent.PRIORITY);
	}

	public void setPriority(int priority) {
		put(RoadrunnerEvent.PRIORITY, priority);
	}

	public boolean hasPriority() {
		return has(RoadrunnerEvent.PRIORITY);
	}

	public Object getPayload() {
		return get(RoadrunnerEvent.PAYLOAD);
	}

	public ChangeLog getChangeLog() {
		return changeLog;
	}

	public Node getAuth() {
		return getNode(AUTH);
	}

	public void setAuth(Node auth) {
		put(AUTH, auth);
	}
}
