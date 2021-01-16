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

import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A {@link DistributionPackageImporter} is responsible for importing {@link DistributionPackage}s into either a local
 * or remote Sling instance.
 * Importing a {@link DistributionPackage} means persisting its stream into a Sling instance.
 */
@ConsumerType
public interface DistributionPackageImporter {

    /**
     * Imports the given distribution package into the underlying system
     *
     * @param resourceResolver    - the resource resolver used to import the resources
     * @param distributionPackage - the package to be imported
     * @throws DistributionException if any error occurs during import
     */
    void importPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException;

    /**
     * Tries to convert an {@link java.io.InputStream} to a {@link DistributionPackage} and then imports it into the underlying system
     *
     * @param resourceResolver - the resource resolver used to read the package
     * @param stream           the {@link InputStream} of the package to be converted and imported
     * @return a {@link DistributionPackageInfo} if the stream has been successfully converted and imported
     * @throws DistributionException when the stream cannot be read as a {@link DistributionPackage} and imported
     */
    @NotNull
    DistributionPackageInfo importStream(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException;

}
