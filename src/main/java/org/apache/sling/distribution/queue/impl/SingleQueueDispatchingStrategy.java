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

import java.util.Collections;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default strategy for delivering packages to queues. Each agent just manages a single queue,
 * no failure / stuck handling where each package is put regardless of anything.
 */
public class SingleQueueDispatchingStrategy extends MultipleQueueDispatchingStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String DEFAULT_QUEUE_NAME = DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME;

    private final String queueName;

    public SingleQueueDispatchingStrategy(String queueName) {
        super(new String[]{queueName});
        this.queueName = queueName;
    }

    public SingleQueueDispatchingStrategy() {
        this(DEFAULT_QUEUE_NAME);
    }

    @Override
    public Iterable<DistributionQueueItemStatus> add(@NotNull DistributionPackage distributionPackage,
            @NotNull DistributionQueueProvider queueProvider) throws DistributionException {
        if (!(distributionPackage instanceof SharedDistributionPackage)) {
            throw new DistributionException("distribution package must be a shared package to be added in multiple queues");
        }

        DistributionQueueItem queueItem = getItem(distributionPackage);

        DistributionQueue queue = queueProvider.getQueue(queueName);
        DistributionQueueItemStatus status = addItemToQueue(queueItem, queue);
        if (null == status) {
            status = new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, queueName);
            log.error("cannot add package {} to queue {}", distributionPackage.getId(), queueName);
        }

        return Collections.singletonList(status);
    }
}
