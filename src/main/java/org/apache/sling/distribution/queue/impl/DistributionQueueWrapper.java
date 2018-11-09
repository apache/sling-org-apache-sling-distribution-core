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
import org.jetbrains.annotations.NotNull;

public abstract class DistributionQueueWrapper implements DistributionQueue, Clearable {
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

    @Override
    public @NotNull Iterable<DistributionQueueEntry> clear(@NotNull Set<String> itemIds) {
        List<DistributionQueueEntry> removed = new ArrayList<DistributionQueueEntry>();
        for (String itemId : itemIds) {
            DistributionQueueEntry entry = remove(itemId);
            if (entry!= null) {
                removed.add(entry);
            }
        }
        return removed;
    }

    @Override
    public void clear() {
        for (DistributionQueueEntry queueEntry : getItems(0, -1)) {
            remove(queueEntry.getId());
        }
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {
        return wrappedQueue.getStatus();
    }
}
