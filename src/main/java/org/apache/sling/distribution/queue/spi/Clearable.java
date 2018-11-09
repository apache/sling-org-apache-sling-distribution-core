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
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Trait to be added to a {@link DistributionQueue} distribution
 * queue that supports clearing all or a batch of items via the
 * {@link Clearable#clear} methods.
 *
 * @since 0.1.0
 */
@ConsumerType
public interface Clearable {

    /**
     * Clear all items from the the queue.
     */
    void clear();

    /**
     * Clear a set of items from the queue.
     *
     * @param itemIds the set of item identifiers to be cleared
     * @return the removed items
     */
    @NotNull
    Iterable<DistributionQueueEntry> clear(@NotNull Set<String> itemIds);

}
