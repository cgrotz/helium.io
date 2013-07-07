package de.skiptag.roadrunner.coyote;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.http.common.HttpServerRequest;
import de.skiptag.coyote.api.modules.ServletModule;
import de.skiptag.coyote.api.modules.WebsocketModule;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataServiceCreationException;
import de.skiptag.roadrunner.core.RuleBasedAuthorizationService;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.disruptor.DisruptorRoadrunnerService;
import de.skiptag.roadrunner.disruptor.event.MessageType;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.inmemory.InMemoryServiceFactory;

public class RoadrunnerModule extends WebsocketModule implements ServletModule {

    private String repositoryName = "";

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerModule.class);

    private DataService dataService;

    private AuthorizationService authorizationService;

    public String getRepositoryName() {
	return repositoryName;
    }

    private String path;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private DisruptorRoadrunnerService disruptor;

    public RoadrunnerModule(Coyote coyote, String path, String repoName,
	    NativeObject rule) {
	super(path, coyote);
	this.path = path;
	this.repositoryName = repoName;

	try {
	    authorizationService = new RuleBasedAuthorizationService(
		    RoadrunnerService.toJSONObject(rule));
	    // dataService = ModeShapeServiceFactory.getInstance()
	    // .getDataService(authorizationService, repoName);
	    dataService = InMemoryServiceFactory.getInstance()
		    .getDataService(authorizationService, repoName);
	    roadrunnerEventHandler = new RoadrunnerEventHandler(this);
	    dataService.addListener(roadrunnerEventHandler);

	    Optional<File> absent = Optional.absent();
	    disruptor = new DisruptorRoadrunnerService(new File(
		    "/home/balu/tmp/roadrunner"), absent, dataService,
		    authorizationService, true);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    public void setAuthentication(String token) throws JSONException {
	JSONObject auth = new JSONObject();
	auth.put("id", token);
	dataService.setAuth(auth);
    }

    @Override
    public void init() {

    }

    @Override
    public void handle(String msg) {
	try {

	    RoadrunnerEvent roadrunnerEvent;

	    try {
		roadrunnerEvent = new RoadrunnerEvent(msg, path, repositoryName);
		Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("repositoryName"), "No repositoryName defined in Event");
	    } catch (Exception exp) {
		logger.warn("Error in message (" + exp.getMessage() + "): "
			+ msg);
		roadrunnerEvent = null;

	    }

	    if (roadrunnerEvent.has("type")) {
		if (roadrunnerEvent.getType() == MessageType.ATTACHED_LISTENER) {
		    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
		    dataService.sync(roadrunnerEvent.extractNodePath());
		} else if (roadrunnerEvent.getType() == MessageType.DETACHED_LISTENER) {
		    roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
		} else if (roadrunnerEvent.getType() == MessageType.QUERY) {
		    String query = roadrunnerEvent.getString("query");
		    // queryAction.handle(query);
		} else {
		    disruptor.handleEvent(roadrunnerEvent);
		}
	    }
	} catch (Exception e) {
	    throw new RuntimeException(msg.toString(), e);
	}
    }

    @Override
    public void handle(HttpServerRequest req) {
	try {
	    if ("roadrunner.html".equals(req.uri)) {
		req.response.sendFile("roadrunner.html");
	    } else if ("roadrunner.js".equals(req.uri)) {
		req.response.sendFile("roadrunner.js");
	    } else {
		req.response.setStatusCode(404);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public RoadrunnerService load() throws DataServiceCreationException {
	return new RoadrunnerService(authorizationService, dataService, null,
		"/");
    }

    @Override
    public void destroy() {
	disruptor.shutdown();
    }

    @Override
    public String getServletPath() {
	return "roadrunner(.)*";
    }

    @Override
    public String getWebsocketPath() {
	return path + "/(.)*";
    }

    private String extractPath(JSONObject message) throws JSONException {
	if (!message.has("path")) {
	    return null;
	}
	int pathLength = path.length();
	int repositoryNameLength = repositoryName.length();

	String requestPath = (String) message.get("path");
	int indexOfPath = requestPath.indexOf(path);
	if (indexOfPath > -1) {
	    int substringIndex = indexOfPath + pathLength
		    + repositoryNameLength + 1;
	    return requestPath.substring(substringIndex)
		    .replaceFirst("roadrunner", "");
	} else {
	    return requestPath.replaceFirst("roadrunner", "");
	}
    }
}
