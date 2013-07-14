package de.skiptag.roadrunner.coyote;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.http.common.HttpServerRequest;
import de.skiptag.coyote.api.modules.ServletModule;
import de.skiptag.coyote.api.modules.WebsocketModule;
import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;

public class RoadrunnerModule extends WebsocketModule implements ServletModule,
	RoadrunnerSender {

    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerModule.class);
    private Roadrunner roadrunner;
    private String moduleWebPath;

    public RoadrunnerModule(Coyote coyote, String moduleWebPath,
	    String journalDirectory, NativeObject rule) throws JSONException {
	this.moduleWebPath = moduleWebPath;
	roadrunner = new Roadrunner(journalDirectory,
		RoadrunnerService.toJSONObject(rule));
    }

    public void setAuthentication(String token) throws JSONException {
	JSONObject auth = new JSONObject();
	auth.put("id", token);
    }

    @Override
    public void init() {

    }

    @Override
    public void handle(String msg) {
	roadrunner.handle(msg);
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

    public RoadrunnerService load() {
	return new RoadrunnerService(roadrunner.getAuthorization(),
		roadrunner.getPersistence(), "/");
    }

    @Override
    public void destroy() {

    }

    @Override
    public String getServletPath() {
	return "roadrunner(.)*";
    }

    @Override
    public String getWebsocketPath() {
	return moduleWebPath + "/(.)*";
    }
}
