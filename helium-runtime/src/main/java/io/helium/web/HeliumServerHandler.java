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

package io.helium.web;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import io.helium.Helium;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.persistence.authorization.HeliumOperation;
import io.helium.persistence.authorization.rulebased.RulesDataSnapshot;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import io.helium.web.admin.HeliumAdmin;
import io.helium.web.messaging.HeliumEndpoint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HeliumServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger.getLogger(HeliumServerHandler.class.getName());

    private static final String WEBSOCKET_PATH = "/websocket";
    private final HeliumAdmin heliumAdmin;

    private WebSocketServerHandshaker handshaker;

    private String basePath;
    private Helium helium;
    private Map<Channel, HeliumEndpoint> endpoints = Maps.newHashMap();

    public HeliumServerHandler(String basePath, Helium helium) {
        this.helium = helium;
        this.heliumAdmin = new HeliumAdmin(helium);
        this.basePath = basePath;
    }

    public static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        return "ws://" + req.headers().get(HOST) + WEBSOCKET_PATH;
    }

    public static String loadJsFile() throws IOException {
        URL uuid = Thread.currentThread().getContextClassLoader().getResource("uuid.js");
        URL rpc = Thread.currentThread().getContextClassLoader().getResource("rpc.js");
        URL reconnectingwebsocket = Thread.currentThread().getContextClassLoader()
                .getResource("reconnecting-websocket.min.js");
        URL helium = Thread.currentThread().getContextClassLoader().getResource("helium.js");

        String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
        String reconnectingWebsocketContent = com.google.common.io.Resources.toString(
                reconnectingwebsocket, Charsets.UTF_8);
        String rpcContent = com.google.common.io.Resources.toString(rpc, Charsets.UTF_8);
        String heliumContent = com.google.common.io.Resources.toString(helium, Charsets.UTF_8);

        return uuidContent + "\r\n" + reconnectingWebsocketContent + "\r\n" + rpcContent + "\r\n"
                + heliumContent;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        if (req.getMethod() == GET && req.getUri().endsWith("helium.js")) {
            String heliumJsFile = loadJsFile();
            ByteBuf content = Unpooled.copiedBuffer(heliumJsFile.getBytes());
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            res.headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
            setContentLength(res, content.readableBytes());
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (req.getMethod() == GET && !req.headers().contains("Upgrade", "websocket", true) && (req.getUri().endsWith(".js") || req.getUri().endsWith(".css") || req.getUri().endsWith(".html") || req.getUri().endsWith(".png"))) {
            String admin = heliumAdmin.servePath("", basePath, req.getUri(), new Path(HeliumEvent.extractPath(req.getUri())), req.getUri());
            ByteBuf content = Unpooled.copiedBuffer(admin.getBytes());
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            res.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(req.getUri()));
            setContentLength(res, content.readableBytes());
            sendHttpResponse(ctx, req, res);
            return;
        }

        if ("/favicon.ico".equals(req.getUri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (!req.headers().contains("Upgrade", "websocket", true)
                && (req.getUri().endsWith(".html") ||
                req.getUri().endsWith(".js") ||
                req.getUri().endsWith(".css") ||
                req.getUri().endsWith(".png")
        )) {
            heliumAdmin.servePath(ctx, req);
            return;
        }

        if (!req.headers().contains("Upgrade", "websocket", true) && req.getUri().endsWith(".json")) {
            Path nodePath = new Path(HeliumEvent.extractPath(req.getUri().replaceAll("\\.json", "")));
            if (req.getMethod() == HttpMethod.GET) {
                RulesDataSnapshot root = new InMemoryDataSnapshot(helium.getPersistence().get(null));
                Object node = helium.getPersistence().get(nodePath);
                Object object = new InMemoryDataSnapshot(node);
                helium.getAuthorization().authorize(HeliumOperation.READ, new Node(), root, nodePath,
                        new InMemoryDataSnapshot(node));

                ByteBuf content = Unpooled.copiedBuffer(node.toString().getBytes());
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
                res.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
                setContentLength(res, content.readableBytes());
                sendHttpResponse(ctx, req, res);
                return;
            } else if (req.getMethod() == HttpMethod.POST
                    || req.getMethod() == HttpMethod.PUT) {
                String msg = new String(req.content().array());
                helium.handleEvent(HeliumEventType.SET, req.getUri(), Optional.fromNullable(msg));
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
                sendHttpResponse(ctx, req, res);
                return;
            } else if (req.getMethod() == HttpMethod.DELETE) {
                helium.handleEvent(HeliumEventType.SET, req.getUri(), null);
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
                sendHttpResponse(ctx, req, res);
                return;
            }
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
            sendHttpResponse(ctx, req, res);
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            HeliumEndpoint endpoint = endpoints.get(ctx.channel());
            endpoint.setOpen(false);
            endpoint.executeDisconnectEvents();
            helium.removeEndpoint(endpoint);
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
        if (!endpoints.containsKey(ctx.channel())) {
            final HeliumEndpoint endpoint = new HeliumEndpoint(basePath, new Node(), ctx.channel(), helium.getPersistence(), helium.getAuthorization(), helium);
            helium.addEndpoint(endpoint);
            endpoints.put(ctx.channel(), endpoint);
        }

        endpoints.get(ctx.channel()).handle(((TextWebSocketFrame) frame).text(), new Node());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
