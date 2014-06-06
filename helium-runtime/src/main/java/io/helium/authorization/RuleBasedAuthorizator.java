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

import io.helium.common.Path;
import io.helium.persistence.mapdb.MapDbBackedNode;
import org.vertx.java.core.json.JsonObject;

import java.util.Optional;

public class RuleBasedAuthorizator {

    private Optional<MapDbBackedNode> nodeRule = Optional.empty();
    private Optional<JsonObject> jsonObjectRule = Optional.empty();

    public RuleBasedAuthorizator(MapDbBackedNode rule) {
        this.nodeRule = Optional.of(rule);
    }

    public RuleBasedAuthorizator(JsonObject rule) {
        this.jsonObjectRule = Optional.of(rule);
    }

    public String getExpressionForPathAndOperation(Path path, Operation op) {
        if (nodeRule.isPresent()) {
            return getExpressionForPathAndOperationNodeBased(path, op);
        } else if (jsonObjectRule.isPresent()) {
            return getExpressionForPathAndOperationJsonObjectBased(path, op);
        }
        return null;
    }

    private String getExpressionForPathAndOperationJsonObjectBased(Path path, Operation op) {
        MapDbBackedNode node = nodeRule.get().getLastLeafNode(path);
        if (node != null && node.has(op.getOp()) && node.get(op.getOp()) != null) {
            Object value = node.get(op.getOp());
            return value.toString();
        } else {
            return "false";
        }
    }

    private String getExpressionForPathAndOperationNodeBased(Path path, Operation op) {
        JsonObject node = getLastLeafNode(jsonObjectRule.get(), path);
        if (node != null && node.containsField(op.getOp()) && node.getValue(op.getOp()) != null) {
            Object value = node.getValue(op.getOp());
            return value.toString();
        } else {
            return "false";
        }
    }

    public JsonObject getLastLeafNode(JsonObject node, Path path) {
        if (node.containsField(path.firstElement())) {
            if (path.isSimple()) {
                if (node.getValue(path.firstElement()) instanceof JsonObject) {
                    return node.getValue(path.firstElement());
                } else {
                    return node;
                }
            } else {
                return getLastLeafNode(node.getValue(path.firstElement()), path.subpath(1));
            }
        } else {
            return node;
        }
    }
}
