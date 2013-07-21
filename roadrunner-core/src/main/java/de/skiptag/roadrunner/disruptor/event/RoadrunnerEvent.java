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
	put("creationDate", new Date());
    }

    public RoadrunnerEvent(String string) {
	super(string);
	put("creationDate", new Date());
	Preconditions.checkArgument(has("type"), "No type defined in Event");
    }

    public RoadrunnerEvent(RoadrunnerEventType type, String nodePath,
	    Optional<?> value) {
	Preconditions.checkArgument(has("type"), "No type defined in Event");
	Preconditions.checkArgument(has("nodePath"), "No nodePath defined in Event");
	put("type", type.toString());
	put("path", nodePath);
	if (value.isPresent()) {
	    put("payload", value.get());
	}
	put("creationDate", new Date());
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
	String requestPath = (String) get("path");
	return extractPath(requestPath);
    }

    public static String extractPath(String requestPath) {
	String result = requestPath;
	if (requestPath.startsWith("ws://")) {
	    result = requestPath.substring(requestPath.indexOf("/", 5));
	} else if (requestPath.startsWith("wss://")) {
	    result = requestPath.substring(requestPath.indexOf("/", 6));
	}

	return result.startsWith("/") ? result : "/" + result;
    }

    public JSONObject getOldValue() {
	return (JSONObject) get("oldValue");
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
