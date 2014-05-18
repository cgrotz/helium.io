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

package io.helium;

import io.helium.authorization.Authorization;
import io.helium.authorization.rulebased.RuleBasedAuthorization;
import io.helium.common.Path;
import io.helium.disruptor.processor.persistence.PersistenceProcessor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.persistence.inmemory.InMemoryPersistence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PersistenceProcessorTest {
    public static final String BASE_PATH = "http://localhot:8080";
    private InMemoryPersistence persistence;
    private PersistenceProcessor persistenceProcessor;

    @Before
    public void setUp() throws Exception {
        persistence = new InMemoryPersistence(new RuleBasedAuthorization(
                Authorization.ALL_ACCESS_RULE), new Helium(BASE_PATH));
        persistenceProcessor = new PersistenceProcessor(persistence);
    }

    @Test
    public void pushTest() {
        HeliumEvent event = HeliumEventBuilder.start().type(HeliumEventType.PUSH)
                .path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
                .build();
        persistenceProcessor.onEvent(event, 0, false);
        Path path = new Path("/test/test");
        Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
    }

    @Test
    public void setTest() {
        HeliumEvent event = HeliumEventBuilder.start().type(HeliumEventType.SET)
                .path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
                .build();
        persistenceProcessor.onEvent(event, 0, false);
        Path path = new Path("/test/test");
        Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
    }

    @Test
    public void setPriorityTest() {
        HeliumEvent event = HeliumEventBuilder.start()
                .type(HeliumEventType.SETPRIORITY).path(BASE_PATH + "/test/test/")
                .priority("1").build();
        persistenceProcessor.onEvent(event, 0, false);
    }
}
