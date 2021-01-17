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
package org.apache.sling.distribution.monitor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.felix.hc.api.Result;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DistributionQueueHealthCheck}
 */
public class DistributionQueueHealthCheckTest {
    
    @Rule
    public final OsgiContext context = new OsgiContext();

    @Test
    public void testWithNoDistributionQueueProvider() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();
        context.registerInjectActivateService(distributionQueueHealthCheck);
        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNoItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        context.registerInjectActivateService(distributionQueueHealthCheck);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queue.getHead()).thenReturn(null);
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);

        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithOneOkItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        context.registerInjectActivateService(distributionQueueHealthCheck);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem item = new DistributionQueueItem("packageId", new HashMap<String, Object>());
        DistributionQueueItemStatus status = new DistributionQueueItemStatus(Calendar.getInstance(), DistributionQueueItemState.QUEUED, 1, "queueName");
        when(queue.getEntry(any(String.class))).thenReturn(new DistributionQueueEntry(null, item, status));
        when(queue.getHead()).thenReturn(new DistributionQueueEntry(null, item, status));
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);


        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNotOkItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        context.registerInjectActivateService(distributionQueueHealthCheck);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem item = new DistributionQueueItem("packageId", new HashMap<String, Object>());
        DistributionQueueItemStatus status = new DistributionQueueItemStatus(Calendar.getInstance(), DistributionQueueItemState.QUEUED, 10, "queueName");
        when(queue.getEntry(any(String.class))).thenReturn(new DistributionQueueEntry(null, item, status));
        when(queue.getHead()).thenReturn(new DistributionQueueEntry(null, item, status));
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);

        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertFalse(result.isOk());
    }
}
