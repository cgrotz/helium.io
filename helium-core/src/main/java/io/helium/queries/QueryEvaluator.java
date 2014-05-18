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

package io.helium.queries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.helium.common.Path;
import io.helium.json.Node;
import io.helium.scripting.SandboxedScriptingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map.Entry;

/**
 * Sandbox Query executing thanks to
 * http://worldwizards.blogspot.de/2009/08/java-scripting-api-sandbox.html
 *
 * @author balu
 */
public class QueryEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryEvaluator.class);
    private Multimap<String, String> attached_queries = HashMultimap.create();
    private Multimap<String, String> nodesForQuery = HashMultimap.create();

    private SandboxedScriptingEnvironment scriptingEnvironment = new SandboxedScriptingEnvironment();

    public boolean appliesToQuery(Path path, Object value) {
        for (String queryStr : attached_queries.get(path.toString())) {
            return evaluateQueryOnValue(value, queryStr);
        }
        return false;
    }

    public boolean evaluateQueryOnValue(Object value, String queryStr) {
        try {

            Object parsedValue;
            if (value instanceof Node) {
                parsedValue = scriptingEnvironment.eval("JSON.parse('"
                        + value.toString() + "');");
            } else {
                parsedValue = value;
            }
            scriptingEnvironment.eval("var query = " + queryStr + ";");
            Boolean result = (Boolean) scriptingEnvironment.invokeFunction("query", parsedValue);
            return result.booleanValue();
        } catch (Exception e) {
            LOGGER.error("Error (" + e.getMessage() + ") on Query (" + queryStr + ")", e);
        }
        return false;
    }

    public void addQuery(Path path, String query) {
        attached_queries.put(path.toString(), query);
    }

    public void removeQuery(Path path, String query) {
        attached_queries.remove(path.toString(), query);
    }

    public boolean hasQuery(Path path) {
        return attached_queries.containsKey(path.toString());
    }

    public boolean queryContainsNode(Path queryPath, String query, Path nodePath) {
        return nodesForQuery.containsEntry(new QueryEntry(queryPath, query).toString(), nodePath.toString());
    }

    public boolean addNodeToQuery(Path path, String query, Path pathToNode) {
        return nodesForQuery.put(new QueryEntry(path, query).toString(), pathToNode.toString());
    }

    public boolean removeNodeFromQuery(Path path, String query, Path pathToNode) {
        return nodesForQuery.remove(new QueryEntry(path, query).toString(), pathToNode.toString());
    }

    public Collection<Entry<String, String>> getQueries() {
        return attached_queries.entries();
    }
}
