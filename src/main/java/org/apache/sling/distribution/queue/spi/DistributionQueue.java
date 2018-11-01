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
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.distribution.queue.impl.DistributionQueueProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An editable distribution queue holding {@link DistributionQueueEntry} distribution queue entries.
 * <p/>
 * A queue is responsible for collecting the {@link DistributionPackage}s
 * exported by a {@link DistributionAgent} in
 * order to be able to process them also when there are multiple (concurrent)
 * {@link org.apache.sling.distribution.DistributionRequest}s executed
 * on that same agent.
 * <p/>
 * The items (packages) in the queue can then get processed according to a FIFO
 * strategy or in parallel, or some other way, via {@link DistributionQueueProcessor}s.
 */
@ConsumerType
public interface DistributionQueue extends ReadOnlyDistributionQueue {

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
     * remove an item from the queue by specifying its id
     *
     * @param itemId the id the item as returned by {@link DistributionQueueItem#getPackageId()}
     * @return the removed item, or {@code null} if the item with the given id
     * doesn't exist
     */
    @Nullable
    DistributionQueueEntry remove(@NotNull String itemId);

}
