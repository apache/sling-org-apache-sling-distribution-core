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

import java.util.Set;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the {@link DistributionQueueItem} queue items in a sequential queue.
 */
@ConsumerType
public interface DistributionQueue {

    /**
     * get this queue name
     *
     * @return the queue name
     */
    @NotNull
    String getName();

    /**
     * add a distribution item to this queue
     *
     * @param item a distribution item, typically representing a {@link DistributionPackage}
     *             to distribute
     * @return the queue entry created for this item or {@code noll} if none is created
     */
    @Nullable
    DistributionQueueEntry add(@NotNull DistributionQueueItem item);

    /**
     * get the first entry (in a FIFO strategy, the next to be processed) from the queue
     *
     * @return the first entry into the queue or {@code null} if the queue is empty
     */
    @Nullable
    DistributionQueueEntry getHead();

    /**
     * get all the entries in the queue
     *
     * @param skip the number of entries to skip
     * @param limit the maximum number of entries to return. use -1 to return all entries.
     * @return a {@link java.lang.Iterable} of {@link DistributionQueueEntry} entries
     */
    @NotNull
    Iterable<DistributionQueueEntry> getEntries(int skip, int limit);

    /**
     * gets an entry from the queue by specifying its id
     *
     * @param entryId the entry identifier
     * @return the entry, or {@code null} if the entry with the given id
     * doesn't exist
     */
    @Nullable
    DistributionQueueEntry getEntry(@NotNull String entryId);

    /**
     * remove an entry from the queue by specifying its id
     *
     * @param entryId the entry identifier
     * @return the removed entry, or {@code null} if the entry with the given id
     * doesn't exist
     */
    @Nullable
    DistributionQueueEntry remove(@NotNull String entryId);

    /**
     * Remove a set entries from the queue by specifying their identifiers.
     *
     * @param entryIds The identifiers of the entries to be removed
     * @return an iterable over the removed entries
     */
    @NotNull
    Iterable<DistributionQueueEntry> remove(@NotNull Set<String> entryIds);

    /**
     * Clear a range of entries from the queue. The range starts from
     * the head entry, includes the specified #limit number of entries.
     *
     * @param limit The maximum number of entries to remove. All entries
     *              are removed when the limit is {@code -1}.
     * @return an iterable over the removed entries
     */
    @NotNull
    Iterable<DistributionQueueEntry> clear(int limit);

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

    /**
     * @return {@code true} if the queue supports the capability ;
     *         {@code false} otherwise
     */
    boolean hasCapability(@NotNull String capability);
}
