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

package io.helium.core.processor.distribution;

import akka.actor.UntypedActor;
import com.google.common.collect.Sets;
import io.helium.common.Path;
import io.helium.core.Endpoints;
import io.helium.core.Event;
import io.helium.event.HeliumEvent;
import io.helium.event.changelog.ChangeLog;
import io.helium.json.Node;
import io.helium.server.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Distributor extends UntypedActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Distributor.class);

    @Override
    public void onReceive(Object message) throws Exception {
        long startTime = System.currentTimeMillis();
        if(message instanceof  HeliumEvent) {
            HeliumEvent event = (HeliumEvent)message;
            distribute(event);
            distributeChangeLog(event.getChangeLog());
            LOGGER.trace("onEvent ("+event.getSequence()+")"+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
        }
        else if (message instanceof Event) {
            Event event = (Event)message;
            distribute(event.getPath(), event.getEvent());
            LOGGER.trace("distribute" + (System.currentTimeMillis() - startTime) + "ms;");
        }
        else if (message instanceof ChangeLog) {
            ChangeLog log = (ChangeLog)message;
            distributeChangeLog(log);
            LOGGER.trace("distributeChangeLog ("+log.getSequence()+")"+(System.currentTimeMillis()-startTime)+"ms;");
        }
    }

    public void distribute(HeliumEvent event) {
        LOGGER.trace("distributing event: " + event);
        if (!event.isFromHistory()) {
            for (Endpoint handler : Sets.newHashSet(Endpoints.get().endpoints())) {
                handler.distribute(event);
            }
        }
    }

    public void distribute(String path, Node data) {
        for (Endpoint handler : Sets.newHashSet(Endpoints.get().endpoints())) {
            handler.distributeEvent(new Path(HeliumEvent.extractPath(path)), data);
        }
    }


    public void distributeChangeLog(ChangeLog changeLog) {
        for (Endpoint endpoint : Endpoints.get().endpoints()) {
            endpoint.distributeChangeLog(changeLog);
        }
    }
}
