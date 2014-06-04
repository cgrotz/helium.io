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

package io.helium.core.processor.authorization;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinPool;
import io.helium.Helium;
import io.helium.common.Path;
import io.helium.core.processor.eventsourcing.EventSourceProcessor;
import io.helium.core.processor.persistence.PersistenceProcessor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.authorization.chained.ChainedAuthorization;
import io.helium.persistence.authorization.rule.RuleBasedAuthorization;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Optional;

public class AuthorizationProcessor extends UntypedActor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationProcessor.class);

    private ActorRef eventSourceActor = getContext().actorOf(new RoundRobinPool(1).props(Props.create(EventSourceProcessor.class)), "eventSourceActor");
    private ActorRef persistenceActor = getContext().actorOf(new RoundRobinPool(5).props(Props.create(PersistenceProcessor.class)), "persistenceActor");

    private Authorization authorization;

    private Persistence persistence;

    public AuthorizationProcessor() {
        this.persistence = Helium.getPersistence();
        this.authorization = new ChainedAuthorization(new RuleBasedAuthorization(persistence));
    }

    @Override
    public void onReceive(Object message) throws Exception {
        long startTime = System.currentTimeMillis();
        HeliumEvent event = (HeliumEvent)message;
        if(event.isFromHistory()) {
            distribute(message);
            return;
        }
        LOGGER.trace("checking auth for ("+event.getSequence()+"): ", message);
        Path path = event.extractNodePath();
        if (event.getType() == HeliumEventType.PUSH) {
            InMemoryDataSnapshot data = new InMemoryDataSnapshot( event.get(HeliumEvent.PAYLOAD, null));
            if (authorization.isAuthorized(Operation.WRITE, getAuth(event), path, data)) {
                distribute(message);
                LOGGER.trace("authorized ("+event.getSequence()+"): ", message);
            }
            else {
                LOGGER.warn("not authorized ("+event.getSequence()+"): ", message);
            }
        } else if (event.getType() == HeliumEventType.SET) {
            if (event.has(HeliumEvent.PAYLOAD) && event.get(HeliumEvent.PAYLOAD) == HashMapBackedNode.NULL) {
                InMemoryDataSnapshot data = new InMemoryDataSnapshot( event.get(HeliumEvent.PAYLOAD));
                if (authorization.isAuthorized(Operation.WRITE, getAuth(event), path, data)){
                    distribute(message);
                    LOGGER.trace("authorized ("+event.getSequence()+"): ", message);
                }
                else {
                    LOGGER.warn("not authorized ("+event.getSequence()+"): ", message);
                }
            } else {
                if (authorization.isAuthorized(Operation.WRITE, getAuth(event), path, null)){
                    distribute(message);
                    LOGGER.trace("authorized ("+event.getSequence()+"): ", message);
                }
                else {
                    LOGGER.warn("not authorized ("+event.getSequence()+"): ", message);
                }
            }
        }
        LOGGER.trace("onEvent ("+event.getSequence()+")"+(System.currentTimeMillis()-startTime)+"ms; event processing time "+(System.currentTimeMillis()-event.getLong("creationDate"))+"ms");
    }

    private void distribute(Object message) {
        eventSourceActor.tell(message, getSelf());
        persistenceActor.tell(message, ActorRef.noSender());
    }

    private Optional<Node> getAuth(HeliumEvent event) {
        if (event.has(HeliumEvent.AUTH)) {
            return Optional.of(event.getNode(HeliumEvent.AUTH));
        } else {
            return Optional.empty();
        }
    }

    public static Node decode(String authorizationToken) {
        String decodedAuthorizationToken = new String(Base64.getDecoder().decode(authorizationToken.substring(6)));
        String username = decodedAuthorizationToken.substring(0, decodedAuthorizationToken.indexOf(":"));
        String password = decodedAuthorizationToken.substring(decodedAuthorizationToken.indexOf(":")+1);
        return new HashMapBackedNode().put("username",username).put("password", password);
    }
}
