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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: This Package Builder does not keep track of the package created.
 */
public class InMemoryDistributionPackageBuilder extends AbstractDistributionPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionContentSerializer serializer;

    private final NavigableMap<String, List<String>> nodeFilters;

    private final NavigableMap<String, List<String>> propertyFilters;

    public InMemoryDistributionPackageBuilder(@NotNull String type,
                                              @NotNull DistributionContentSerializer serializer,
                                              @Nullable String[] nodeFilters,
                                              @Nullable String[] propertyFilters) {
        super(type);
        this.serializer = serializer;
        this.nodeFilters = VltUtils.parseFilters(nodeFilters);
        this.propertyFilters = VltUtils.parseFilters(propertyFilters);
    }

    @Override
    protected DistributionPackage createPackageForAdd(@NotNull ResourceResolver resourceResolver,
                                                      @NotNull DistributionRequest request)
            throws DistributionException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        export(resourceResolver, request, baos);

        String packageId = "dstrpck-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();

        DistributionPackageInfo info = new DistributionPackageInfo(getType());
        DistributionPackageUtils.fillInfo(info, request);
        return new InMemoryDistributionPackage(packageId, getType(), baos.toByteArray(), info);
    }

    @Override
    protected DistributionPackage readPackageInternal(@NotNull ResourceResolver resourceResolver,
                                                      @NotNull InputStream stream)
            throws DistributionException {

        Map<String, Object> info = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(stream, info);

        final String packageId;
        Object remoteId = info.get(DistributionPackageUtils.PROPERTY_REMOTE_PACKAGE_ID);
        if (remoteId != null) {
            packageId = remoteId.toString();
            log.debug("preserving remote id {}", packageId);
        } else {
            packageId = "distrpck-read-" + System.nanoTime();
            log.debug("generating a new id {}", packageId);
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(stream, baos);
            baos.flush();

            byte[] data = baos.toByteArray();
            return new InMemoryDistributionPackage(packageId, getType(), data, info);
        } catch (IOException e) {
            throw new DistributionException(e);
        }
    }

    @Override
    protected boolean installPackageInternal(@NotNull ResourceResolver resourceResolver,
                                             @NotNull InputStream inputStream)
            throws DistributionException {
        try {
            serializer.importFromStream(resourceResolver, inputStream);
            return true;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(@NotNull ResourceResolver resourceResolver,
                                                     @NotNull String id) {
        return null;
    }

    private void export(@NotNull ResourceResolver resourceResolver,
                        @NotNull final DistributionRequest request,
                        @NotNull OutputStream outputStream)
            throws DistributionException {
        final DistributionExportFilter filter = serializer.isRequestFiltering() ? null : DistributionExportFilter.createFilter(request, nodeFilters, propertyFilters);
        DistributionExportOptions distributionExportOptions = new DistributionExportOptions(request, filter);
        serializer.exportToStream(resourceResolver, distributionExportOptions, outputStream);
    }
}
