package de.skiptag.roadrunner.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;

public class RoadrunnerMessageInbound extends MessageInbound implements
	RoadrunnerSender {
    private static final Logger logger = LoggerFactory.getLogger(RoadrunnerMessageInbound.class);
    private Roadrunner roadrunner;

    public RoadrunnerMessageInbound(Roadrunner roadrunner) {
	this.roadrunner = roadrunner;
    }

    @Override
    protected void onBinaryMessage(ByteBuffer message) throws IOException {
	throw new UnsupportedOperationException("Binary message not supported.");
    }

    @Override
    protected void onClose(int status) {
	super.onClose(status);
	roadrunner.removeSender(this);
    }

    @Override()
    protected void onTextMessage(CharBuffer message) throws IOException {
	String msg = message.toString();
	roadrunner.handle(msg);
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
