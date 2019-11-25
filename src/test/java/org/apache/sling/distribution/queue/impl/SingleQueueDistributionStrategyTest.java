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

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.spi.DistributionQueueProvider;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SingleQueueDispatchingStrategy}
 */
public class SingleQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        DistributionQueueItemStatus state = newDistributionQueueItemStatus();
        DistributionQueueItem queueItem = new DistributionQueueItem("packageId", Collections.<String, Object>emptyMap());
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(new DistributionQueueEntry("entryId", queueItem, state));

        Iterable<DistributionQueueItemStatus> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem queueItem = new DistributionQueueItem("packageId", new HashMap<String, Object>());
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        DistributionQueueItemStatus state = newDistributionQueueItemStatus();
        when(queue.add(queueItem)).thenReturn(new DistributionQueueEntry(null, queueItem, state));
        Iterable<DistributionQueueItemStatus> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        DistributionQueueItemStatus state = newDistributionQueueItemStatus();
        DistributionQueueItem queueItem = new DistributionQueueItem("packageId", Collections.<String, Object>emptyMap());
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(new DistributionQueueEntry("entryId", queueItem, state));

        Iterable<DistributionQueueItemStatus> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    private DistributionQueueItemStatus newDistributionQueueItemStatus() {
        return new DistributionQueueItemStatus(Calendar.getInstance(), DistributionQueueItemState.QUEUED, 0, DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME);
    }
}
