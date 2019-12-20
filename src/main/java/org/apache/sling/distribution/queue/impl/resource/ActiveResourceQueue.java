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

package org.apache.sling.distribution.queue.impl.resource;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.impl.DistributionQueueUtils;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * ActiveResourceQueue, in conjunction with a queue-processor active only on a single Sling instance,
 * provides an Active, JCR resource-backed queue.
 *
 * It maintains the dequeue attempts in JCR to reflect consistent dequeue attempt status across all cluster instances
 */
public class ActiveResourceQueue extends ResourceQueue {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ActiveResourceQueue(ResourceResolverFactory resolverFactory, String serviceName, String queueName, String rootPath) {
        super(resolverFactory, serviceName, queueName, rootPath);
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            int count = ResourceQueueUtils.getResourceCount(queueRoot);

            DistributionQueueEntry head = ResourceQueueUtils.getHead(queueRoot);
            DistributionQueueItem firstItem = (null != head)? head.getItem(): null;
            DistributionQueueItemStatus firstItemStatus = (null != head)? head.getStatus(): null;
            log.debug("Queue has {} items, with following status for the head: {}",
                    count, firstItemStatus);
            return new DistributionQueueStatus(count,
                    DistributionQueueUtils.calculateState(firstItem, firstItemStatus));
        } catch (LoginException | PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }
}
