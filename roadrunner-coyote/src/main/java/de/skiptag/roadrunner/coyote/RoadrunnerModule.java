package de.skiptag.roadrunner.coyote;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.http.HttpServerRequest;
import de.skiptag.coyote.api.modules.Module.ServletModule;
import de.skiptag.coyote.api.modules.Module.WebsocketModule;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataServiceCreationException;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.coyote.actions.PushAction;
import de.skiptag.roadrunner.coyote.actions.QueryAction;
import de.skiptag.roadrunner.coyote.actions.SetAction;
import de.skiptag.roadrunner.modeshape.ModeShapeServiceFactory;

public class RoadrunnerModule extends WebsocketModule implements ServletModule {

	private String repositoryName = "";

	private DataService dataService;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(RoadrunnerModule.class);

	private AuthorizationService authorizationService;

	public String getRepositoryName() {
		return repositoryName;
	}

	private String path;

	private QueryAction queryAction;

	private PushAction pushAction;

	private SetAction setAction;

	private RoadrunnerEventHandler roadrunnerEventHandler;

	public RoadrunnerModule(Coyote coyote, String path, String repoName) {
		super(path, coyote);
		this.path = path;
		this.repositoryName = repoName;
		try {
			authorizationService = ModeShapeServiceFactory.getInstance()
					.getAuthorizationService(repoName);
			dataService = ModeShapeServiceFactory.getInstance().getDataService(
					authorizationService, repoName);
			roadrunnerEventHandler = new RoadrunnerEventHandler(this);
			dataService.addListener(roadrunnerEventHandler);

			queryAction = new QueryAction(dataService, this);
			pushAction = new PushAction(dataService);
			setAction = new SetAction(dataService);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init() {

	}

	@Override
	public void handle(String msg) {
		try {
			JSONObject message = new JSONObject(msg);
			String messageType = (String) message.get("type");
			final String path = extractPath(message);

			if ("query".equalsIgnoreCase(messageType)) {
				String query = message.getString("query");
				queryAction.handle(query);
			} else if ("attached_listener".equalsIgnoreCase(messageType)) {
				roadrunnerEventHandler.addListener(path);
				dataService.sync(path);
			} else if ("detached_listener".equalsIgnoreCase(messageType)) {
				roadrunnerEventHandler.removeListener(path);
			} else if ("push".equalsIgnoreCase(messageType)) {
				pushAction.handle(message, path);
			} else if ("set".equalsIgnoreCase(messageType)) {
				setAction.handle(message, path);
			}
		} catch (Exception e) {
			throw new RuntimeException(msg.toString(), e);
		}
	}

	@Override
	public void handle(HttpServerRequest req) {
		try {
			req.response.sendFile("roadrunner.js");
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
		ModeShapeServiceFactory.getInstance().destroy();
	}

	@Override
	public String getServletPath() {
		return "roadrunner.js";
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
			return requestPath.substring(substringIndex).replaceFirst(
					"roadrunner", "");
		} else {
			return requestPath.replaceFirst("roadrunner", "");
		}
	}
}
