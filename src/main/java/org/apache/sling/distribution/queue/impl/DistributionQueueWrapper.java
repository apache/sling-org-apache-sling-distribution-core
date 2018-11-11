/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.queue.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.sling.distribution.queue.spi.Clearable;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.spi.Removable;
import org.jetbrains.annotations.NotNull;

public abstract class DistributionQueueWrapper implements DistributionQueue, Clearable, Removable {
    final DistributionQueue wrappedQueue;

    DistributionQueueWrapper(DistributionQueue wrappedQueue) {

        this.wrappedQueue = wrappedQueue;
    }

    @NotNull
    @Override
    public String getName() {
        return wrappedQueue.getName();
    }

    @Override
    public DistributionQueueEntry add(@NotNull DistributionQueueItem item) {
        return wrappedQueue.add(item);
    }

    @Override
    public DistributionQueueEntry getHead() {
        return wrappedQueue.getHead();
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> getItems(int skip, int limit) {
        return wrappedQueue.getItems(skip, limit);
    }

    @Override
    public DistributionQueueEntry getItem(@NotNull String itemId) {
        return wrappedQueue.getItem(itemId);
    }

    @Override
    public DistributionQueueEntry remove(@NotNull String itemId) {
        return wrappedQueue.remove(itemId);
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {
        return wrappedQueue.getStatus();
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> remove(@NotNull Set<String> entryIds) {
        List<DistributionQueueEntry> removed = new ArrayList<DistributionQueueEntry>();
        for (String entryId : entryIds) {
            DistributionQueueEntry entry = remove(entryId);
            if (entry != null) {
                removed.add(entry);
            }
        }
        return removed;
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> clear(int limit) {
        final List<DistributionQueueEntry> removedEntries = new ArrayList<DistributionQueueEntry>();
        for (DistributionQueueEntry entry : getItems(0, limit)) {
            DistributionQueueEntry removed = remove(entry.getId());
            if (removed != null) {
                removedEntries.add(removed);
            }
        }
        return removedEntries;
    }
}
