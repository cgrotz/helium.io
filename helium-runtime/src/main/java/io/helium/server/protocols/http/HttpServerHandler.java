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

package io.helium.server.protocols.http;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.authorization.rulebased.RulesDataSnapshot;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

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

public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger.getLogger(HttpServerHandler.class.getName());
    private final Authorization authorization;
    private final Core core;
    private final Persistence persistence;

    private WebSocketServerHandshaker handshaker;

    private String basePath;
    private Map<Channel, HttpEndpoint> endpoints = Maps.newHashMap();

    public HttpServerHandler(String basePath, Persistence persistence, Authorization authorization, Core core) {
        this.core = core;
        this.persistence = persistence;
        this.authorization = authorization;
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

    public static String loadJsFile() throws IOException {
        URL uuid = Thread.currentThread().getContextClassLoader().getResource("uuid.js");
        URL rpc = Thread.currentThread().getContextClassLoader().getResource("rpc.js");
        URL reconnectingWebSocket = Thread.currentThread().getContextClassLoader()
                .getResource("reconnecting-websocket.min.js");
        URL helium = Thread.currentThread().getContextClassLoader().getResource("helium.js");

        String uuidContent = com.google.common.io.Resources.toString(uuid, Charsets.UTF_8);
        String reconnectingWebSocketContent = com.google.common.io.Resources.toString(
                reconnectingWebSocket, Charsets.UTF_8);
        String rpcContent = com.google.common.io.Resources.toString(rpc, Charsets.UTF_8);
        String heliumContent = com.google.common.io.Resources.toString(helium, Charsets.UTF_8);

        return uuidContent + "\r\n" + reconnectingWebSocketContent + "\r\n" + rpcContent + "\r\n"
                + heliumContent;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
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

        if ("/favicon.ico".equals(req.getUri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (!req.headers().contains("Upgrade")) {
            Path nodePath = new Path(HeliumEvent.extractPath(req.getUri().replaceAll("\\.json", "")));
            if (req.getMethod() == HttpMethod.GET) {
                RulesDataSnapshot root = new InMemoryDataSnapshot(persistence.get(null));
                Object node = persistence.get(nodePath);
                Object object = new InMemoryDataSnapshot(node);
                authorization.authorize(Operation.READ, new Node(), root, nodePath,
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
                core.handleEvent(HeliumEventType.SET, req.getUri(), Optional.fromNullable(msg));
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
                sendHttpResponse(ctx, req, res);
                return;
            } else if (req.getMethod() == HttpMethod.DELETE) {
                core.handleEvent(HeliumEventType.SET, req.getUri(), null);
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
                "ws://" + req.headers().get(HOST) + req.getUri(), null, false);
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
            HttpEndpoint endpoint = endpoints.get(ctx.channel());
            if(endpoint != null) {
                endpoint.setOpen(false);
                endpoint.executeDisconnectEvents();
                core.removeEndpoint(endpoint);
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            }
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
            final HttpEndpoint endpoint = new HttpEndpoint(
                    basePath,
                    new Node(),
                    ctx.channel(),
                    persistence,
                    authorization,
                    core);
            core.addEndpoint(endpoint);
            endpoints.put(ctx.channel(), endpoint);
        }

        endpoints.get(ctx.channel()).handle(((TextWebSocketFrame) frame).text());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
