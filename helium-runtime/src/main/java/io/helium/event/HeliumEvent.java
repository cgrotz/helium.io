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
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.event.changelog.ChangeLog;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Optional;

/**
 * Abstraction for Helium Events. Has helper methods for accessing the underlying JSON data.
 *
 * @author Christoph Grotz
 */
public class HeliumEvent extends JsonObject {

    public static final String NO_AUTH = "noAuth";
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

    private HeliumEventBuilder builder;

    public HeliumEvent(JsonObject body) {
        this.map = body.toMap();
    }

    public HeliumEventBuilder complete() {
        return this.builder;
    }

    public HeliumEvent(HeliumEventBuilder heliumEventBuilder) {
        this();
        this.builder = heliumEventBuilder;
    }

    /**
     * Creates an empty event
     */
    public HeliumEvent() {
        putNumber(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    /**
     * Creates an Node from the given json string
     *
     * @param json data as {@link String}
     */
    public HeliumEvent(String json) {
        super(json);
        putNumber(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path, Object payload) {
        putString(HeliumEvent.TYPE, type.toString());
        putString(HeliumEvent.PATH, path);
        putValue(HeliumEvent.PAYLOAD, payload);
        putNumber(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    /**
     * @param type    of the event {@link HeliumEventType}
     * @param path    of the event
     * @param payload Optional playload of the event
     */
    public HeliumEvent(HeliumEventType type, String path, Optional<?> payload) {
        putString(HeliumEvent.TYPE, type.toString());
        putString(HeliumEvent.PATH, path);
        if (payload.isPresent()) {
            putValue(HeliumEvent.PAYLOAD, payload.get());
        }
        putNumber(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
    }

    public HeliumEvent(HeliumEventType type, String path) {
        putString(HeliumEvent.TYPE, type.toString());
        putString(HeliumEvent.PATH, path);
        putNumber(HeliumEvent.CREATION_DATE, System.currentTimeMillis());
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

    public Path extractNodePath() {
        if (!containsField(HeliumEvent.PATH)) {
            return null;
        }
        String requestPath = getString(HeliumEvent.PATH);
        if (containsField(HeliumEvent.NAME)) {
            return new Path(extractPath(requestPath)).append(getString(HeliumEvent.NAME));
        }
        return new Path(extractPath(requestPath));
    }

    public String extractParentPath() {
        if (!containsField(HeliumEvent.PATH)) {
            return null;
        }
        String parentPath;
        String requestPath = getString(HeliumEvent.PATH);
        if (containsField(HeliumEvent.NAME)) {
            parentPath = extractPath(requestPath) + "/" + getString(HeliumEvent.NAME);
        } else {
            parentPath = extractPath(requestPath);
        }
        return parentPath.substring(0, parentPath.lastIndexOf("/"));

    }

    public HeliumEventType getType() {
        return HeliumEventType.valueOf((getString(HeliumEvent.TYPE)).toUpperCase());
    }

    public boolean isFromHistory() {
        if (containsField(HeliumEvent.FROM_HISTORY)) {
            return getBoolean(HeliumEvent.FROM_HISTORY);
        } else {
            return false;
        }
    }

    public void setFromHistory(boolean fromHistory) {
        putBoolean(HeliumEvent.FROM_HISTORY, fromHistory);
    }

    public long getCreationDate() {
        return getLong(HeliumEvent.CREATION_DATE);
    }

    public void setCreationDate(long creationDate) {
        putNumber(HeliumEvent.CREATION_DATE, creationDate);
    }

    public Object getPayload() {
        return getValue(HeliumEvent.PAYLOAD);
    }

    public ChangeLog getChangeLog() {
        JsonArray log = getArray("changelog", new JsonArray());
        putArray("changelog", log);
        return ChangeLog.of(log);
    }

    public Optional<JsonObject> getAuth() {
        if (!containsField(AUTH)) {
            return Optional.empty();
        }
        return Optional.of(getObject(AUTH));
    }

    public void setAuth(JsonObject auth) {
        putObject(AUTH, auth);
    }

    public HeliumEvent copy() {
        return new HeliumEvent(toString());
    }

    public boolean isNoAuth() {
        return getBoolean(NO_AUTH, false);
    }

    public static HeliumEvent of(JsonObject body) {
        return new HeliumEvent(body);
    }
}
