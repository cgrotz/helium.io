package de.skiptag.roadrunner.disruptor.event;

import java.util.Date;

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

    public RoadrunnerEvent() {
    }

    public RoadrunnerEvent(String string, String basePath, String repositoryName)
	    throws JSONException {
	super(string);
	put("basePath", basePath);
	put("repositoryName", repositoryName);
	put("creationDate", new Date());
    }

    public RoadrunnerEvent(String string) throws JSONException {
	super(string);
    }

    public String getRepositoryName() throws JSONException {
	return getString("repositoryName");
    }

    public String getBasePath() throws JSONException {
	return getString("basePath");
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

    public RoadrunnerEventType getType() throws JSONException {
	return RoadrunnerEventType.valueOf(((String) get("type")).toUpperCase());
    }

    public String extractNodePath() throws JSONException {
	if (!has("path")) {
	    return null;
	}
	int pathLength = getBasePath().length();
	int repositoryNameLength = getRepositoryName().length();

	String requestPath = (String) get("path");
	int indexOfPath = requestPath.indexOf(getBasePath());
	if (indexOfPath > -1) {
	    int substringIndex = indexOfPath + pathLength
		    + repositoryNameLength + 1;
	    return requestPath.substring(substringIndex)
		    .replaceFirst("roadrunner", "");
	} else {
	    return requestPath.replaceFirst("roadrunner", "");
	}
    }

    public JSONObject getOldValue() throws JSONException {
	return (JSONObject) get("oldValue");
    }

    public void setBasePath(String basePath) throws JSONException {
	put("basePath", basePath);
    }

    public void setRepositoryName(String repositoryName) throws JSONException {
	put("repositoryName", repositoryName);
    }

    public boolean isFromHistory() throws JSONException {
	if (has("fromHistory")) {
	    return getBoolean("fromHistory");
	} else {
	    return false;
	}
    }

    public void setFromHistory(boolean fromHistory) throws JSONException {
	put("fromHistory", fromHistory);
    }

    public Date getCreationDate() throws JSONException {
	return new Date(getLong("creationDate"));
    }

    public void setCreationDate(Date creationDate) throws JSONException {
	put("creationDate", creationDate.getTime());
    }
}
