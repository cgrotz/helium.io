package de.skiptag.roadrunner.disruptor.event;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.lmax.disruptor.EventFactory;

public class RoadrunnerEvent extends JSONObject {

	public static final EventFactory<RoadrunnerEvent> EVENT_FACTORY = new EventFactory<RoadrunnerEvent>() {

		@Override
		public RoadrunnerEvent newInstance() {
			return new RoadrunnerEvent();
		}
	};

	private String repositoryName;
	private String basePath;

	public RoadrunnerEvent() {
	}

	public RoadrunnerEvent(String string, String basePath, String repositoryName) throws JSONException {
		super(string);
		this.basePath = basePath;
		this.repositoryName = repositoryName;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
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

	public MessageType getType() throws JSONException {
		if (!has("type")) {
			throw new RuntimeException("No type: " + toString());
		}
		return MessageType.valueOf(((String) get("type")).toUpperCase());
	}

	public String extractNodePath() throws JSONException {
		if (!has("path")) {
			return null;
		}
		int pathLength = basePath.length();
		int repositoryNameLength = repositoryName.length();

		String requestPath = (String) get("path");
		int indexOfPath = requestPath.indexOf(basePath);
		if (indexOfPath > -1) {
			int substringIndex = indexOfPath + pathLength + repositoryNameLength + 1;
			return requestPath.substring(substringIndex).replaceFirst("roadrunner", "");
		} else {
			return requestPath.replaceFirst("roadrunner", "");
		}
	}

	public JSONObject getOldValue() throws JSONException {
		return (JSONObject) get("oldValue");
	}
}
