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

import org.apache.sling.distribution.queue.spi.DistributionQueue;

import java.util.Map;

import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.impl.DistributionQueueProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a simple scheduled {@link SimpleDistributionQueue}s processor
 */
class SimpleDistributionQueueProcessor implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DistributionQueue queue;
    private final DistributionQueueProcessor queueProcessor;
    private final Map<String, DistributionQueueItemStatus> statusMap;

    public SimpleDistributionQueueProcessor(DistributionQueue queue,
                                            DistributionQueueProcessor queueProcessor,
                                            Map<String, DistributionQueueItemStatus> statusMap) {
        this.queue = queue;
        this.queueProcessor = queueProcessor;
        this.statusMap = statusMap;
    }

    public void run() {
        try {
            DistributionQueueEntry entry;
            while ((entry = queue.getHead()) != null) {
                DistributionQueueItemStatus itemStatus = entry.getStatus();
                statusMap.put(entry.getId(),  new DistributionQueueItemStatus(itemStatus.getEntered(),
                        itemStatus.getItemState(), itemStatus.getAttempts() + 1, queue.getName()));
                if (queueProcessor.process(queue.getName(), entry)) {
                    if (queue.remove(entry.getId()) != null) {
                        log.debug("item {} processed and removed from the queue", entry.getItem());
                    }
                } else {
                    log.warn("processing of item {} failed", entry.getId());
                }
            }
        } catch (Exception e) {
            log.error("error while processing queue {}", e);
        }

    }
}
