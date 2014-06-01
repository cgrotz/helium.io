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

package io.helium.core.processor.persistence;

import akka.actor.UntypedActor;
import io.helium.core.processor.persistence.actions.*;
import io.helium.event.HeliumEvent;
import io.helium.persistence.Persistence;
import io.helium.persistence.inmemory.InMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceProcessor extends UntypedActor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceProcessor.class);

    private PushAction pushAction;

    private SetAction setAction;

    private RemoveAction removeAction;

    private SetPriorityAction setPriorityAction;

    private UpdateAction updateAction;

    public PersistenceProcessor() {
        Persistence persistence = InMemoryPersistence.getInstance();
        pushAction = new PushAction(persistence);
        updateAction = new UpdateAction(persistence);
        setAction = new SetAction(persistence);
        removeAction = new RemoveAction(persistence);
        setPriorityAction = new SetPriorityAction(persistence);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        HeliumEvent event = (HeliumEvent)message;
        long startTime = System.currentTimeMillis();
        switch (event.getType()) {
            case PUSH:
                pushAction.handle(event);
                break;
            case UPDATE:
                updateAction.handle(event);
                break;
            case SET:
                setAction.handle(event);
                break;
            case REMOVE:
                removeAction.handle(event);
                break;
            case SETPRIORITY:
                setPriorityAction.handle(event);
                break;
            default:
                break;
        }

        LOGGER.info("onEvent "+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
    }
}
