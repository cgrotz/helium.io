/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.server;

import io.helium.Helium;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.messaging.HeliumOutboundSocket;
import org.apache.catalina.websocket.MessageInbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class HeliumMessageInbound extends MessageInbound implements HeliumOutboundSocket {
    private static final Logger logger = LoggerFactory.getLogger(HeliumMessageInbound.class);
    private Helium helium;
    private HeliumEndpoint endpoint;
    private Node auth;

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
