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
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.impl.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceQueueProcessingTest {

    public static final Logger log = LoggerFactory.getLogger(ResourceQueueProcessingTest.class);

    protected static BundleContext bundleContext = null;
    protected static ResourceResolverFactory rrf = null;
    protected static Scheduler scheduler = null;
    protected static ScheduledExecutorService executorService = null;

    @Test
    public void testActiveResourceQueue() throws DistributionException, PersistenceException, LoginException {
        // obtain an active queue provider instance
        final String QUEUE_NAME = "testQueue";
        final String PACKAGE_ID = "testPackageId";
        final int MAX_ENTRIES = 32;

        DistributionQueueProvider resourceQueueProvider = new ResourceQueueProvider(bundleContext,
                rrf, "test", "testAgent", scheduler, true);
        DistributionQueueProcessor mockResourceQueueProcessor = mock(DistributionQueueProcessor.class);

        DistributionQueue resourceQueue = resourceQueueProvider.getQueue(QUEUE_NAME);

        try {
            for (int i = 0; i < MAX_ENTRIES; i++) {
                resourceQueue.add(new DistributionQueueItem(PACKAGE_ID, Collections.<String, Object>emptyMap()));
            }

            assertTrue("Resource Queue state is not RUNNING",
                    resourceQueue.getStatus().getState().equals(DistributionQueueState.RUNNING));
            assertEquals(MAX_ENTRIES, resourceQueue.getStatus().getItemsCount());

            when(mockResourceQueueProcessor.process(eq(QUEUE_NAME), Matchers.any(DistributionQueueEntry.class)))
                .thenReturn(true);

            resourceQueueProvider.enableQueueProcessing(mockResourceQueueProcessor, QUEUE_NAME);
            while (!resourceQueue.getStatus().getState().equals(DistributionQueueState.IDLE)) {
                // do nothing, wait for processing to finish
                log.info("Processing Resource Queue. Items remaining: {}",
                        resourceQueue.getStatus().getItemsCount());
            }

            assertEquals(0, resourceQueue.getStatus().getItemsCount());
        } finally {
            resourceQueue.clear(Integer.MAX_VALUE);
        }
    }

    @BeforeClass
    public static void setUp() throws LoginException {
        bundleContext = MockOsgi.newBundleContext();
        MockSling.setAdapterManagerBundleContext(bundleContext);
        rrf = MockSling.newResourceResolverFactory(ResourceResolverType.JCR_OAK, bundleContext);
        scheduler = mock(Scheduler.class);
        ScheduleOptions mockScheduleOptions = mock(ScheduleOptions.class);
        when(mockScheduleOptions.canRunConcurrently(Matchers.anyBoolean())).thenReturn(mockScheduleOptions);
        when(mockScheduleOptions.onSingleInstanceOnly(Matchers.anyBoolean())).thenReturn(mockScheduleOptions);
        when(mockScheduleOptions.name(Matchers.anyString())).thenReturn(mockScheduleOptions);
        executorService = Executors.newSingleThreadScheduledExecutor();
        when(scheduler.NOW(Matchers.anyInt(), Matchers.anyLong())).thenReturn(mockScheduleOptions);
        when(scheduler.schedule(Matchers.any(Runnable.class), Matchers.any(ScheduleOptions.class)))
            .thenAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {
                    Runnable task = (Runnable) invocation.getArguments()[0];
                    executorService.scheduleAtFixedRate(task, 0L, 1L, TimeUnit.SECONDS);
                    return true;
                }
            });
    }

    @AfterClass
    public static void tearDown() {
        MockSling.clearAdapterManagerBundleContext();
        executorService.shutdownNow();
    }
}
