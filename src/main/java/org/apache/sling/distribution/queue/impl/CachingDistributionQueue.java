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

import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/**
 * {@link DistributionQueueWrapper} that caches entries for 30s.
 */
public class CachingDistributionQueue extends DistributionQueueWrapper {
    // cache status for 30 sec as it is expensive to count items
    private static final int EXPIRY_QUEUE_CACHE = 30 * 1000;

    private static final Map<String, DistributionQueueStatus> queueCache = new ConcurrentHashMap<String, DistributionQueueStatus>();
    private static final Map<String, Long> queueCacheExpiry = new ConcurrentHashMap<String, Long>();
    private final String cacheKey;

    public CachingDistributionQueue(String cacheKey, DistributionQueue wrappedQueue) {
        super(wrappedQueue);
        this.cacheKey = cacheKey;
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {

        DistributionQueueStatus queueStatus = null;
        long now = System.currentTimeMillis();

        Long expiryDate = queueCacheExpiry.get(cacheKey);
        if (expiryDate != null && expiryDate < now) {
            queueCache.remove(cacheKey);
            queueCacheExpiry.remove(cacheKey);
        }

        queueStatus = queueCache.get(cacheKey);

        if (queueStatus != null) {
            return queueStatus;
        }

        queueStatus = wrappedQueue.getStatus();

        queueCache.put(cacheKey, queueStatus);
        queueCacheExpiry.put(cacheKey,  System.currentTimeMillis() + EXPIRY_QUEUE_CACHE);

        return queueStatus;
    }

    @Override
    public DistributionQueueType getType() {
        return wrappedQueue.getType();
    }

    @Override
    public DistributionQueueEntry add(@NotNull DistributionQueueItem item) {
        queueCache.remove(cacheKey);
        queueCacheExpiry.remove(cacheKey);
        return super.add(item);
    }

    @Override
    public DistributionQueueEntry remove(@NotNull String itemId) {
        queueCache.remove(cacheKey);
        queueCacheExpiry.remove(cacheKey);
        return super.remove(itemId);
    }

    @Override
    public @NotNull Iterable<DistributionQueueEntry> clear(@NotNull Set<String> itemIds) {
        queueCache.remove(cacheKey);
        queueCacheExpiry.remove(cacheKey);
        return super.clear(itemIds);
    }

    @Override
    public void clear() {
        queueCache.remove(cacheKey);
        queueCacheExpiry.remove(cacheKey);
        super.clear();
    }
}
