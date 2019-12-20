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
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.APPENDABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.REMOVABLE;
import static org.apache.sling.distribution.queue.DistributionQueueCapabilities.CLEARABLE;


public class ResourceQueue implements DistributionQueue {

    private static final Set<String> CAPABILITIES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(APPENDABLE, REMOVABLE, CLEARABLE)));

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected final ResourceResolverFactory resolverFactory;
    protected final String queueRootPath;
    protected String serviceName;
    protected String queueName;

    public ResourceQueue(ResourceResolverFactory resolverFactory, String serviceName, String queueName, String rootPath) {
        this.resolverFactory = resolverFactory;
        this.serviceName = serviceName;
        this.queueName = queueName;
        this.queueRootPath = rootPath + "/" + queueName;
        log.debug("starting a Resource Queue {}", queueName);
    }

    @NotNull
    @Override
    public String getName() {
        return queueName;
    }

    @Nullable
    @Override
    public DistributionQueueEntry add(@NotNull DistributionQueueItem item) {

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);

            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            Resource resource = ResourceQueueUtils.createResource(queueRoot, item);

            DistributionQueueEntry entry = ResourceQueueUtils.readEntry(queueRoot, resource);

            logEntry(entry, "add");

            return entry;

        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }


    @Override
    public DistributionQueueEntry getHead() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);


            DistributionQueueEntry head =  ResourceQueueUtils.getHead(queueRoot);

            logEntry(head, "getHead");

            return head;

        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> getEntries(int skip, int limit) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            List<DistributionQueueEntry> entries =  ResourceQueueUtils.getEntries(queueRoot, skip, limit);

            log.debug("queue[{}] getEntries entries={}", new Object[] { queueName, entries.size() });

            return entries;
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }

    @Nullable
    @Override
    public DistributionQueueEntry getEntry(@NotNull String itemId) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            Resource itemResource =  ResourceQueueUtils.getResourceById(queueRoot, itemId);

            DistributionQueueEntry entry = ResourceQueueUtils.readEntry(queueRoot, itemResource);

            logEntry(entry, "getEntry");

            return entry;

        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> remove(@NotNull Set<String> entryIds) {
        List<DistributionQueueEntry> removed = new ArrayList<DistributionQueueEntry>();
        for (String entryId : entryIds) {
            DistributionQueueEntry entry = remove(entryId);
            if (entry != null) {
                removed.add(entry);
            }
        }
        return removed;
    }

    @Nullable
    @Override
    public DistributionQueueEntry remove(@NotNull String itemId) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            Resource itemResource = ResourceQueueUtils.getResourceById(queueRoot, itemId);

            DistributionQueueEntry entry = ResourceQueueUtils.readEntry(queueRoot, itemResource);

            ResourceQueueUtils.deleteResource(itemResource);

            logEntry(entry, "remove");

            return entry;

        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }

    @NotNull
    @Override
    public DistributionQueueStatus getStatus() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource queueRoot = ResourceQueueUtils.getRootResource(resourceResolver, queueRootPath);

            int count = ResourceQueueUtils.getResourceCount(queueRoot);

            return new DistributionQueueStatus(count, DistributionQueueState.PASSIVE);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }

    @NotNull
    @Override
    public DistributionQueueType getType() {
        return DistributionQueueType.ORDERED;
    }

    void logEntry(DistributionQueueEntry entry, String scope) {
        if (entry == null) {
            log.debug("queue[{}] {} null entry", new Object[] { queueName, scope });
            return;
        }

        if (entry.getItem() == null) {
            log.debug("queue[{}] {} null item (should not happen)", new Object[] { queueName, scope });
            return;
        }

        String entryId = entry.getId();
        DistributionQueueItem item = entry.getItem();
        log.debug("queue[{}] {} entryId={} packageId={}", new Object[] { queueName, scope, entryId, item.getPackageId() });
    }

    @NotNull
    @Override
    public Iterable<DistributionQueueEntry> clear(int limit) {
        final List<DistributionQueueEntry> removedEntries = new ArrayList<DistributionQueueEntry>();
        for (DistributionQueueEntry entry : getEntries(0, limit)) {
            DistributionQueueEntry removed = remove(entry.getId());
            if (removed != null) {
                removedEntries.add(removed);
            }
        }
        return removedEntries;
    }

    @Override
    public boolean hasCapability(@NotNull String capability) {
        return CAPABILITIES.contains(capability);
    }

}
