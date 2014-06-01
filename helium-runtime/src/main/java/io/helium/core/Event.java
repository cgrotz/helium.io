package io.helium.core;

import io.helium.json.Node;

/**
 * Created by Christoph Grotz on 01.06.14.
 */
public class Event {
    private final Node event;
    private final String path;

    public Event(String path, Node event) {
        this.path = path;
        this.event = event;
    }

    public String getPath() {
        return path;
    }

    public Node getEvent() {
        return event;
    }
}
