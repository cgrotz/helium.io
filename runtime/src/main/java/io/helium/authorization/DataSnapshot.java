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

package io.helium.authorization;

import org.vertx.java.core.json.JsonObject;

/**
 * <p>
 * A RulesDataSnapshot contains data from a Helium location. It is akin to a
 * DataSnapshot but is available for use within Security Rule Expressions.
 * </p>
 * <p/>
 * <p>
 * The data, newData, and root variables each return a RulesDataSnapshot,
 * allowing your rules to operate on the data in Helium or the new data being
 * written.
 * </p>
 */
public class DataSnapshot {

    Object val;
    private JsonObject node;

    public DataSnapshot(Object value) {
        val = value;
        if (value instanceof JsonObject) {
            this.node = (JsonObject) value;
        }
    }

    /**
     * <p>
     * Get the primitive value (string, number, boolean, or null) from this
     * RulesDataSnapshot.
     * </p>
     * <p>
     * Unlike DataSnapshot.val(), calling val() on a RulesDataSnapshot that has
     * child data will not return an object containing the children. It will
     * instead return a special sentinel value. This ensures the rules can
     * always operate extremely efficiently.
     * </p>
     * <p>
     * As a consequence, you must always use child( ) to access children (e.g.
     * "data.child('name').val()", not "data.val().name").
     * </p>
     */
    public Object val() {
        return val;
    }

    /**
     * <p>
     * Get a RulesDataSnapshot for the location at the specified relative path.
     * The relative path can either be a simple child name (e.g. 'fred') or a
     * deeper slash-separated path (e.g. 'fred/name/first'). If the child
     * location has no data, an empty RulesDataSnapshot is returned.
     * </p>
     */
    @SuppressWarnings({"unused"})
    public DataSnapshot child(String childPath) {
        return new DataSnapshot(node.getValue(childPath));
    }

    /**
     * Return true if the specified child exists.
     */
    @SuppressWarnings({"unused"})
    public boolean hasChild(String childPath) {
        return node.containsField(childPath);
    }

    /**
     * The exists function returns true if this RulesDataSnapshot contains any
     * data. It is purely a convenience function since "data.exists()" is
     * equivalent to "data.val() != null".
     */
    @SuppressWarnings({"unused"})
    public boolean exists() {
        return (val != null);
    }

    /**
     * @return true if this RulesDataSnapshot contains a numeric value.
     */
    @SuppressWarnings({"unused"})
    public boolean isNumber() {
        return val instanceof Integer || val instanceof Float || val instanceof Double;
    }

    /**
     * @return true if this RulesDataSnapshot contains a string value.
     */
    public boolean isString() {
        return val instanceof String;
    }

    /**
     * @return true if this RulesDataSnapshot contains a boolean value.
     */
    public boolean isBoolean() {
        return val instanceof Boolean;
    }

}
