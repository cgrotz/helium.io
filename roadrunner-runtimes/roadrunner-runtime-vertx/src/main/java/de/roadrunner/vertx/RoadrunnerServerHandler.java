package de.roadrunner.vertx;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.io.IOException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

import com.google.common.base.Optional;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.authorization.rulebased.RulesDataSnapshot;
import de.skiptag.roadrunner.common.Path;
import de.skiptag.roadrunner.event.RoadrunnerEvent;
import de.skiptag.roadrunner.event.RoadrunnerEventType;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.messaging.RoadrunnerOutboundSocket;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryDataSnapshot;

public class RoadrunnerServerHandler {
	private String			basePath;
	private Roadrunner	roadrunner;

	public RoadrunnerServerHandler(String basePath, Roadrunner roadrunner) {
		this.roadrunner = roadrunner;
		this.basePath = basePath;
	}

	public Handler<HttpServerRequest> getRestHttpHandler() {
		return new RoadrunnerRestHttpHandler();
	}

	public Handler<ServerWebSocket> getWebsocketHandler() {
		return new RoadrunnerWebsocketHandler();
	}

	public Handler<HttpServerRequest> getRoadrunnerFileHttpHandler() {
		return new RoadrunnerFileHttpHandler();
	}

	private final class RoadrunnerFileHttpHandler implements Handler<HttpServerRequest> {
		@Override
		public void handle(HttpServerRequest request) {
			String roadrunnerJsFile;
			try {
				roadrunnerJsFile = roadrunner.loadJsFile();
				request.response().headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
				request.response().end(roadrunnerJsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final class RoadrunnerRestHttpHandler implements Handler<HttpServerRequest> {
		private Node	auth;

		@Override
		public void handle(final HttpServerRequest request) {
			request.response().headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

			Path nodePath = new Path(RoadrunnerEvent.extractPath(request.uri()));
			if (request.method().equalsIgnoreCase("GET")) {
				RulesDataSnapshot root = new InMemoryDataSnapshot(roadrunner.getPersistence().get(null));
				Object node = roadrunner.getPersistence().get(nodePath);
				Object object = new InMemoryDataSnapshot(node);
				roadrunner.getAuthorization().authorize(RoadrunnerOperation.READ, auth, root, nodePath,
						new InMemoryDataSnapshot(node));

				request.response().end(node.toString());
			} else if (request.method().equalsIgnoreCase("POST")
					|| request.method().equalsIgnoreCase("PUT")) {
				request.bodyHandler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer arg0) {
						String msg = new String(arg0.getBytes());
						roadrunner.handleEvent(RoadrunnerEventType.SET, request.uri(),
								Optional.fromNullable(msg));
					}
				});
			} else if (request.method().equalsIgnoreCase("DELETE")) {
				roadrunner.handleEvent(RoadrunnerEventType.SET, request.uri(), null);
			}
		}
	}

	private final class RoadrunnerWebsocketHandler implements Handler<ServerWebSocket> {
		@Override
		public void handle(final ServerWebSocket socket) {
			final RoadrunnerEndpoint endpoint = new RoadrunnerEndpoint(basePath, new Node(),
					new RoadrunnerOutboundSocket() {

						@Override
						public void send(String string) {
							socket.writeTextFrame(string);
						}
					}, roadrunner.getPersistence(), roadrunner.getAuthorization(), roadrunner);
			roadrunner.addEndpoint(endpoint);

			socket.dataHandler(new Handler<Buffer>() {

				@Override
				public void handle(Buffer event) {
					// Send the uppercase string back.
					String msg = event.toString();
					endpoint.handle(msg, new Node());
				}
			});

			socket.closeHandler(new Handler<Void>() {

				@Override
				public void handle(Void arg0) {
					endpoint.setOpen(false);
					endpoint.executeDisconnectEvents();
					roadrunner.removeEndpoint(endpoint);
				}
			});
		}
	}
}
