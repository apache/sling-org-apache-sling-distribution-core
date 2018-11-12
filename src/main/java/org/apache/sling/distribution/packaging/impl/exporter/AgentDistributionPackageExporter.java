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
package org.apache.sling.distribution.packaging.impl.exporter;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.log.spi.DistributionLog;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.impl.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageBuilderProvider;
import org.apache.sling.distribution.packaging.impl.DistributionPackageWrapper;
import org.apache.sling.distribution.packaging.impl.SimpleDistributionPackage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link DistributionPackageExporter} that picks {@link DistributionPackage} from a specific {@link DistributionAgent}'s
 * queue.
 */
public class AgentDistributionPackageExporter implements DistributionPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DistributionPackageBuilderProvider packageBuilderProvider;
    private final String name;
    private boolean dropInvalidItems;

    private final static String PACKAGE_TYPE = "agentexporter";

    private DistributionAgent agent;
    private String queueName;

    public AgentDistributionPackageExporter(String queueName, DistributionAgent agent, DistributionPackageBuilderProvider packageBuilderProvider, String name, boolean dropInvalidItems) {
        this.packageBuilderProvider = packageBuilderProvider;
        this.name = name;
        this.dropInvalidItems = dropInvalidItems;

        if (agent == null || packageBuilderProvider == null) {
            throw new IllegalArgumentException("Agent and package builder are required");
        }
        this.queueName = queueName;
        this.agent = agent;
    }

    public void exportPackages(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionPackageProcessor packageProcessor) throws DistributionException {

        if (DistributionRequestType.TEST.equals(distributionRequest.getRequestType())) {
            packageProcessor.process(new SimpleDistributionPackage(distributionRequest, PACKAGE_TYPE));
            return;
        }

        if (!DistributionRequestType.PULL.equals(distributionRequest.getRequestType())) {
            throw new DistributionException("request type not supported " + distributionRequest.getRequestType());
        }

        DistributionPackage distributionPackage = null;

        try {
            log.debug("getting packages from queue {}", queueName);

            DistributionQueue queue = getQueueOrThrow(queueName);
            DistributionQueueEntry entry = queue.getHead();
            if (entry != null) {
                DistributionQueueItem queueItem = entry.getItem();
                DistributionPackageInfo info = DistributionPackageUtils.fromQueueItem(queueItem);
                DistributionPackageBuilder packageBuilder = packageBuilderProvider.getPackageBuilder(info.getType());

                if (packageBuilder != null) {
                    distributionPackage = packageBuilder.getPackage(resourceResolver, queueItem.getPackageId());
                    if (distributionPackage != null) {
                        distributionPackage.getInfo().putAll(info);

                        log.debug("item {} fetched from the queue", info);

                        packageProcessor.process(new AgentDistributionPackage(distributionPackage, queue, entry.getId()));
                    } else {
                        if (dropInvalidItems) {
                            queue.remove(entry.getId());
                            log.warn("ghost package: cannot get package {} dropping from queue", info);
                        } else {
                            log.warn("ghost package: cannot get package {} keeping in queue", info);
                        }
                    }
                } else {
                    log.warn("cannot find package builder with type {}", info.getType());
                }
            }

        } finally {
            DistributionPackageUtils.closeSafely(distributionPackage);
        }
    }

    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String distributionPackageId) {

        try {
            log.debug("getting package from queue {}", queueName);

            DistributionQueue queue = getQueueOrThrow(queueName);
            DistributionQueueEntry entry = queue.getEntry(distributionPackageId);
            DistributionPackage distributionPackage;

            if (entry != null) {
                DistributionQueueItem queueItem = entry.getItem();
                DistributionPackageInfo info = DistributionPackageUtils.fromQueueItem(queueItem);

                DistributionPackageBuilder packageBuilder = packageBuilderProvider.getPackageBuilder(info.getType());

                if (packageBuilder != null) {
                    distributionPackage = packageBuilder.getPackage(resourceResolver, queueItem.getPackageId());
                    distributionPackage.getInfo().putAll(info);

                    log.debug("item {} fetched from the queue", info);
                    if (distributionPackage != null) {
                        return new AgentDistributionPackage(distributionPackage, queue, entry.getId());
                    }
                } else {
                    log.warn("cannot find package builder with type {}", info.getType());
                }

            }

        } catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return null;
    }

    private class AgentDistributionPackage extends DistributionPackageWrapper {

        private final DistributionPackage distributionPackage;
        private final DistributionQueue queue;
        private final String itemId;

        AgentDistributionPackage(DistributionPackage distributionPackage, DistributionQueue queue, String itemId) {
            super(distributionPackage);
            this.distributionPackage = distributionPackage;
            this.queue = queue;
            this.itemId = itemId;
        }

        @Override
        public void delete() {
            queue.remove(itemId);
            DistributionPackageUtils.releaseOrDelete(distributionPackage, queue.getName());
            agentLog("exported package {} with info {} from queue {} by exporter {}", new Object[] {itemId, distributionPackage.getInfo(), queue.getName(), name});
        }

        @NotNull
        @Override
        public String getId() {
            return itemId;
        }
    }

    private void agentLog(String message, Object[] values) {
        DistributionLog agentLog = agent.getLog();

        if (agentLog instanceof DefaultDistributionLog) {
            ((DefaultDistributionLog) agentLog).info(message, values);
        }
    }

    @NotNull
    private DistributionQueue getQueueOrThrow(@NotNull String queueName)
            throws DistributionException {
        DistributionQueue queue = agent.getQueue(queueName);
        if (queue == null) {
            throw new DistributionException(String.format("Could not find queue %s", queueName));
        }
        return queue;
    }
}
