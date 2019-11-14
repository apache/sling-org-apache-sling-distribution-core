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
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.distribution.queue.impl.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueueProcessor;
import org.apache.sling.distribution.queue.spi.DistributionQueue;

import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ResourceQueueProvider implements DistributionQueueProvider {
    public final static String TYPE = "resource";

    private final static String QUEUES_ROOT = "/var/sling/distribution/queues/";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ResourceResolverFactory resolverFactory;
    private String serviceName;
    private String agentRootPath;
    private Scheduler scheduler;
    private boolean isActive;

    private final Map<String, ResourceQueue> queueMap = new ConcurrentHashMap<String, ResourceQueue>();

    private ServiceRegistration<Runnable> cleanupTask;

    public ResourceQueueProvider(BundleContext context, ResourceResolverFactory resolverFactory,
            String serviceName, String agentName, Scheduler scheduler, boolean isActive) {
        if (serviceName == null || scheduler == null
                || context == null || resolverFactory == null || agentName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        this.resolverFactory = resolverFactory;
        this.serviceName = serviceName;
        this.agentRootPath = QUEUES_ROOT + agentName;
        this.scheduler = scheduler;
        this.isActive = isActive;

        register(context);
    }

    @NotNull
    @Override
    public DistributionQueue getQueue(@NotNull String queueName) throws DistributionException {
        return queueMap.computeIfAbsent(queueName, (name) -> {
            if (isActive) {
                return new ActiveResourceQueue(resolverFactory, serviceName, name, agentRootPath);
            } else {
                return new ResourceQueue(resolverFactory, serviceName, name, agentRootPath);
            }
        });
    }

    @NotNull
    @Override
    public DistributionQueue getQueue(@NotNull String queueName, @NotNull DistributionQueueType type) {
        try {
            return getQueue(queueName);
        } catch (DistributionException e) {
            throw new RuntimeException("could not create config for queue " + queueName, e);
        }
    }

    @Override
    public void enableQueueProcessing(@NotNull DistributionQueueProcessor queueProcessor, String... queueNames) throws DistributionException {
        // enable processing only for active ResourceQueues
        if (isActive) {
            for (String queueName : queueNames) {
                ScheduleOptions options = scheduler.NOW(-1, 1)
                        .canRunConcurrently(false)
                        .onSingleInstanceOnly(true)
                        .name(getJobName(queueName));
                scheduler.schedule(new SimpleDistributionQueueProcessor(getQueue(queueName), queueProcessor), options);
            }
        } else {
            throw new DistributionException(new UnsupportedOperationException("enable Processing not supported for Passive Queues"));
        }
    }

    @Override
    public void disableQueueProcessing() throws DistributionException {
        // disable processing only for active ResourceQueues
        if (isActive) {
            for (DistributionQueue queue : queueMap.values()) {
                String queueName = queue.getName();
                // disable queue processing
                if (scheduler.unschedule(getJobName(queueName))) {
                    log.debug("queue processing on {} stopped", queue);
                } else {
                    log.warn("could not disable queue processing on {}", queue);
                }
            }
        } else {
            throw new DistributionException(new UnsupportedOperationException("disable Processing not supported for Passive Queues"));
        }
    }

    private String getJobName(String queueName) {
        return "resource-queueProcessor-" + serviceName + "-" + queueName;
    }


    private void register(BundleContext context) {
        Runnable cleanup = new ResourceQueueCleanupTask(resolverFactory, serviceName, agentRootPath);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, false);
        props.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, 300L);
        cleanupTask = context.registerService(Runnable.class, cleanup, props);
    }

    public void close() {
        if (cleanupTask != null) {
            cleanupTask.unregister();
            cleanupTask = null;
        }
    }
}
