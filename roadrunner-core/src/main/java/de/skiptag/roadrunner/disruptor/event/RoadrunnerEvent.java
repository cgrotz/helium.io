package de.skiptag.roadrunner.disruptor.event;

import java.util.Date;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventFactory;

public class RoadrunnerEvent extends JSONObject {

    public static final EventFactory<RoadrunnerEvent> EVENT_FACTORY = new EventFactory<RoadrunnerEvent>() {

	@Override
	public RoadrunnerEvent newInstance() {
	    return new RoadrunnerEvent();
	}
    };
    private boolean created;

    public RoadrunnerEvent() {
    }

    public RoadrunnerEvent(String string, String basePath) {
	super(string);
	put("basePath", basePath);
	put("creationDate", new Date());
	Preconditions.checkArgument(has("type"), "No type defined in Event");
	Preconditions.checkArgument(has("basePath"), "No basePath defined in Event");
    }

    public RoadrunnerEvent(String string) {
	super(string);
    }

    public RoadrunnerEvent(RoadrunnerEventType type, String nodePath,
	    Optional<?> value, String basePath) {
	Preconditions.checkArgument(has("type"), "No type defined in Event");
	Preconditions.checkArgument(has("basePath"), "No basePath defined in Event");
	Preconditions.checkArgument(has("nodePath"), "No nodePath defined in Event");
	put("type", type.toString());
	put("path", nodePath);
	if (value.isPresent()) {
	    put("payload", value.get());
	}
	put("basePath", basePath);
	put("creationDate", new Date());
    }

    public String getBasePath() {
	return getString("basePath");
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
	return RoadrunnerEventType.valueOf(((String) get("type")).toUpperCase());
    }

    public String extractNodePath() {
	if (!has("path")) {
	    return null;
	}
	String basePath = getBasePath();
	String requestPath = (String) get("path");

	return extractPath(basePath, requestPath);
    }

    public static String extractPath(String basePath, String requestPath) {
	String result = requestPath;
	int basePathLength = basePath.length();

	int indexOfBasePath = result.indexOf(basePath);
	if (indexOfBasePath > -1) {
	    result = result.substring(indexOfBasePath + basePathLength);
	}

	return result.startsWith("/") ? result : "/" + result;
    }

    public JSONObject getOldValue() {
	return (JSONObject) get("oldValue");
    }

    public void setBasePath(String basePath) {
	put("basePath", basePath);
    }

    public boolean isFromHistory() {
	if (has("fromHistory")) {
	    return getBoolean("fromHistory");
	} else {
	    return false;
	}
    }

    public void setFromHistory(boolean fromHistory) {
	put("fromHistory", fromHistory);
    }

    public Date getCreationDate() {
	return new Date(getLong("creationDate"));
    }

    public void setCreationDate(Date creationDate) {
	put("creationDate", creationDate.getTime());
    }

    public boolean created() {
	return created;
    }

    public void created(boolean created) {
	this.created = created;
    }
}
