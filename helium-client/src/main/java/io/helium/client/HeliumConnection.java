package de.helium.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;

import com.google.common.collect.Maps;

public class HeliumConnection {

	private static HeliumConnection instance = new HeliumConnection();
	private Map<String, HttpClient> clients = Maps.newHashMap();

	private HeliumConnection() {

	}

	public static HeliumConnection getInstance() {
		return instance;
	}

	public void getSocketFor(String path, Handler<WebSocket> handler) throws MalformedURLException {
		URL url = new URL(path);
		HttpClient client;
		if (clients.containsKey(url.getHost() + ":" + url.getPort())) {
			client = clients.get(url.getHost() + ":" + url.getPort());
		} else {
			client = VertxFactory.newVertx().createHttpClient().setHost(url.getHost())
					.setPort(url.getPort());
		}
		client.connectWebsocket(url.getPath(), handler);
	}
}
