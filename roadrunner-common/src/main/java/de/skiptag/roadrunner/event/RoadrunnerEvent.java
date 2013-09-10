package de.skiptag.roadrunner.event;

import com.google.common.base.Optional;

import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.changelog.ChangeLog;
import de.skiptag.roadrunner.json.JSONTokener;
import de.skiptag.roadrunner.json.Node;

/**
 * 
 * Abstraction for Roadrunner Events. Has helper methods for accessing the underlying JSON data.
 * 
 * 
 * @author Christoph Grotz
 * 
 */
public class RoadrunnerEvent extends Node {

	/**
	 * Payload of the event
	 */
	public static final String	PAYLOAD				= "payload";

	/**
	 * Timestamp when the event was created
	 */
	public static final String	CREATION_DATE	= "creationDate";

	/**
	 * Type of the Event {@link RoadrunnerEventType}
	 */
	public static final String	TYPE					= "type";

	/**
	 * {@link Path} for the event
	 */
	public static final String	PATH					= "path";

	/**
	 * Was this event read from the history
	 */
	public static final String	FROM_HISTORY	= "fromHistory";

	/**
	 * Authentication associated with the event
	 */
	public static final String	AUTH					= "auth";

	/**
	 * Name of the data element
	 */
	public static final String	NAME					= "name";

	/**
	 * Priority to be set on the element at the path
	 */
	public static final String	PRIORITY			= "priority";

	/**
	 * Previous Child name
	 */
	public static final String	PREVCHILDNAME	= "prevChildName";

	private ChangeLog						changeLog			= new ChangeLog();

	/**
	 * Creates an empty event
	 */
	public RoadrunnerEvent() {
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	/**
	 * 
	 * Creates an Node from the given json string
	 * 
	 * @param json
	 *          data as {@link String}
	 */
	public RoadrunnerEvent(String json) {
		super(json);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(RoadrunnerEventType type, String path, Object payload) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, path);
		put(RoadrunnerEvent.PAYLOAD, payload);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	/**
	 * @param type
	 *          of the event {@link RoadrunnerEventType}
	 * @param path
	 *          of the event
	 * @param payload
	 *          Optional playload of the event
	 */
	public RoadrunnerEvent(RoadrunnerEventType type, String path, Optional<?> payload) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, path);
		if (payload.isPresent()) {
			put(RoadrunnerEvent.PAYLOAD, payload.get());
		}
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(RoadrunnerEventType type, String path, Object data, Integer priority) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, path);
		put(RoadrunnerEvent.PAYLOAD, data);
		put(RoadrunnerEvent.PRIORITY, priority);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(RoadrunnerEventType type, String path, Integer priority) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, path);
		put(RoadrunnerEvent.PRIORITY, priority);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	public RoadrunnerEvent(RoadrunnerEventType type, String path) {
		put(RoadrunnerEvent.TYPE, type.toString());
		put(RoadrunnerEvent.PATH, path);
		put(RoadrunnerEvent.CREATION_DATE, System.currentTimeMillis());
	}

	/**
	 * Populates the instance with the handed json data
	 * 
	 * @param json
	 */
	public void populate(String json) {
		JSONTokener x = new JSONTokener(json);
		char c;
		String key;

		if (x.nextClean() != '{') {
			throw x.syntaxError("A JSONObject text must begin with '{'");
		}
		for (;;) {
			c = x.nextClean();
			switch (c)
				{
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

			switch (x.nextClean())
				{
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

	public Path extractNodePath() {
		if (!has(RoadrunnerEvent.PATH)) {
			return null;
		}
		String requestPath = (String) get(RoadrunnerEvent.PATH);
		if (has(RoadrunnerEvent.NAME)) {
			if (get(RoadrunnerEvent.NAME) != Node.NULL) {
				return new Path(extractPath(requestPath)).append(getString(RoadrunnerEvent.NAME));
			}
		}
		return new Path(extractPath(requestPath));
	}

	public String extractParentPath() {
		if (!has(RoadrunnerEvent.PATH)) {
			return null;
		}
		String parentPath;
		String requestPath = (String) get(RoadrunnerEvent.PATH);
		if (has(RoadrunnerEvent.NAME) && get(RoadrunnerEvent.NAME) != Node.NULL) {
			parentPath = extractPath(requestPath) + "/" + getString(RoadrunnerEvent.NAME);
		} else {
			parentPath = extractPath(requestPath);
		}
		return parentPath.substring(0, parentPath.lastIndexOf("/"));

	}

	public RoadrunnerEventType getType() {
		return RoadrunnerEventType.valueOf(((String) get(RoadrunnerEvent.TYPE)).toUpperCase());
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
		if (!has(AUTH)) {
			return new Node();
		}
		return getNode(AUTH);
	}

	public void setAuth(Node auth) {
		put(AUTH, auth);
	}

	/**
	 * @param url
	 *          of the element
	 * @return the path representation
	 */
	public static String extractPath(String url) {
		String result = url;
		if (url.startsWith("http://")) {
			if (url.indexOf("/", 7) != -1) {
				result = url.substring(url.indexOf("/", 7));
			} else {
				result = "";
			}
		} else if (url.startsWith("https://")) {
			if (url.indexOf("/", 8) != -1) {
				result = url.substring(url.indexOf("/", 8));
			} else {
				result = "";
			}
		}

		return result.startsWith("/") ? result : "/" + result;
	}

	@Override
	public void clear() {
		super.clear();
		getChangeLog().clear();
	}

	public RoadrunnerEvent copy() {
		return new RoadrunnerEvent(toString());
	}
}
