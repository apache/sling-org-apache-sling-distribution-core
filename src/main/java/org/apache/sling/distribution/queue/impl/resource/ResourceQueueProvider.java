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

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.distribution.queue.impl.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.spi.DistributionQueue;

import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class ResourceQueueProvider implements DistributionQueueProvider {

    private final static String QUEUES_ROOT = "/var/sling/distribution/queues/";
    private ResourceResolverFactory resolverFactory;
    private String serviceName;
    private String agentRootPath;

    private ServiceRegistration cleanupTask;


    public ResourceQueueProvider(BundleContext context, ResourceResolverFactory resolverFactory, String serviceName, String agentName) {
        this.resolverFactory = resolverFactory;
        this.serviceName = serviceName;
        this.agentRootPath = QUEUES_ROOT + agentName;

        register(context);
    }

    @NotNull
    @Override
    public DistributionQueue getQueue(@NotNull String queueName) throws DistributionException {
        return new ResourceQueue(resolverFactory, serviceName, queueName, agentRootPath);
    }

    @NotNull
    @Override
    public DistributionQueue getQueue(@NotNull String queueName, @NotNull DistributionQueueType type) {
        return new ResourceQueue(resolverFactory, serviceName, queueName, agentRootPath);
    }

    @Override
    public void enableQueueProcessing(@NotNull DistributionQueueProcessor queueProcessor, String... queueNames) throws DistributionException {
        // processing not supported
    }

    @Override
    public void disableQueueProcessing() throws DistributionException {
        // processing not supported
    }


    private void register(BundleContext context) {
        Runnable cleanup = new ResourceQueueCleanupTask(resolverFactory, serviceName, agentRootPath);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, false);
        props.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, 300L);
        cleanupTask = context.registerService(Runnable.class.getName(), cleanup, props);
    }

    public void close() {
        if (cleanupTask != null) {
            cleanupTask.unregister();
            cleanupTask = null;
        }
    }
}
