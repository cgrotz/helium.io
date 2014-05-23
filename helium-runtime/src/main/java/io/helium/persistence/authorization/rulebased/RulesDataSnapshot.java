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

package io.helium.persistence.authorization.rulebased;

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
public interface RulesDataSnapshot {

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
    public Object val();

    /**
     * <p>
     * Get a RulesDataSnapshot for the location at the specified relative path.
     * The relative path can either be a simple child name (e.g. 'fred') or a
     * deeper slash-separated path (e.g. 'fred/name/first'). If the child
     * location has no data, an empty RulesDataSnapshot is returned.
     * </p>
     */
    public RulesDataSnapshot child(String childPath);

    /**
     * Return true if the specified child exists.
     */
    public boolean hasChild(String childPath);

    /**
     * Checks for the existence of children. If no arguments are provided, it
     * will return true if the RulesDataSnapshot has any children. If an array
     * of child names is provided, it will return true only if all of the
     * specifid children exist in the RulesDataSnapshot.
     */
    public boolean hasChildren(String... childPaths);

    /**
     * The exists function returns true if this RulesDataSnapshot contains any
     * data. It is purely a convenience function since "data.exists()" is
     * equivalent to "data.val() != null".
     */
    public boolean exists();

    /**
     * @return true if this RulesDataSnapshot contains a numeric value.
     */
    public boolean isNumber();

    /**
     * @return true if this RulesDataSnapshot contains a string value.
     */
    public boolean isString();

    /**
     * @return true if this RulesDataSnapshot contains a boolean value.
     */
    public boolean isBoolean();

}
