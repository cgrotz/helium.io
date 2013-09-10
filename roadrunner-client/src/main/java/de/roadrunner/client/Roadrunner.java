package de.roadrunner.client;

import java.net.MalformedURLException;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.WebSocket;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.json.Node;

public class Roadrunner {

	protected WebSocket																					ws;
	private String																							path;
	private Multimap<Tuple<String, String>, RoadrunnerCallback>	callbacks	= ArrayListMultimap
																																						.create();
	private Set<String>																					msgCache	= Sets.newHashSet();

	public Roadrunner(String path) {
		this.path = Preconditions.checkNotNull(path);
		try {
			RoadrunnerConnection.getInstance().getSocketFor(path, new Handler<WebSocket>() {
				@Override
				public void handle(WebSocket ws) {
					Roadrunner.this.ws = ws;
					ws.endHandler(new RoadrunnerEndHandler(Roadrunner.this));
					ws.dataHandler(new RoadrunnerDataHandler(Roadrunner.this));
					for (String msg : msgCache) {
						send(msg);
					}
				}
			});
		} catch (MalformedURLException e) {

		}
	}

	public void handleEvent(Node event) {
		if (!"rpc".equalsIgnoreCase(event.getString("type"))) {
			String type = event.getString("type");
			String path = event.getString("path");
			Tuple<String, String> tuple = new Tuple<String, String>(type, path);
			for (RoadrunnerCallback callback : callbacks.get(tuple)) {
				DataSnapshot snapshot = new DataSnapshot(event);
				callback.handle(snapshot, null);
			}
		}
	}

	private void send(String msg) {
		if (ws == null) {
			msgCache.add(msg);
		} else {
			ws.writeTextFrame(msg);
		}
	}

	private void send(String type, Object data, String name) {
		Node node = new Node();
		node.put("type", type);
		node.put("name", name);
		node.put("path", path);
		node.put("payload", data);
		send(node.toString());
	}

	private void sendRpc(String method, Node args) {
		Node node = new Node();
		node.put("id", UUID.randomUUID().toString());
		node.put("method", method);
		node.put("args", args);
		send(node.toString());
	}

	public Roadrunner child(String child) {
		Preconditions.checkNotNull(child);
		return new Roadrunner(path + "/" + child);
	}

	public Roadrunner parent() {
		return new Roadrunner(path.substring(0, path.lastIndexOf("/")));
	}

	public Roadrunner push(Object value) {
		String name = UUID.randomUUID().toString();
		sendRpc("push", new Node().put("path", path).put("name", name).put("data", value));
		return new Roadrunner(path + "/" + name);
	}

	public void set(Object data) {
		sendRpc("set", new Node().put("path", path).put("data", data));
	}

	public void set(String name, Object data) {
		sendRpc("set", new Node().put("path", path).put("name", name).put("data", data));
	}

	public void update(Object data) {
		sendRpc("update", new Node().put("path", path).put("data", data));
	}

	public void on(String event_type, RoadrunnerCallback callback) {
		callbacks.put(new Tuple<String, String>(event_type, path), callback);
		sendRpc("attachListener", new Node().put("path", path).put("event_type", event_type));
	}

	public void off(String event_type, RoadrunnerCallback callback) {
		callbacks.remove(event_type, callback);
		sendRpc("detachListener", new Node().put("path", path).put("event_type", event_type));
	}

	public static void main(String args[]) {
		Roadrunner ref = new Roadrunner("http://localhost:8080/drawing/points");
		RoadrunnerCallback callback = new RoadrunnerCallback() {
			@Override
			public void handle(DataSnapshot data, String prevChildName) {
				System.out.println(data.name());
			}
		};
		ref.on("child_added", callback);
		ref.on("child_changed", callback);
		ref.on("child_removed", callback);
		ref.child("11:2").set("000");
		ref.child("12:2").set("000");
		ref.child("13:2").set("000");
		ref.child("14:2").set("000");
		while (true) {

		}
	}
}
