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

package io.helium.event;

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.JSONTokener;
import io.helium.json.Node;

import java.util.Optional;

/**
 * Abstraction for Helium Events. Has helper methods for accessing the underlying JSON data.
 *
 * @author Christoph Grotz
 */
public class HeliumEvent extends Node {

    /**
     * Payload of the event
     */
    public static final String PAYLOAD = "payload";

    /**
     * Timestamp when the event was created
     */
    public static final String CREATION_DATE = "creationDate";

    /**
     * Type of the Event {@link HeliumEventType}
     */
    public static final String TYPE = "type";

    /**
     * {@link Path} for the event
     */
    public static final String PATH = "path";

    /**
     * Was this event read from the history
     */
    public static final String FROM_HISTORY = "fromHistory";

    /**
     * Authentication associated with the event
     */
    public static final String AUTH = "auth";

    /**
     * Name of the data element
     */
    public static final String NAME = "name";

    /**
     * Priority to be set on the element at the path
     */
    public static final String PRIORITY = "priority";

    /**
     * Previous Child name
     */
    public static final String PREVCHILDNAME = "prevChildName";

    private ChangeLog changeLog = new ChangeLog();

    /**
     * Creates an empty event
     */
    public HeliumEvent() {
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    /**
     * Creates an Node from the given json string
     *
     * @param json data as {@link String}
     */
    public HeliumEvent(String json) {
        super(json);
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path, Object payload) {
        put(HeliumEvent.TYPE, type.toString());
        put(HeliumEvent.PATH, path);
        put(HeliumEvent.PAYLOAD, payload);
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    /**
     * @param type    of the event {@link HeliumEventType}
     * @param path    of the event
     * @param payload Optional playload of the event
     */
    public HeliumEvent(HeliumEventType type, String path, Optional<?> payload) {
        put(HeliumEvent.TYPE, type.toString());
        put(HeliumEvent.PATH, path);
        if (payload.isPresent()) {
            put(HeliumEvent.PAYLOAD, payload.get());
        }
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path, Object data, Integer priority) {
        put(HeliumEvent.TYPE, type.toString());
        put(HeliumEvent.PATH, path);
        put(HeliumEvent.PAYLOAD, data);
        put(HeliumEvent.PRIORITY, priority);
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path, Integer priority) {
        put(HeliumEvent.TYPE, type.toString());
        put(HeliumEvent.PATH, path);
        put(HeliumEvent.PRIORITY, priority);
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path) {
        put(HeliumEvent.TYPE, type.toString());
        put(HeliumEvent.PATH, path);
        put(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    /**
     * @param url of the element
     * @return the path representation
     */
    public static String extractPath(String url) {
        String result = url;
        if (url.startsWith("http://")) {
            if (url.indexOf("/", 7) != -1) {
                result = url.substring(url.indexOf("/", 7));
            } else {
                result = "";
            }
        } else if (url.startsWith("https://")) {
            if (url.indexOf("/", 8) != -1) {
                result = url.substring(url.indexOf("/", 8));
            } else {
                result = "";
            }
        }

        return result.startsWith("/") ? result : "/" + result;
    }

    /**
     * Populates the instance with the handed json data
     *
     * @param json
     */
    public void populate(String json) {
        JSONTokener x = new JSONTokener(json);
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'");
        }
        for (; ; ) {
            c = x.nextClean();
            switch (c) {
                case 0:
                    throw x.syntaxError("A JSONObject text must end with '}'");
                case '}':
                    return;
                default:
                    x.back();
                    key = x.nextValue().toString();
            }

            // The key is followed by ':'.

            c = x.nextClean();
            if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            this.put(key, x.nextValue());

            // Pairs are separated by ','.

            switch (x.nextClean()) {
                case ';':
                case ',':
                    if (x.nextClean() == '}') {
                        return;
                    }
                    x.back();
                    break;
                case '}':
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    public Path extractNodePath() {
        if (!has(HeliumEvent.PATH)) {
            return null;
        }
        String requestPath = (String) get(HeliumEvent.PATH);
        if (has(HeliumEvent.NAME)) {
            if (get(HeliumEvent.NAME) != Node.NULL) {
                return new Path(extractPath(requestPath)).append(getString(HeliumEvent.NAME));
            }
        }
        return new Path(extractPath(requestPath));
    }

    public String extractParentPath() {
        if (!has(HeliumEvent.PATH)) {
            return null;
        }
        String parentPath;
        String requestPath = (String) get(HeliumEvent.PATH);
        if (has(HeliumEvent.NAME) && get(HeliumEvent.NAME) != Node.NULL) {
            parentPath = extractPath(requestPath) + "/" + getString(HeliumEvent.NAME);
        } else {
            parentPath = extractPath(requestPath);
        }
        return parentPath.substring(0, parentPath.lastIndexOf("/"));

    }

    public HeliumEventType getType() {
        return HeliumEventType.valueOf(((String) get(HeliumEvent.TYPE)).toUpperCase());
    }

    public boolean isFromHistory() {
        if (has(HeliumEvent.FROM_HISTORY)) {
            return getBoolean(HeliumEvent.FROM_HISTORY);
        } else {
            return false;
        }
    }

    public void setFromHistory(boolean fromHistory) {
        put(HeliumEvent.FROM_HISTORY, fromHistory);
    }

    public long getCreationDate() {
        return getLong(HeliumEvent.CREATION_DATE);
    }

    public void setCreationDate(long creationDate) {
        put(HeliumEvent.CREATION_DATE, creationDate);
    }

    public int getPriority() {
        return getInt(HeliumEvent.PRIORITY);
    }

    public void setPriority(int priority) {
        put(HeliumEvent.PRIORITY, priority);
    }

    public boolean hasPriority() {
        return has(HeliumEvent.PRIORITY);
    }

    public Object getPayload() {
        return get(HeliumEvent.PAYLOAD);
    }

    public ChangeLog getChangeLog() {
        return changeLog;
    }

    public Node getAuth() {
        if (!has(AUTH)) {
            return new Node();
        }
        return getNode(AUTH);
    }

    public void setAuth(Node auth) {
        put(AUTH, auth);
    }

    @Override
    public void clear() {
        super.clear();
        getChangeLog().clear();
    }

    public HeliumEvent copy() {
        return new HeliumEvent(toString());
    }
}
