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

package io.helium.persistence;

import io.helium.persistence.actions.*;
import org.vertx.java.core.Future;

public class Persistence extends CommonPersistenceVerticle {

    public static final String PUSH = "io.helium.persistor.push";
    public static final String SET = "io.helium.persistor.set";
    public static final String DELETE = "io.helium.persistor.delete";
    public static final String UPDATE = "io.helium.persistor.update";
    public static final String GET = "io.helium.persistor.get";

    @Override
    public void start(Future<Void> startedResult) {
        try{
            container.deployVerticle(Get.class.getName());
            container.deployVerticle(Post.class.getName());
            container.deployVerticle(Put.class.getName());
            container.deployVerticle(Delete.class.getName());
            container.deployVerticle(Update.class.getName());
        }
        catch(Exception e) {
            startedResult.setFailure(e);
        }
    }
}
