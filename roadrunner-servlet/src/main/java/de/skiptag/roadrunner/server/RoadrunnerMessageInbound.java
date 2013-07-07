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

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;
import de.skiptag.roadrunner.persistence.Persistence;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class RoadrunnerMessageInbound extends MessageInbound implements
	RoadrunnerSender {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerMessageInbound.class);

    private Roadrunner disruptor;

    private String path;

    private String repositoryName;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private Persistence persistence;

    public RoadrunnerMessageInbound(String servletPath, String path)
	    throws IOException, JSONException {
	this.path = path;
	this.repositoryName = path.indexOf("/") > -1 ? path.substring(0, path.indexOf("/"))
		: path;

	roadrunnerEventHandler = new RoadrunnerEventHandler(this,
		repositoryName);
	Authorization authorization = new RuleBasedAuthorization(
		new JSONObject());
	this.persistence = new InMemoryPersistence(authorization);

	Optional<File> snapshotDirectory = Optional.absent();
	disruptor = new Roadrunner(new File(""), snapshotDirectory,
		persistence, authorization, roadrunnerEventHandler, true);

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
		if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
		    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
		    persistence.sync(roadrunnerEvent.extractNodePath(), roadrunnerEventHandler);
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.DETACHED_LISTENER) {
		    roadrunnerEventHandler.removeListener(roadrunnerEvent.extractNodePath());
		} else if (roadrunnerEvent.getType() == RoadrunnerEventType.QUERY) {
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
