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

package io.helium.persistence.authorization.rule;

import io.helium.common.Path;
import io.helium.json.Node;
import io.helium.persistence.authorization.Operation;

public class RuleBasedAuthorizator {

    private Node rule;

    public RuleBasedAuthorizator(Node rule) {
        this.rule = rule;
    }

    // private static HeliumOperation getOperation(String key) {
    // for (HeliumOperation operation : HeliumOperation.values()) {
    // if (key.contains(operation.getOp())) {
    // return operation;
    // }
    // }
    // throw new RuntimeException("HeliumOperation " + key + " not found");
    // }

    public String getExpressionForPathAndOperation(Path path, Operation op) {
        Node node = rule.getLastLeafNode(path);
        if (node != null && node.has(op.getOp()) && node.get(op.getOp()) != null) {
            Object value = node.get(op.getOp());
            return value.toString();
            // return node.getString(op.getOp());
        } else {
            return "true";
        }

    }
}
