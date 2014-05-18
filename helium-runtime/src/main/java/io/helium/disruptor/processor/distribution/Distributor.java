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

package io.helium.disruptor.processor.distribution;

import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.messaging.HeliumEventDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Distributor implements EventHandler<HeliumEvent> {

    private static final Logger logger = LoggerFactory.getLogger(Distributor.class);

    private Set<HeliumEventDistributor> handlers = Sets.newHashSet();

    private long sequence;

    @Override
    public void onEvent(HeliumEvent event, long sequence, boolean endOfBatch) {
        this.sequence = sequence;
        distribute(event);
    }

    public void distribute(HeliumEvent event) {
        logger.trace("distributing event: " + event);
        if (!event.isFromHistory()) {
            for (HeliumEventDistributor handler : Sets.newHashSet(handlers)) {
                handler.distribute(event);
            }
        }
    }

    public void distribute(String path, Node data) {
        for (HeliumEventDistributor handler : Sets.newHashSet(handlers)) {
            handler.distributeEvent(new Path(HeliumEvent.extractPath(path)), data);
        }
    }

    public void addHandler(HeliumEventDistributor handler) {
        handlers.add(handler);
    }

    public void removeHandler(HeliumEventDistributor handler) {
        handlers.remove(handler);
    }

    public long getSequence() {
        return sequence;
    }
}
