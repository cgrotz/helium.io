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

import io.helium.common.Path;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.persistence.authorization.rule.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryPersistence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class PersistenceTest {

    private InMemoryPersistence persistence;

    @Before
    public void setUp() throws Exception {
        persistence = new InMemoryPersistence();
        persistence.setAuthorization(new RuleBasedAuthorization(persistence));
    }

    @Test
    public void setSimpleValueTest() {
        Path path = new Path("/test/test");
        Assert.assertNotNull(persistence.get(path));
        Assert.assertNotNull(persistence.getNode(path));
        persistence.applyNewValue(new ChangeLog(), Optional.empty(), path.append("msg"), 1, "HalloWelt");
        Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
    }

    @Test
    public void setNodeValueTest() {
        Path path = new Path("/test/test");
        Assert.assertNotNull(persistence.get(path));
        Assert.assertNotNull(persistence.getNode(path));
        persistence.applyNewValue(new ChangeLog(), Optional.empty(), path, 2,
                new Node().put("msg", "HalloWelt"));
        Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
        persistence.applyNewValue(new ChangeLog(), Optional.empty(), path.append("msg"), 1, "HalloWelt2");
        Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt2");
    }
}
