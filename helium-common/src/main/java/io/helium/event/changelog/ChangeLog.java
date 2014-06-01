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

package io.helium.event.changelog;

import com.google.common.collect.Lists;
import io.helium.common.Path;

import java.util.List;

public class ChangeLog {
    private final long sequence;

    private List<ChangeLogEvent> log = Lists.newArrayList();

    public ChangeLog(long sequence) {
        this.sequence = sequence;
    }

    public List<ChangeLogEvent> getLog() {
        return log;
    }

    public void addLog(ChangeLogEvent event) {
        log.add(event);
    }

    public void addChildAddedLogEntry(String name, Path path, Path parent, Object value,
                                      boolean hasChildren, long numChildren, String prevChildName, int priority) {
        log.add(new ChildAddedLogEvent(name, path, parent, value, numChildren, prevChildName, priority));
    }

    public void addChildChangedLogEntry(String name, Path path, Path parent, Object value,
                                        boolean hasChildren, long numChildren, String prevChildName, int priority) {
        if (name != null) {
            log.add(new ChildChangedLogEvent(name, path, parent, value, numChildren, prevChildName,
                    priority));
        }
    }

    public void addValueChangedLogEntry(String name, Path path, Path parent, Object value,
                                        String prevChildName, int priority) {
        log.add(new ValueChangedLogEvent(name, path, parent, value, prevChildName, priority));
    }

    public void addChildRemovedLogEntry(Path path, String name, Object value) {
        log.add(new ChildRemovedLogEvent(path, name, value));
    }

    public void clear() {

        log.clear();
    }

    public long getSequence() {
        return sequence;
    }

    public long size() {
        return log.size();
    }
}
