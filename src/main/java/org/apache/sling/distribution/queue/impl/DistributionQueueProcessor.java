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

import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Processor of {@link DistributionQueueItem}s
 */
@ProviderType
public interface DistributionQueueProcessor {

    /**
     * Process an item from a certain {@link DistributionQueue}
     *
     * @param queueName            the name of the {@link DistributionQueue} to be processed
     * @param queueEntry the {@link org.apache.sling.distribution.queue.DistributionQueueEntry} to be processed
     * @return {@code true} if the item was successfully processed, {@code false} otherwise
     */
    boolean process(@NotNull String queueName, @NotNull DistributionQueueEntry queueEntry);
}
