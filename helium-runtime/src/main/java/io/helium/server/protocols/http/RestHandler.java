package io.helium.server.protocols.http;

import io.helium.common.Path;
import io.helium.core.Core;
import io.helium.core.processor.authorization.AuthorizationProcessor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import io.helium.server.DataTypeConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class RestHandler {

    private final Authorization authorization;
    private final Core core;
    private final Persistence persistence;
    private final String basePath;

    public RestHandler(String basePath, Core core, Persistence persistence, Authorization authorization) {
        this.basePath = basePath;
        this.core = core;
        this.persistence = persistence;
        this.authorization = authorization;
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest req) {

        Path nodePath = new Path(HeliumEvent.extractPath(req.getUri().replaceAll("\\.json", "")));
        if (req.getMethod() == HttpMethod.GET) {
            get(ctx, req, nodePath);
        } else if (req.getMethod() == HttpMethod.POST) {
            post(ctx, req);
        } else if( req.getMethod() == HttpMethod.PUT) {
            put(ctx, req);
        } else if (req.getMethod() == HttpMethod.DELETE) {
            delete(ctx, req);
        } else {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
    }

    private Optional<Node> extractAuthentication(FullHttpRequest req) {
        Optional<Node> auth = Optional.empty();
        if( req.headers().contains(HttpHeaders.Names.AUTHORIZATION)) {
            String authorizationToken = req.headers().get(HttpHeaders.Names.AUTHORIZATION);
            Node authentication = AuthorizationProcessor.decode(authorizationToken);
            String username = authentication.getString("username");
            String password = authentication.getString("password");
            Node users = persistence.getNode(new Path("/users"));
            for(Object value : users.values()) {
                if(value instanceof HashMapBackedNode) {
                    Node node = (Node)value;
                    if(node.has("username") && node.has("password")) {
                        String localUsername = node.getString("username");
                        String localPassword = node.getString("password");
                        if(username.equals(localUsername) && password.equals(localPassword)) {
                            auth = Optional.of(node);
                        }
                    }
                }
            }
        }
        return auth;
    }

    private void delete(ChannelHandlerContext ctx, FullHttpRequest req) {
        Path nodePath = Path.of(req.getUri());
        DataSnapshot root = new InMemoryDataSnapshot(persistence.get(null));
        Object node = persistence.get(nodePath);
        Object object = new InMemoryDataSnapshot(node);
        if(authorized(Operation.WRITE, req, root, nodePath, object)) {
            core.handleEvent(HeliumEventType.REMOVE, extractAuthentication(req), req.getUri(), Optional.empty());
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.buffer(0));
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        } else {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, Unpooled.buffer(0));
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
    }

    private void put(ChannelHandlerContext ctx, FullHttpRequest req) {
        Path nodePath = Path.of(req.getUri());
        DataSnapshot root = new InMemoryDataSnapshot(persistence.get(null));
        Object node = persistence.get(nodePath);
        Object object = new InMemoryDataSnapshot(node);
        if(authorized(Operation.WRITE, req, root, nodePath, object)) {
            ByteBuf content = Unpooled.buffer(req.content().readableBytes());
            req.content().readBytes(content);
            core.handleEvent(HeliumEventType.SET, extractAuthentication(req), req.getUri(), Optional.ofNullable(DataTypeConverter.convert(content.array())));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.buffer(0));
            res.headers().set(HttpHeaders.Names.LOCATION, req.getUri());
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        } else {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, Unpooled.buffer(0));
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
    }

    private void post(ChannelHandlerContext ctx, FullHttpRequest req) {
        ByteBuf content = Unpooled.buffer(req.content().readableBytes());
        req.content().readBytes(content);
        String uri;
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        if(req.getUri().endsWith("/")) {
            uri = req.getUri()+ uuid;
        }
        else {
            uri = req.getUri()+"/"+ uuid;
        }
        Path nodePath = Path.of(uri);
        DataSnapshot root = new InMemoryDataSnapshot(persistence.get(null));
        Object node = persistence.get(nodePath);
        Object object = new InMemoryDataSnapshot(node);
        if(authorized(Operation.WRITE, req, root, nodePath, object)) {
            core.handleEvent(HeliumEventType.PUSH, extractAuthentication(req), uri, Optional.ofNullable(DataTypeConverter.convert(content.array())));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.buffer(0));
            res.headers().set(HttpHeaders.Names.LOCATION, uri);
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        } else {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, Unpooled.buffer(0));
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
    }

    private void get(ChannelHandlerContext ctx, FullHttpRequest req, Path nodePath) {
        DataSnapshot root = new InMemoryDataSnapshot(persistence.get(null));
        Object node = persistence.get(nodePath);
        InMemoryDataSnapshot object = new InMemoryDataSnapshot(node);
        if(authorized(Operation.READ, req, root, nodePath, object)) {
            ByteBuf content = Unpooled.buffer();
            content.writeBytes(authorization.filterContent(extractAuthentication(req), nodePath, persistence.getRoot(), node).toString().getBytes());
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            res.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
            setContentLength(res, content.readableBytes());
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
        else {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, Unpooled.buffer(0));
            setContentLength(res, 0);
            HttpServerHandler.sendHttpResponse(ctx, req, res);
        }
    }

    private boolean authorized(Operation operation, FullHttpRequest req, DataSnapshot root, Path nodePath, Object node) {
        Optional<Node> auth = extractAuthentication(req);
        return authorization.isAuthorized(operation, auth, root, nodePath,
                new InMemoryDataSnapshot(node));
    }
}
