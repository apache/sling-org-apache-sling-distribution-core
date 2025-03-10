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
package org.apache.sling.distribution.serialization;

import org.apache.jackrabbit.vault.fs.api.IdConflictPolicy;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.Map;

/**
 * A provider for content serializer used to convert
 * distribution payloads to and from binary streams.
 */
@ConsumerType
public interface DistributionContentSerializerProvider {

    /**
     * @param name The serializer name
     * @param importMode The serializer import mode
     * @param aclHandling The serializer ACL handling mode
     * @param cugHandling The serializer CUG handling mode
     * @param packageRoots The serializer package roots
     * @param nodeFilters The serializer node path filters
     * @param propertyFilters The serializer property path filters
     * @param useBinaryReferences {@code true} to pass binaries by reference ;
     *                            {@code false} to inline binaries
     * @param autosaveThreshold The number of resources to handle before
     *                          automatically saving the changes.
     * @param exportPathMapping The mapping for exported paths
     * @param strict {@code true} to enforce import constraints;
     *               {@code false} otherwise
     * @param overwritePrimaryTypesOfFolders {@code true} to overwrite folder primary types ;
     *               {@code false} otherwise
     * @param idConflictPolicy The policy to handle conflicts
     * @return a distribution content serializer
     */
    DistributionContentSerializer build(
            String name,
            ImportMode importMode,
            AccessControlHandling aclHandling,
            AccessControlHandling cugHandling,
            String[] packageRoots,
            String[] nodeFilters,
            String[] propertyFilters,
            boolean useBinaryReferences,
            int autosaveThreshold,
            Map<String, String> exportPathMapping,
            boolean strict,
            boolean overwritePrimaryTypesOfFolders,
            IdConflictPolicy idConflictPolicy);
}
