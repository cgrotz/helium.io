package de.skiptag.roadrunner.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.json.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.messaging.RoadrunnerResponseSender;

public class RoadrunnerMessageInbound extends MessageInbound implements
	RoadrunnerResponseSender {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerMessageInbound.class);
    private Roadrunner roadrunner;
    private RoadrunnerEndpoint endpoint;
    private Node auth;

    public RoadrunnerMessageInbound(String basePath, Roadrunner roadrunner) {
	this.roadrunner = roadrunner;
	this.endpoint = new RoadrunnerEndpoint(basePath, this,
		roadrunner.getPersistence());
	this.roadrunner.addEndpoint(endpoint);
    }

    @Override
    protected void onBinaryMessage(ByteBuffer message) throws IOException {
	throw new UnsupportedOperationException("Binary message not supported.");
    }

    @Override
    protected void onClose(int status) {
	super.onClose(status);
	roadrunner.removeEndpoint(endpoint);
    }

    @Override()
    protected void onTextMessage(CharBuffer message) throws IOException {
	String msg = message.toString();
	RoadrunnerEvent roadrunnerEvent = new RoadrunnerEvent(msg);
	roadrunnerEvent.put(RoadrunnerEvent.AUTH, auth);
	roadrunner.handle(endpoint, roadrunnerEvent);
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
