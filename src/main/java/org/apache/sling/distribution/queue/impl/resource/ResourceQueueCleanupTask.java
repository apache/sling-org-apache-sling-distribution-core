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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Iterator;


public class ResourceQueueCleanupTask implements Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private final ResourceResolverFactory resolverFactory;
    private final String serviceName;
    private final String rootPath;

    public ResourceQueueCleanupTask(ResourceResolverFactory resolverFactory, String serviceName, String rootPath) {

        this.resolverFactory = resolverFactory;
        this.serviceName = serviceName;
        this.rootPath = rootPath;
    }

    @Override
    public void run() {
        log.debug("Cleaning up resource queues at {}", rootPath);

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = DistributionUtils.loginService(resolverFactory, serviceName);
            Resource root = ResourceQueueUtils.getRootResource(resourceResolver, rootPath);

            Iterator<Resource> it = root.listChildren();

            while (it.hasNext()) {
                Resource queueRoot = it.next();

                log.debug("Starting cleaning up queue at {}", queueRoot.getPath());

                removeEmptyFolders(queueRoot);

                log.debug("Finished cleaning up queue at {}", queueRoot.getPath());
            }
        } catch (Throwable e) {
            log.error("Error cleaning up resource queues", e);
        } finally {
            DistributionUtils.safelyLogout(resourceResolver);
        }
    }


    public void removeEmptyFolders(Resource root) throws PersistenceException {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -5);

        ResourceResolver resolver = root.getResourceResolver();
        ResourceIterator it = new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, true, false);

        String nowPath =  ResourceQueueUtils.getTimePath(now);

        while (it.hasNext()) {
            Resource res = it.next();

            String resPath = res.getPath().substring(root.getPath().length()+1);

            if (!res.isResourceType(ResourceQueueUtils.RESOURCE_FOLDER)) {
                continue;
            }

            // now 2018/03/15/03/48
            // res 2018/02/15
            if (!ResourceQueueUtils.isSafeToDelete(nowPath, resPath)) {
                continue;
            }

            if (res.hasChildren()) {
                continue;
            }

            log.debug("removing empty folder {}", res.getPath());

            resolver.delete(res);
            resolver.commit();
        }

    }
}
