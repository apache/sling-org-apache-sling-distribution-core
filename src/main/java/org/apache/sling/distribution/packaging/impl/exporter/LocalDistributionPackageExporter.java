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
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.impl.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * {@link DistributionPackageExporter} implementation which creates a
 * {@link DistributionPackage} locally.
 */
public class LocalDistributionPackageExporter implements DistributionPackageExporter {

    private final DistributionPackageBuilder packageBuilder;

    public LocalDistributionPackageExporter(DistributionPackageBuilder packageBuilder) {
        this.packageBuilder = packageBuilder;
    }

    public void exportPackages(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionPackageProcessor packageProcessor) throws DistributionException {
        DistributionPackage createdPackage = packageBuilder.createPackage(resourceResolver, distributionRequest);

        try {
            if (createdPackage != null) {
                packageProcessor.process(createdPackage);
            }
        } finally {
            DistributionPackageUtils.closeSafely(createdPackage);
        }
    }

    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String distributionPackageId) throws DistributionException {
        return packageBuilder.getPackage(resourceResolver, distributionPackageId);
    }
}
