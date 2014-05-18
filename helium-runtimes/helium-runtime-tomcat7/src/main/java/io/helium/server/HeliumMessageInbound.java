package io.helium.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.helium.Helium;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.messaging.HeliumOutboundSocket;

public class HeliumMessageInbound extends MessageInbound implements HeliumOutboundSocket {
	private static final Logger	logger	= LoggerFactory.getLogger(HeliumMessageInbound.class);
	private Helium					helium;
	private HeliumEndpoint	endpoint;
	private Node								auth;

	public HeliumMessageInbound(Node auth, String basePath, Helium helium) {
		this.helium = helium;
		this.auth = auth;
		this.endpoint = new HeliumEndpoint(basePath, auth, this, helium.getPersistence(),
				helium.getAuthorization(), helium);
		this.helium.addEndpoint(endpoint);
	}

	public void setAuth(Node auth) {
		this.auth = auth;
	}

	@Override
	protected void onBinaryMessage(ByteBuffer message) throws IOException {
		throw new UnsupportedOperationException("Binary message not supported.");
	}

	@Override
	protected void onClose(int status) {
		super.onClose(status);
		endpoint.setOpen(false);
		endpoint.executeDisconnectEvents();
		helium.removeEndpoint(endpoint);
	}

	@Override()
	protected void onTextMessage(CharBuffer message) throws IOException {
		String msg = message.toString();
		endpoint.handle(msg, new Node());
	}

	@Override
	public void send(String string) {
		try {
			logger.trace("Sending Message: " + string);
			getWsOutbound().writeTextMessage(CharBuffer.wrap(string));
		} catch (IOException e) {
			logger.error("Error sending message", e);
		}
	}

}
