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


import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ResourceQueueUtils {

    // prefix for queue entry ids
    private static final String ID_START = "distrq-";

    // resource folder for queue roots
    private static final String RESOURCE_ROOT = "sling:Folder";

    // resource type for internal ordered folders
    public static final String RESOURCE_FOLDER = "sling:OrderedFolder";

    // resource type for internal entries
    private static final String RESOURCE_ITEM = "nt:unstructured";

    private static final String DISTRIBUTION_PACKAGE_PREFIX = "distribution.";
    private static final String DISTRIBUTION_PACKAGE_ID = DISTRIBUTION_PACKAGE_PREFIX + "item.id";
    private static final String DISTRIBUTION_PACKAGE_SIZE = DISTRIBUTION_PACKAGE_PREFIX + "package.size";
    private static final String ENTERED_DATE = "entered.date";
    private static final String PROCESSING_ATTEMPTS = "processing.attempts";


    private static final AtomicLong itemCounter = new AtomicLong(0);
    private static final Logger log = LoggerFactory.getLogger(ResourceQueueUtils.class);


    private static Map<String, Object> serializeItem(DistributionQueueItem queueItem) {

        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : queueItem.keySet()) {
            Object value = queueItem.get(key);

            if (DistributionPackageInfo.PROPERTY_REQUEST_TYPE.equals(key)) {
                if (value instanceof DistributionRequestType) {
                    value = ((DistributionRequestType) value).name();
                }
            }

            if (value != null) {
                properties.put(DISTRIBUTION_PACKAGE_PREFIX + key, value);
            }
        }

        properties.put(DISTRIBUTION_PACKAGE_ID, queueItem.getPackageId());
        properties.put(DISTRIBUTION_PACKAGE_SIZE, queueItem.getSize());

        return properties;
    }

    private static DistributionQueueItem deserializeItem(ValueMap valueMap) {

        String packageId = valueMap.get(DISTRIBUTION_PACKAGE_ID, String.class);
        Long sizeProperty = valueMap.get(DISTRIBUTION_PACKAGE_SIZE, Long.class);
        long size = sizeProperty == null ? -1 : sizeProperty;

        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : valueMap.keySet()) {
            if (key.startsWith(DISTRIBUTION_PACKAGE_PREFIX)) {
                String infoKey = key.substring(DISTRIBUTION_PACKAGE_PREFIX.length());
                Object value = valueMap.get(key);

                if (DistributionPackageInfo.PROPERTY_REQUEST_TYPE.equals(infoKey)) {
                    if (value instanceof String) {
                        value = DistributionRequestType.valueOf((String) value);
                    }
                }

                properties.put(infoKey, value);
            }
        }

        DistributionQueueItem queueItem = new DistributionQueueItem(packageId, size, properties);
        return queueItem;
    }

    static DistributionQueueEntry readEntry(Resource queueRoot, Resource resource) {

        if (resource == null) {
            return null;
        }

        if (!resource.getPath().startsWith(queueRoot.getPath() + "/")) {
            return null;
        }

        if (!resource.isResourceType(RESOURCE_ITEM)) {
            return null;
        }

        String queueName = queueRoot.getName();
        ValueMap valueMap = resource.getValueMap();
        DistributionQueueItem queueItem = deserializeItem(valueMap);
        Calendar entered = valueMap.get(ENTERED_DATE, Calendar.getInstance());
        int attempts = valueMap.get(PROCESSING_ATTEMPTS, 0);
        DistributionQueueItemStatus queueItemStatus = new DistributionQueueItemStatus(entered,
                DistributionQueueItemState.QUEUED, attempts, queueName);

        String entryId = getIdFromPath(queueRoot.getPath(), resource.getPath());

        return new DistributionQueueEntry(entryId, queueItem, queueItemStatus);
    }

    static List<DistributionQueueEntry> getEntries(Resource queueRoot, int skip, int limit) {
        Iterator<Resource> it = new ResourceIterator(queueRoot, RESOURCE_FOLDER, false, true);

        List<DistributionQueueEntry> entries = new ArrayList<DistributionQueueEntry>();

        int i = 0;
        while (it.hasNext()) {
            Resource resource = it.next();

            if (i++ < skip) {
                continue;
            }

            DistributionQueueEntry entry = readEntry(queueRoot, resource);
            entries.add(entry);

            if (entries.size() >= limit) {
                break;
            }
        }

        return entries;

    }


    static DistributionQueueEntry getHead(Resource root) {
        Iterator<DistributionQueueEntry> it =  getEntries(root, 0, 1).iterator();

        if (it.hasNext()) {
            return it.next();
        }

        return null;
    }

    public static Resource getRootResource(ResourceResolver resourceResolver, String rootPath) throws PersistenceException {
        Resource resource =  ResourceUtil.getOrCreateResource(resourceResolver, rootPath, RESOURCE_FOLDER, RESOURCE_ROOT, true);

        return resource;
    }

    public static Resource getResourceById(Resource root, String entryId)  {
        String entryPath = getPathFromId(root.getPath(), entryId);
        return root.getResourceResolver().getResource(entryPath);
    }



    public static Resource createResource(Resource root, DistributionQueueItem queueItem) throws PersistenceException {

        Resource minuteResource = getOrCreateMinuteResource(root);

        String entryPath = getUniqueEntryPath(minuteResource);

        ResourceResolver resourceResolver = root.getResourceResolver();

        Map<String, Object> properties = serializeItem(queueItem);

        properties.put("sling:resourceType", RESOURCE_ITEM);
        properties.put(ENTERED_DATE, Calendar.getInstance());
        Resource resourceItem =  ResourceUtil.getOrCreateResource(resourceResolver, entryPath, properties,
                RESOURCE_FOLDER, true);

        resourceResolver.commit();

        return resourceItem;
    }


    /**
     * Creates a minute resource by retrying several times. If it fails even the last time it will throw an exception.
     */
    private static Resource getOrCreateMinuteResource(Resource root) throws PersistenceException {

        final int retries = 2;
        for (int i=0; i < retries; i++) {
            try {
                return tryGetOrCreateMinutes(root);
            } catch (PersistenceException e) {
                log.warn("creating minute resource failed. retrying {} more times.", retries-i);
            }

            root.getResourceResolver().revert();
            root.getResourceResolver().refresh();
        }

        return tryGetOrCreateMinutes(root);
    }

    /**
     * Creates a set of resources for consecutive minutes.
     * This ensures that consecutive minutes are created by a single thread, and that are created in order.
     * This might fail due to concurrency issues and needs to be retried a couple of times.
     */
    private static Resource tryGetOrCreateMinutes(Resource root) throws PersistenceException {

        ResourceResolver resourceResolver = root.getResourceResolver();
        Calendar now = Calendar.getInstance();

        String firstMinutePath = getTimePath(now);
        Resource firstMinuteResource = resourceResolver.getResource(root, firstMinutePath);

        if (firstMinuteResource != null) {
            return firstMinuteResource;
        }

        for (int i=0; i < 3; i++) {
            String newMinutePath = getTimePath(now);
            Resource resource = createResource(root, newMinutePath);
            log.debug("minute resource created {}", resource.getPath());
            now.add(Calendar.MINUTE, 1);
        }

        resourceResolver.commit();

        firstMinuteResource = resourceResolver.getResource(root, firstMinutePath);

        return firstMinuteResource;
    }

    /*
     * Creates a new resource at the specified path
     * This is different than ResourceUtil.getOrCreateResource as it only creates the resource, it does not retrieve it.
     * This ensures that consecutive minutes are always created atomically.
     */
    private static Resource createResource(Resource root, String relPath) throws PersistenceException {
        ResourceResolver resourceResolver = root.getResourceResolver();

        String path = root.getPath() + "/" + relPath;
        final String parentPath = ResourceUtil.getParent(path);
        final String name = ResourceUtil.getName(path);

        Resource parent =  ResourceUtil.getOrCreateResource(resourceResolver, parentPath, RESOURCE_FOLDER,
                RESOURCE_FOLDER, false);

        Map<String, Object> props = Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) RESOURCE_FOLDER);

        return resourceResolver.create(parent, name, props);
    }

    public static void deleteResource(Resource resource) throws PersistenceException {
        ResourceResolver resolver = resource.getResourceResolver();

        String path = resource.getPath();

        try {
            resolver.delete(resource);
            resolver.commit();
        } catch (PersistenceException var10) {
            resolver.revert();
            resolver.refresh();
            resource = resolver.getResource(path);
            if (resource != null) {
                resolver.delete(resource);
                resolver.commit();
            }
        }
    }


    public static int getResourceCount(Resource root) {
        ResourceResolver resolver = root.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);

        StringBuilder buf = new StringBuilder();
        buf.append("/jcr:root");
        buf.append(root.getPath());
        buf.append("//element(*,");
        buf.append(RESOURCE_ITEM);
        buf.append(")");

        try {
            QueryManager qManager = session.getWorkspace().getQueryManager();
            Query q = qManager.createQuery(buf.toString(), "xpath");
            final QueryResult res = q.execute();

            NodeIterator it = res.getNodes();
            return  (int) it.getSize();
        } catch (RepositoryException e) {
            return -1;
        }
    }


    private static String getUniqueEntryPath(Resource parent) {
        final StringBuilder sb = new StringBuilder();
        sb.append(parent.getPath());
        sb.append('/');
        sb.append(UUID.randomUUID().toString().replace("-", ""));
        sb.append('_');
        sb.append(itemCounter.getAndIncrement());

        return sb.toString();
    }

    /**
     * Transforms current time to path 2018/01/03/23/54
     * @param now the current time
     * @return the serialized time
     */
    public static String getTimePath(Calendar now) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH/mm");

        return sdf.format(now.getTime());
    }


    /**
     * Checks if path is safe to delete at this time.
     * A path is safe to delete if the nowPath does not overlap with it.
     *
     * @param nowPath represents a full path of current time (e.g. 2018/01/03/23/54)
     * @param path the path to be checked (it can be a partial path e.g. 2018/01)
     * @return true if checked path is in the past
     */
    public static boolean isSafeToDelete(String nowPath, String path) {

        // should not happen
        if (nowPath.length() < path.length()) {
            return false;
        }

        nowPath = nowPath.substring(0, path.length());

        return nowPath.compareTo(path) > 0;
    }



    private static String getPathFromId(String roothPath, String entryId) {
        String entryPath = unescapeId(entryId);
        return roothPath + "/" + entryPath;
    }

    private static String getIdFromPath(String rootPath, String path) {

        if (path.startsWith(rootPath)) {
            String entryPath = path.substring(rootPath.length()+1);

            String entryId = escapeId(entryPath);

            return entryId;
        }
        throw new IllegalArgumentException("entry path does not start with " + rootPath);
    }


    private static String escapeId(String jobId) {
        //return id;
        if (jobId == null) {
            return null;
        }
        return ID_START + jobId.replace("/", "--");
    }

    public static String unescapeId(String itemId) {
        if (itemId == null) {
            return null;
        }
        if (!itemId.startsWith(ID_START)) {
            return null;
        }

        return itemId.replace(ID_START, "").replace("--", "/");
    }

    public static void incrementProcessingAttemptForQueueItem(Resource queueItemResource) {
        ValueMap vm = queueItemResource.adaptTo(ModifiableValueMap.class);
        int attempts = vm.get(PROCESSING_ATTEMPTS, 0);
        vm.put(PROCESSING_ATTEMPTS, attempts + 1);
    }

}
