/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package de.roadrunner.netty;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.authorization.Authorization;
import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.messaging.RoadrunnerEventHandler;
import de.skiptag.roadrunner.messaging.RoadrunnerSender;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

/**
 * Handles handshakes and messages
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object>
	implements RoadrunnerSender {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class.getName());

    private static final String WEBSOCKET_PATH = "/";

    private WebSocketServerHandshaker handshaker;

    private Set<Channel> channels = Sets.newHashSet();

    private InMemoryPersistence persistence;

    private RoadrunnerEventHandler roadrunnerEventHandler;

    private String path;

    private Roadrunner disruptor;

    private String repositoryName;

    public WebSocketServerHandler() {

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg)
	    throws Exception {
	if (msg instanceof FullHttpRequest) {
	    handleHttpRequest(ctx, (FullHttpRequest) msg);
	} else if (msg instanceof WebSocketFrame) {
	    handleWebSocketFrame(ctx, (WebSocketFrame) msg);
	}
    }

    private void handleHttpRequest(ChannelHandlerContext ctx,
	    FullHttpRequest req) throws Exception {
	// Handle a bad request.
	if (!req.getDecoderResult().isSuccess()) {
	    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
		    BAD_REQUEST));
	    return;
	}

	// Allow only GET methods.
	if (req.getMethod() != GET) {
	    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
		    FORBIDDEN));
	    return;
	}

	// Send the demo page and favicon.ico
	if ("/".equals(req.getUri())
		&& !"websocket".equals(((io.netty.handler.codec.http.DefaultFullHttpRequest) req).headers()
			.get("Upgrade"))) {
	    ByteBuf content = WebSocketServerIndexPage.getContent(getWebSocketLocation(req));
	    FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
		    content);

	    res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
	    setContentLength(res, content.readableBytes());

	    sendHttpResponse(ctx, req, res);
	    return;
	}
	if ("/favicon.ico".equals(req.getUri())) {
	    FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
		    NOT_FOUND);
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
	channels.add(ctx.channel());
	if (disruptor == null) {
	    try {
		this.path = getWebSocketLocation(req);
		this.repositoryName = req.getUri().substring(1);
		if (repositoryName.indexOf("/") != -1) {
		    this.repositoryName = repositoryName.substring(0, repositoryName.indexOf("/"));
		}
		roadrunnerEventHandler = new RoadrunnerEventHandler(this,
			repositoryName);
		Authorization authorization = new RuleBasedAuthorization(
			new JSONObject());
		this.persistence = new InMemoryPersistence(authorization);

		Optional<File> snapshotDirectory = Optional.absent();
		disruptor = new Roadrunner(
			new File("/home/balu/tmp/roadrunner"),
			snapshotDirectory, persistence, authorization,
			roadrunnerEventHandler, true);
	    } catch (Exception exp) {
		throw new RuntimeException(exp);
	    }
	}
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx,
	    WebSocketFrame frame) {

	// Check for closing frame
	if (frame instanceof CloseWebSocketFrame) {
	    handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
	    return;
	}
	if (frame instanceof PingWebSocketFrame) {
	    ctx.channel()
		    .write(new PongWebSocketFrame(frame.content().retain()));
	    return;
	}
	if (!(frame instanceof TextWebSocketFrame)) {
	    throw new UnsupportedOperationException(
		    String.format("%s frame types not supported", frame.getClass()
			    .getName()));
	}

	// Send the uppercase string back.
	String msg = ((TextWebSocketFrame) frame).text();
	System.out.println("Received Message: " + msg);
	try {
	    RoadrunnerEvent roadrunnerEvent;

	    try {
		roadrunnerEvent = new RoadrunnerEvent(msg, path, repositoryName);
		Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("repositoryName"), "No repositoryName defined in Event");
	    } catch (Exception exp) {
		roadrunnerEvent = null;
	    }

	    if (roadrunnerEvent.has("type")) {
		if (roadrunnerEvent.getType() == RoadrunnerEventType.ATTACHED_LISTENER) {
		    roadrunnerEventHandler.addListener(roadrunnerEvent.extractNodePath());
		    persistence.sync(new Path(roadrunnerEvent.extractNodePath()), roadrunnerEventHandler);
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

    private static void sendHttpResponse(ChannelHandlerContext ctx,
	    FullHttpRequest req, FullHttpResponse res) {
	// Generate an error page if response getStatus code is not OK (200).
	if (res.getStatus().code() != 200) {
	    res.content().writeBytes(Unpooled.copiedBuffer(res.getStatus()
		    .toString(), CharsetUtil.UTF_8));
	    setContentLength(res, res.content().readableBytes());
	}

	// Send the response and close the connection if necessary.
	ChannelFuture f = ctx.channel().write(res);
	if (!isKeepAlive(req) || res.getStatus().code() != 200) {
	    f.addListener(ChannelFutureListener.CLOSE);
	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	    throws Exception {
	cause.printStackTrace();
	ctx.close();
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
	return "ws://" + req.headers().get(HOST) + WEBSOCKET_PATH;
    }

    @Override
    public void send(String msg) {
	System.out.println("Send Message: " + msg);
	for (Channel channel : channels) {
	    channel.write(new TextWebSocketFrame(msg));
	}
    }
}