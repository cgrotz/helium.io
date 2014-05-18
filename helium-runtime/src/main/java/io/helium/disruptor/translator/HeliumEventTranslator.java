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

package io.helium.disruptor.translator;

import com.lmax.disruptor.EventTranslator;
import io.helium.event.HeliumEvent;

public class HeliumEventTranslator implements EventTranslator<HeliumEvent> {
    private HeliumEvent heliumEvent;
    private long sequence;

    public HeliumEventTranslator(HeliumEvent heliumEvent) {
        this.heliumEvent = heliumEvent;
    }

    public long getSequence() {
        return sequence;
    }

    @Override
    public void translateTo(HeliumEvent event, long sequence) {
        event.clear();
        event.populate(heliumEvent.toString());
        this.sequence = sequence;
    }
}