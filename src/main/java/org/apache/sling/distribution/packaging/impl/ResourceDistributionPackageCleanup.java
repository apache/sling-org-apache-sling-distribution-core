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

import java.util.Iterator;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.common.DistributionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This runnable removes unreferenced {@link ResourceDistributionPackage} packages.
 * It is meant to be run periodically on a dedicated thread pool.
 * See SLING-6503 and SLING-11026.
 * Deletions are committed in batches (see SLING-13356) rather than in a single commit for
 * the whole run, to avoid an unbounded transaction when a large number of packages have
 * accumulated.
 */
public class ResourceDistributionPackageCleanup implements Runnable {

    /**
     * The default logger
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ResourceDistributionPackageBuilder packageBuilder;

    private final ResourceResolverFactory resolverFactory;

    /**
     * Maximum number of disposable packages deleted per JCR commit during a cleanup run.
     * A value {@code <= 0} disables batching, restoring the previous behavior of a single
     * commit for the whole run.
     */
    private final int cleanupBatchSize;

    public ResourceDistributionPackageCleanup(
            @NotNull ResourceResolverFactory resolverFactory,
            @NotNull ResourceDistributionPackageBuilder packageBuilder) {
        this(resolverFactory, packageBuilder, 0);
    }

    public ResourceDistributionPackageCleanup(
            @NotNull ResourceResolverFactory resolverFactory,
            @NotNull ResourceDistributionPackageBuilder packageBuilder,
            int cleanupBatchSize) {
        this.resolverFactory = resolverFactory;
        this.packageBuilder = packageBuilder;
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public void run() {
        log.debug("Cleaning up {} packages", packageBuilder.getType());
        ResourceResolver serviceResolver = null;
        try {
            int deleted = 0, total = 0, pendingInBatch = 0;
            serviceResolver = resolverFactory.getServiceResourceResolver(null);
            for (Iterator<ResourceDistributionPackage> pkgs = packageBuilder.getPackages(serviceResolver);
                    pkgs.hasNext();
                    total++) {
                ResourceDistributionPackage pkg = pkgs.next();
                if (pkg.disposable()) {
                    log.debug("Delete package {}", pkg.getId());
                    deleted++;
                    pkg.delete(false);
                    pendingInBatch++;
                    if (cleanupBatchSize > 0 && pendingInBatch >= cleanupBatchSize) {
                        serviceResolver.commit();
                        pendingInBatch = 0;
                    }
                } else {
                    log.debug("package {} is not disposable", pkg.getId());
                }
            }
            if (serviceResolver.hasChanges()) {
                serviceResolver.commit();
            }
            log.debug("Cleaned up {}/{} {} packages", deleted, total, packageBuilder.getType());
        } catch (LoginException e) {
            log.error("Failed to get distribution service resolver: {}", e.getMessage());
        } catch (DistributionException e) {
            log.error("Failed to get the list of packages", e);
        } catch (PersistenceException e) {
            log.error("Failed to delete disposable packages", e);
        } finally {
            if (serviceResolver != null && serviceResolver.isLive()) {
                serviceResolver.close();
            }
        }
    }
}
