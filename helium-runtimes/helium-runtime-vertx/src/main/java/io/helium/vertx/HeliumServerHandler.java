package de.helium.vertx;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.io.IOException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

import com.google.common.base.Optional;

import io.helium.Helium;
import io.helium.admin.HeliumAdmin;
import io.helium.authorization.HeliumOperation;
import io.helium.authorization.rulebased.RulesDataSnapshot;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.messaging.HeliumOutboundSocket;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;

public class HeliumServerHandler {
	private String			basePath;
	private Helium	helium;

	public HeliumServerHandler(String basePath, Helium helium) {
		this.helium = helium;
		this.basePath = basePath;
	}

	public Handler<HttpServerRequest> getRestHttpHandler() {
		return new HeliumRestHttpHandler();
	}

	public Handler<HttpServerRequest> getAdminHttpHandler() {
		return new HeliumAdminHttpHandler(basePath);
	}

	public Handler<ServerWebSocket> getWebsocketHandler() {
		return new HeliumWebsocketHandler();
	}

	public Handler<HttpServerRequest> getHeliumFileHttpHandler() {
		return new HeliumFileHttpHandler();
	}

	private final class HeliumFileHttpHandler implements Handler<HttpServerRequest> {
		@Override
		public void handle(HttpServerRequest request) {
			String heliumJsFile;
			try {
				heliumJsFile = helium.loadJsFile();
				request.response().headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
				request.response().end(heliumJsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final class HeliumRestHttpHandler implements Handler<HttpServerRequest> {
		private Node	auth;

		@Override
		public void handle(final HttpServerRequest request) {
			request.response().headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

			Path nodePath = new Path(HeliumEvent.extractPath(request.uri().replaceAll("\\.json", "")));
			if (request.method().equalsIgnoreCase("GET")) {
				RulesDataSnapshot root = new InMemoryDataSnapshot(helium.getPersistence().get(null));
				Object node = helium.getPersistence().get(nodePath);
				Object object = new InMemoryDataSnapshot(node);
				helium.getAuthorization().authorize(HeliumOperation.READ, auth, root, nodePath,
						new InMemoryDataSnapshot(node));

				request.response().end(node.toString());
			} else if (request.method().equalsIgnoreCase("POST")
					|| request.method().equalsIgnoreCase("PUT")) {
				request.bodyHandler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer arg0) {
						String msg = new String(arg0.getBytes());
						helium.handleEvent(HeliumEventType.SET, request.uri(),
								Optional.fromNullable(msg));
					}
				});
			} else if (request.method().equalsIgnoreCase("DELETE")) {
				helium.handleEvent(HeliumEventType.SET, request.uri(), null);
			}
		}
	}

	private final class HeliumAdminHttpHandler implements Handler<HttpServerRequest> {
		private HeliumAdmin	heliumAdmin	= new HeliumAdmin(helium);
		private String					basePath;

		public HeliumAdminHttpHandler(String basePath) {
			this.basePath = basePath;
		}

		@Override
		public void handle(final HttpServerRequest request) {
			request.response().end(
					heliumAdmin.servePath("", basePath, request.absoluteURI().toString(), new Path(
							HeliumEvent.extractPath(request.absoluteURI().toString())), request.uri()));
		}
	}

	private final class HeliumWebsocketHandler implements Handler<ServerWebSocket> {
		@Override
		public void handle(final ServerWebSocket socket) {
			final HeliumEndpoint endpoint = new HeliumEndpoint(basePath, new Node(),
					new HeliumOutboundSocket() {

						@Override
						public void send(String string) {
							socket.writeTextFrame(string);
						}
					}, helium.getPersistence(), helium.getAuthorization(), helium);
			helium.addEndpoint(endpoint);

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
					helium.removeEndpoint(endpoint);
				}
			});
		}
	}
}
