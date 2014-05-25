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

/**
 *
 */
package io.helium;

import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.rulebased.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryPersistence;
import io.helium.server.protocols.http.HttpEndpoint;
import io.netty.channel.Channel;
import org.junit.Before;
import org.mockito.Mockito;

/**
 * @author balu
 */
public class HeliumSenderTest {

    private static final String BASE_PATH = "http://localhot:8080";
    private Channel channel;
    private HttpEndpoint endpoint;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.channel = Mockito.mock(Channel.class);
        this.endpoint = new HttpEndpoint(BASE_PATH, new Node(), channel, new InMemoryPersistence(
                new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE)),
                new RuleBasedAuthorization(new Node()), null
        );
    }

    // @Test
    // public void test() throws InterruptedException {
    // endpoint.addListener(new Path("/test/test"), "child_added");
    //
    // endpoint.distribute(HeliumEventBuilder.start().type(HeliumEventType.PUSH)
    // .path(BASE_PATH + "/test/test/asdasd").withNode().put("name",
    // "Hans").complete()
    // .build());
    //
    // Mockito.verify(sender)
    // .send("{\"type\":\"child_added\",\"name\":\"asdasd\",\"path\":\"http://localhot:8080/test/test\",\"parent\":\"http://localhot:8080/test\",\"payload\":{\"name\":\"Hans\"},\"hasChildren\":false,\"numChildren\":0,\"priority\":0}\"");
    // }

    private HeliumEvent createHeliumEvent(String content) {
        return new HeliumEvent(content);
    }

}
