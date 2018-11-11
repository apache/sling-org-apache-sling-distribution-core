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
import org.jetbrains.annotations.Nullable;

/**
 * Trait to be added to a {@link DistributionQueue} distribution
 * queue that supports removing random entries in batch via the
 * {@link Removable#remove} methods.
 *
 * @since 0.1.0
 */
@ConsumerType
public interface Removable {

    /**
     * Remove a set entries from the queue by specifying their identifiers.
     *
     * @param entryIds The identifiers of the entries to be removed
     * @return an iterable over the removed entries
     */
    @NotNull
    Iterable<DistributionQueueEntry> remove(@NotNull Set<String> entryIds);

    /**
     * Remove an entry from the queue by specifying its identifier.
     *
     * @param entryId The identifier of the entry to be removed
     * @return the removed entry or {@code null} if the entry
     *         could not be found
     */
    @Nullable
    DistributionQueueEntry remove(@NotNull String entryId);
}
