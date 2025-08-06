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
import java.util.Arrays;
import java.util.List;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.SharedDistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default strategy for delivering packages to queues. Each package can be dispatched to multiple queues.
 */
public class MultipleQueueDispatchingStrategy implements DistributionQueueDispatchingStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String[] queueNames;

    public MultipleQueueDispatchingStrategy(String[] queueNames) {
        this.queueNames = Arrays.copyOf(queueNames, queueNames.length);
    }

    public Iterable<DistributionQueueItemStatus> add(@NotNull DistributionPackage distributionPackage,
                                                     @NotNull DistributionQueueProvider queueProvider) throws DistributionException {

        if (!(distributionPackage instanceof SharedDistributionPackage) && queueNames.length > 1) {
            throw new DistributionException("distribution package must be a shared package to be added in multiple queues");
        }

        DistributionQueueItem queueItem = getItem(distributionPackage);
        List<DistributionQueueItemStatus> result = new ArrayList<DistributionQueueItemStatus>();

        // first acquire the package for all queues
        DistributionPackageUtils.acquire(distributionPackage, queueNames);

        // second add the package to all queues
        for (String queueName : queueNames) {
            DistributionQueue queue = queueProvider.getQueue(queueName);
            DistributionQueueItemStatus status = addItemToQueue(queueItem, queue);
            if (null == status) {
                status = new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, queueName);
                DistributionPackageUtils.release(distributionPackage, queueName);
                log.error("cannot add package {} to queue {}", distributionPackage.getId(), queueName);
            }

            result.add(status);
        }

        return result;

    }

    @NotNull
    public List<String> getQueueNames() {
        return Arrays.asList(queueNames);
    }

    protected DistributionQueueItem getItem(DistributionPackage distributionPackage) {
        return DistributionPackageUtils.toQueueItem(distributionPackage);
    }

    protected DistributionQueueItemStatus addItemToQueue(@NotNull DistributionQueueItem queueItem,
            @NotNull DistributionQueue queue) throws DistributionException {

        DistributionQueueItemStatus status = null;

        DistributionQueueEntry queueEntry = queue.add(queueItem);

        if (queueEntry != null) {
            status = queueEntry.getStatus();
        }

        return status;
    }
}