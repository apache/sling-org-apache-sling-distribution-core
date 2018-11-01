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
package org.apache.sling.distribution.queue.spi;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A read only distribution queue holding {@link DistributionQueueEntry} distribution queue entries.
 *
 * @since 0.1.0
 */
@ConsumerType
public interface ReadOnlyDistributionQueue {

    /**
     * get this queue name
     *
     * @return the queue name
     */
    @NotNull
    String getName();

    /**
     * get the first queue entry (in a FIFO strategy, the next to be processed) from the queue
     *
     * @return the first entry into the queue or {@code null} if the queue is empty
     */
    @Nullable
    DistributionQueueEntry getHead();

    /**
     * get all the entries in the queue
     *
     * @param skip the number of items to skip
     * @param limit the maximum number of items to return. use -1 to return all items.
     * @return a {@link java.lang.Iterable} of {@link DistributionQueueItem}s
     */
    @NotNull
    Iterable<DistributionQueueEntry> getItems(int skip, int limit);

    /**
     * gets an entry from the queue by specifying its id
     *
     * @param itemId the id of the item as returned by {@link DistributionQueueItem#getPackageId()}
     * @return the entry, or {@code null} if the entry with the given id
     * doesn't exist
     */
    @Nullable
    DistributionQueueEntry getItem(@NotNull String itemId);

    /**
     * get the status of the queue
     * @return the queue status
     */
    @NotNull
    DistributionQueueStatus getStatus();

    /**
     * get the type of this queue
     * @return the type
     */
    @NotNull
    DistributionQueueType getType();

}
