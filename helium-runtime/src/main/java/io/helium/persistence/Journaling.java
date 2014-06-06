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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import io.helium.event.HeliumEvent;
import journal.io.api.Journal;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import java.io.File;
import java.util.Optional;

public class Journaling extends Verticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Journaling.class);
    public static final String SUBSCRIPTION = "eventsource";

    private Journal journal = new Journal();
    private Optional<Location> currentLocation = Optional.empty();

    @Override
    public void start() {
        try {
            String directory = container.config().getString("directory", "helium");
            File file = new File(Strings.isNullOrEmpty(directory) ? "helium/journal" : directory);
            Files.createParentDirs(new File(file, ".helium"));
            journal.setDirectory(file);
            journal.open();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        vertx.eventBus().registerHandler(SUBSCRIPTION, this::onReceive);
    }

    public void onReceive(Object message) {
        try {
            HeliumEvent event = (HeliumEvent) message;

            long startTime = System.currentTimeMillis();
            if (!event.isFromHistory() || event.isNoAuth()) {
                LOGGER.info("storing event: " + event);
                Location write = journal.write(event.toString().getBytes(), WriteType.SYNC);
                journal.sync();
                currentLocation = Optional.of(write);
            }
            LOGGER.info("onEvent " + (System.currentTimeMillis() - startTime) + "ms; event processing time " + (System.currentTimeMillis() - event.getLong("creationDate")) + "ms");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
}
