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
package org.apache.sling.distribution.queue.impl.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.distribution.queue.impl.DistributionQueueUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.APPENDABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.REMOVABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.CLEARABLE;

/**
 * A simple implementation of a {@link DistributionQueue}.
 * <p/>
 * Note that, at the moment, this is a transient in memory queue not persisted on the repository and
 * therefore not usable for production.
 *
 * Note: potentially the Queue could contain the ordered package ids, with a sidecar map id->item;
 * that way removal could be faster.
 */
public class SimpleDistributionQueue implements DistributionQueue {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Set<String> CAPABILITIES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(APPENDABLE, REMOVABLE, CLEARABLE)));

    private final String name;

    private final Queue<DistributionQueueItem> queue;

    private final Map<DistributionQueueItem, DistributionQueueItemStatus> statusMap;

    public SimpleDistributionQueue(String agentName, String name) {
        log.debug("starting a simple queue {} for agent {}", name, agentName);
        this.name = name;
        this.queue = new LinkedBlockingQueue<DistributionQueueItem>();
        this.statusMap = new WeakHashMap<DistributionQueueItem, DistributionQueueItemStatus>(10);
    }

    @NotNull
    public String getName() {
        return name;
    }

    public DistributionQueueEntry add(@NotNull DistributionQueueItem item) {
        DistributionQueueItemState itemState = DistributionQueueItemState.ERROR;
        boolean result = false;
        try {
            result = queue.offer(item);
            itemState = DistributionQueueItemState.QUEUED;
        } catch (Exception e) {
            log.error("cannot add an item to the queue", e);
        } finally {
            statusMap.put(item, new DistributionQueueItemStatus(Calendar.getInstance(), itemState, 0, name));
        }

        if (result) {
            return new DistributionQueueEntry(item.getPackageId(), item, statusMap.get(item));
        }

        return null;
    }


    @Nullable
    public DistributionQueueEntry getHead() {
        DistributionQueueItem element = queue.peek();
        if (element != null) {
            DistributionQueueItemStatus itemState = statusMap.get(element);

            return new DistributionQueueEntry(element.getPackageId(), element, itemState);
        }
        return null;
    }

    @NotNull
    private DistributionQueueState getState() {
        DistributionQueueItem firstItem = queue.peek();
        DistributionQueueItemStatus firstItemStatus = firstItem != null ? statusMap.get(firstItem) : null;
        return DistributionQueueUtils.calculateState(firstItem, firstItemStatus);
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {
        return new DistributionQueueStatus(queue.size(), getState());
    }

    @NotNull
    @Override
    public DistributionQueueType getType() {
        return DistributionQueueType.ORDERED;
    }

    @Override
    public boolean hasCapability(@NotNull String capability) {
        return CAPABILITIES.contains(capability);
    }


    @NotNull
    public Iterable<DistributionQueueEntry> getEntries(int skip, int limit) {
        List<DistributionQueueEntry> result = new ArrayList<DistributionQueueEntry>();

        for (DistributionQueueItem item : queue) {
            result.add(new DistributionQueueEntry(item.getPackageId(), item, statusMap.get(item)));
        }
        return result;
    }

    @Nullable
    public DistributionQueueEntry getEntry(@NotNull String id) {
        for (DistributionQueueItem item : queue) {
            if (id.equals(item.getPackageId())) {
                return new DistributionQueueEntry(id, item, statusMap.get(item));
            }
        }

        return null;
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

    @Nullable
    public DistributionQueueEntry remove(@NotNull String id) {
        DistributionQueueEntry toRemove = getEntry(id);

        boolean removed = false;
        if (toRemove != null) {
            removed = queue.remove(toRemove.getItem());
        }
        log.debug("item with id {} removed from the queue: {}", id, removed);
        if (removed) {
            return toRemove;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SimpleDistributionQueue{" +
                "name='" + name + '\'' +
                '}';
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> clear(int limit) {
        final List<DistributionQueueEntry> removedEntries = new ArrayList<DistributionQueueEntry>();
        for (DistributionQueueEntry entry : getEntries(0, limit)) {
            DistributionQueueEntry removed = remove(entry.getId());
            if (removed != null) {
                removedEntries.add(removed);
            }
        }
        return removedEntries;
    }

    public void recordProcessingAttempt(@NotNull DistributionQueueEntry entry) {
        statusMap.computeIfPresent(entry.getItem(), (item, status) -> {
            return new DistributionQueueItemStatus(status.getEntered(),
                    status.getItemState(), status.getAttempts() + 1, status.getQueueName());
        });
    }
}
