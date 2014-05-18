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

package io.helium.client;

import com.google.common.base.Objects;

public class Tuple<X, Y> {
    public final X x;
    public final Y y;

    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple) {
            return Objects.equal(x, ((Tuple) obj).x) && Objects.equal(y, ((Tuple) obj).y);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Tuple.class).add("x", x).add("y", y).toString();
    }

}