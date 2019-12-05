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
package org.apache.sling.distribution.packaging.impl;

import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Resource} based {@link DistributionPackage}
 */
public class ResourceDistributionPackage extends AbstractDistributionPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private final Resource resource;
    private final ResourceResolver resourceResolver;
    private final long size;

    ResourceDistributionPackage(Resource resource,
                                String type,
                                ResourceResolver resourceResolver,
                                @Nullable String digestAlgorithm,
                                @Nullable String digestMessage,
                                @Nullable Map<String, Object> baseInfoMap) {
        super(resource.getName(), type, digestAlgorithm, digestMessage);
        this.resourceResolver = resourceResolver;
        ValueMap valueMap = resource.getValueMap();
        assert type.equals(valueMap.get("type")) : "wrong resource type";
        this.resource = resource;
        Object sizeProperty = resource.getValueMap().get("size");
        Object paths = resource.getValueMap().get(DistributionPackageInfo.PROPERTY_REQUEST_PATHS);
        Object deepPaths = resource.getValueMap().get(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS);
        this.size = sizeProperty == null ? -1 : Long.parseLong(sizeProperty.toString());
        if (null != baseInfoMap) {
            this.getInfo().putAll(baseInfoMap);
        }
        if (paths instanceof String[]) {
            this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, (String[])paths);
        }
        if (deepPaths instanceof String[]) {
            this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS, (String[])deepPaths);
        }

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
    }

    @NotNull
    @Override
    public InputStream createInputStream() throws IOException {
        try {
            return new BufferedInputStream(DistributionPackageUtils.getStream(resource));
        } catch (RepositoryException e) {
            throw new IOException("Cannot create stream", e);
        }
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public void delete() {
        delete(true);
    }

    @Override
    public void acquire(@NotNull String... holderNames) {
        try {
            DistributionPackageUtils.acquire(resource, holderNames);
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }

        } catch (RepositoryException e) {
            log.error("cannot release package", e);
        } catch (PersistenceException e) {
            log.error("cannot release package", e);
        }
    }

    @Override
    public void release(@NotNull String... holderNames) {
        try {
            DistributionPackageUtils.release(resource, holderNames);
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
        } catch (RepositoryException e) {
            log.error("cannot release package", e);
        } catch (PersistenceException e) {
            log.error("cannot release package", e);
        }
    }

    public boolean disposable() {
        try {
            return DistributionPackageUtils.disposable(resource);
        } catch (RepositoryException e) {
            log.error("cannot check if package is disposable", e);
        }
        return false;
    }

    void delete(boolean save) {
        try {
            resourceResolver.delete(resource);
            if (save) {
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }
}
