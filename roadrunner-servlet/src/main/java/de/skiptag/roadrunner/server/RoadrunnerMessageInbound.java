package de.skiptag.roadrunner.server;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import de.skiptag.roadrunner.authorization.AuthorizationService;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorizationService;
import de.skiptag.roadrunner.dataService.DataService;
import de.skiptag.roadrunner.dataService.DataServiceCreationException;
import de.skiptag.roadrunner.dataService.inmemory.InMemoryDataService;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;
import de.skiptag.roadrunner.disruptor.DisruptorRoadrunnerService;
import de.skiptag.roadrunner.disruptor.event.MessageType;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class RoadrunnerMessageInbound extends MessageInbound implements
	RoadrunnerSender {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerMessageInbound.class);

    private DisruptorRoadrunnerService disruptor;

    private String path;

    private String repositoryName;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private DataService dataService;

    public RoadrunnerMessageInbound(String servletPath, String path)
	    throws DataServiceCreationException, IOException, JSONException {
	this.path = path;
	this.repositoryName = path.indexOf("/") > -1 ? path.substring(0, path.indexOf("/"))
		: path;

	roadrunnerEventHandler = new RoadrunnerEventHandler(this,
		repositoryName);
	AuthorizationService authorizationService = new RuleBasedAuthorizationService(
		new JSONObject());
	this.dataService = new InMemoryDataService(authorizationService);

	Optional<File> snapshotDirectory = Optional.absent();
	disruptor = new DisruptorRoadrunnerService(new File(""),
		snapshotDirectory, dataService, authorizationService, true);

    }

    @Override
    protected void onBinaryMessage(ByteBuffer message) throws IOException {
	throw new UnsupportedOperationException("Binary message not supported.");
    }

    @Override
    protected void onClose(int status) {
	super.onClose(status);
	try {
	    disruptor.shutdown();
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    @Override()
    protected void onTextMessage(CharBuffer message) throws IOException {
	String msg = message.toString();
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
    public void send(String string) {
	try {
	    getWsOutbound().writeTextMessage(CharBuffer.wrap(string));
	} catch (IOException e) {
	    logger.error("Error sending message", e);
	}
    }
}
