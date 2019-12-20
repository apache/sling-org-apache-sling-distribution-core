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
import org.apache.sling.distribution.queue.impl.DistributionQueueProviderFactory;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceQueueProcessingTest {

    public static final Logger log = LoggerFactory.getLogger(ResourceQueueProcessingTest.class);

    protected static final String PACKAGE_ID = "testPackageId";
    protected static BundleContext bundleContext = null;
    protected static ResourceResolverFactory rrf = null;
    protected static Scheduler scheduler = null;
    protected static ScheduledExecutorService executorService = null;

    @Test
    public void testActiveResourceQueue() throws DistributionException, PersistenceException, LoginException {
        // obtain an active queue provider instance
        final String QUEUE_NAME = "testActiveQueue";
        final int MAX_ENTRIES = 32;

        DistributionQueueProvider resourceQueueProvider = new ResourceQueueProvider(bundleContext,
                rrf, "test", "testAgent", scheduler, true);
        DistributionQueueProcessor mockResourceQueueProcessor = mock(DistributionQueueProcessor.class);

        DistributionQueue resourceQueue = resourceQueueProvider.getQueue(QUEUE_NAME);

        try {
            populateDistributionQueue(resourceQueue, MAX_ENTRIES);

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
            resourceQueueProvider.disableQueueProcessing();
            resourceQueue.clear(Integer.MAX_VALUE);
        }
    }

    @Test
    public void testActiveResourceQueueWithoutEnablingProcessing() throws DistributionException {
        final String QUEUE_NAME = "testActiveQueue_2";
        final int MAX_ENTRIES = 2;
        Scheduler tempScheduler = mock(Scheduler.class);
        when(tempScheduler.unschedule(Matchers.anyString())).thenReturn(false);

        DistributionQueueProvider resourceQueueProvider = new ResourceQueueProvider(bundleContext,
                rrf, "test", "testAgent", tempScheduler, true);
        DistributionQueue resourceQueue = resourceQueueProvider.getQueue(QUEUE_NAME);

        try {
            populateDistributionQueue(resourceQueue, MAX_ENTRIES);

            assertTrue("Resource Queue state is not RUNNING",
                    resourceQueue.getStatus().getState().equals(DistributionQueueState.RUNNING));
            assertEquals(MAX_ENTRIES, resourceQueue.getStatus().getItemsCount());
        } finally {
            // should log a WARN for ResourceQueueProvider class
            resourceQueueProvider.disableQueueProcessing();
            resourceQueue.clear(Integer.MAX_VALUE);
        }
    }

    @Test(expected = DistributionException.class)
    public void testPassiveResourceQueueEnableProcessing() throws DistributionException {
        final String QUEUE_NAME = "testPassiveQueue_1";
        final int MAX_ENTRIES = 4;
        DistributionQueueProviderFactory resQueueProviderFactory= new ResourceQueueProviderFactory();
        Whitebox.setInternalState(resQueueProviderFactory, "isActive", false);
        Whitebox.setInternalState(resQueueProviderFactory, "resourceResolverFactory", rrf);
        Whitebox.setInternalState(resQueueProviderFactory, "scheduler", scheduler);
        MockOsgi.activate(resQueueProviderFactory, bundleContext);

        DistributionQueueProvider resourceQueueProvider = resQueueProviderFactory
                .getProvider("test", "testAgent");

        DistributionQueue resourceQueue = resourceQueueProvider.getQueue(QUEUE_NAME, null);

        try {
            populateDistributionQueue(resourceQueue, MAX_ENTRIES);

            assertTrue("Resource Queue state is PASSIVE",
                    resourceQueue.getStatus().getState().equals(DistributionQueueState.PASSIVE));
            assertEquals(MAX_ENTRIES, resourceQueue.getStatus().getItemsCount());

            resourceQueueProvider.enableQueueProcessing(null, QUEUE_NAME); // expect exception
        } finally {
            resourceQueue.clear(Integer.MAX_VALUE);
        }
    }

    @Test(expected = DistributionException.class)
    public void testPassiveResourceQueueDisableProcessing() throws DistributionException {
        final String QUEUE_NAME = "testPassiveQueue_2";
        final int MAX_ENTRIES = 2;
        DistributionQueueProvider resourceQueueProvider = new ResourceQueueProvider(bundleContext,
                rrf, "test", "testAgent", null, false);

        DistributionQueue resourceQueue = resourceQueueProvider.getQueue(QUEUE_NAME, null);

        try {
            populateDistributionQueue(resourceQueue, MAX_ENTRIES);

            assertTrue("Resource Queue state is PASSIVE",
                    resourceQueue.getStatus().getState().equals(DistributionQueueState.PASSIVE));
            assertEquals(MAX_ENTRIES, resourceQueue.getStatus().getItemsCount());
        } finally {
            resourceQueueProvider.disableQueueProcessing(); // expect exception
            resourceQueue.clear(Integer.MAX_VALUE);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQueueProviderConstruction_1() {
        constructIllegalResourceQueueProvider(IllegalQueueProviderType.MISSING_BUNDLE_CONTEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQueueProviderConstruction_2() {
        constructIllegalResourceQueueProvider(IllegalQueueProviderType.MISSING_RESOLVER_FACTORY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQueueProviderConstruction_3() {
        constructIllegalResourceQueueProvider(IllegalQueueProviderType.MISSING_SERVICENAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQueueProviderConstruction_4() {
        constructIllegalResourceQueueProvider(IllegalQueueProviderType.MISSING_AGENTNAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQueueProviderConstruction_5() {
        constructIllegalResourceQueueProvider(IllegalQueueProviderType.MISSING_SCHEDULER_WHEN_ACTIVE);
    }

    private void populateDistributionQueue(DistributionQueue queue, int maxEntries) {
        for (int i = 0; i < maxEntries; i++) {
            queue.add(new DistributionQueueItem(PACKAGE_ID, Collections.<String, Object>emptyMap()));
        }
    }

    private ResourceQueueProvider constructIllegalResourceQueueProvider(IllegalQueueProviderType type) {
        switch(type) {
        case MISSING_BUNDLE_CONTEXT:
            return new ResourceQueueProvider(null,
                    rrf, "test", "testAgent", scheduler, true);
        case MISSING_RESOLVER_FACTORY:
            return new ResourceQueueProvider(bundleContext,
                    null, "test", "testAgent", scheduler, true);
        case MISSING_SERVICENAME:
            return new ResourceQueueProvider(bundleContext,
                    rrf, null, "testAgent", scheduler, true);
        case MISSING_AGENTNAME:
            return new ResourceQueueProvider(bundleContext,
                    rrf, "test", null, scheduler, true);
        case MISSING_SCHEDULER_WHEN_ACTIVE:
        default:
            return new ResourceQueueProvider(bundleContext,
                    rrf, "test", "testAgent", null, true);
        }
    }

    private enum IllegalQueueProviderType {
        MISSING_BUNDLE_CONTEXT,
        MISSING_RESOLVER_FACTORY,
        MISSING_SERVICENAME,
        MISSING_AGENTNAME,
        MISSING_SCHEDULER_WHEN_ACTIVE,
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
        when(scheduler.unschedule(Matchers.anyString())).thenReturn(true);
    }

    @AfterClass
    public static void tearDown() {
        MockSling.clearAdapterManagerBundleContext();
        executorService.shutdownNow();
    }
}
