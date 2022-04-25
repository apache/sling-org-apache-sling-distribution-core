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
package org.apache.sling.distribution.servlet;

import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.APPENDABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.CLEARABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.REMOVABLE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageBuilderProvider;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to retrieve a {@link DistributionQueue} status.
 */
@SuppressWarnings("serial")
@Component(service=Servlet.class)
@SlingServletResourceTypes(
        methods = {"POST"},
        resourceTypes = {DistributionResourceTypes.AGENT_QUEUE_RESOURCE_TYPE})
public class DistributionAgentQueueServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private
    DistributionPackageBuilderProvider packageBuilderProvider;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String operation = request.getParameter("operation");

        DistributionQueue queue = request.getResource().adaptTo(DistributionQueue.class);


        ResourceResolver resourceResolver = request.getResourceResolver();


        if ("delete".equals(operation)) {
            String limitParam = request.getParameter("limit");
            String[] idParam = request.getParameterValues("id");
            if (idParam != null) {
                assertCapability(queue, REMOVABLE);
                deleteItems(resourceResolver, queue, new HashSet<String>(Arrays.asList(idParam)));
            } else {
                int limit = 1;
                try {
                    limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException ex) {
                    log.warn("limit param malformed : "+limitParam, ex);
                }
                assertCapability(queue, CLEARABLE);
                clearItems(resourceResolver, queue, limit);
            }
        } else if ("copy".equals(operation)) {
            String from = request.getParameter("from");
            String[] idParam = request.getParameterValues("id");

            if (idParam != null && from != null) {
                assertCapability(queue, APPENDABLE);
                DistributionAgent agent = request.getResource().getParent().getParent().adaptTo(DistributionAgent.class);
                DistributionQueue sourceQueue = getQueueOrThrow(agent,from);

                addItems(resourceResolver, queue, sourceQueue, idParam);

            }
        } else if ("move".equals(operation)) {
            String from = request.getParameter("from");
            String[] idParam = request.getParameterValues("id");

            if (idParam != null && from != null) {
                assertCapability(queue, APPENDABLE);
                DistributionAgent agent = request.getResource().getParent().getParent().adaptTo(DistributionAgent.class);
                DistributionQueue sourceQueue = getQueueOrThrow(agent,from);
                assertCapability(sourceQueue, REMOVABLE);
                addItems(resourceResolver, queue, sourceQueue, idParam);
                deleteItems(resourceResolver, sourceQueue, new HashSet<String>(Arrays.asList(idParam)));
            }
        }
    }

    private void addItems(ResourceResolver resourceResolver, DistributionQueue targetQueue, DistributionQueue sourceQueue, String[] ids) {
        for (String id: ids) {
            DistributionQueueEntry entry = sourceQueue.getEntry(id);
            if (entry != null) {
                targetQueue.add(entry.getItem());
                DistributionPackage distributionPackage = getPackage(resourceResolver, entry.getItem());
                DistributionPackageUtils.acquire(distributionPackage, targetQueue.getName());
            }
        }
    }

    private void deleteItems(ResourceResolver resourceResolver, DistributionQueue queue, Set<String> entryIds) {
        for (DistributionQueueEntry removed : queue.remove(entryIds)) {
            releaseOrDeletePackage(resourceResolver, removed.getItem(), queue.getName());
        }
    }

    private void clearItems(ResourceResolver resourceResolver, DistributionQueue queue, int limit) {
        for (DistributionQueueEntry removed : queue.clear(limit)) {
            releaseOrDeletePackage(resourceResolver, removed.getItem(), queue.getName());
        }
    }

    private void releaseOrDeletePackage(ResourceResolver resourceResolver, DistributionQueueItem queueItem, String queueName) {
        DistributionPackage distributionPackage = getPackage(resourceResolver, queueItem);
        DistributionPackageUtils.releaseOrDelete(distributionPackage, queueName);
    }

    private DistributionPackage getPackage(ResourceResolver resourceResolver, DistributionQueueItem item) {
        DistributionPackageInfo info = DistributionPackageUtils.fromQueueItem(item);
        String type = info.getType();

        DistributionPackageBuilder packageBuilder = packageBuilderProvider.getPackageBuilder(type);

        if (packageBuilder != null) {

            try {
                return packageBuilder.getPackage(resourceResolver, item.getPackageId());
            } catch (DistributionException e) {
                log.error("cannot get package", e);
            }
        }

        return null;
    }

    private void assertCapability(DistributionQueue queue, String capability) {
        if (!queue.hasCapability(capability)) {
            throw new UnsupportedOperationException(String.format("Capability %s not supported for queue %s", capability, queue.getName()));
        }
    }

    @NotNull
    private static DistributionQueue getQueueOrThrow(@NotNull DistributionAgent agent, @NotNull String queueName) {
        DistributionQueue queue = agent.getQueue(queueName);
        if (queue == null) {
            throw new IllegalArgumentException(String.format("Could not find queue %s", queueName));
        }
        return queue;
    }
}
