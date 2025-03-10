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
package org.apache.sling.distribution.serialization.impl.vlt;

import org.apache.jackrabbit.vault.fs.api.IdConflictPolicy;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionContentSerializerProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

/**
 * Provides {@link DistributionContentSerializer} based on Apache Jackrabbit FileVault
 */
@Component(service = DistributionContentSerializerProvider.class)
public class FileVaultContentSerializerProvider implements DistributionContentSerializerProvider {

    private final Packaging packaging;

    @Activate
    public FileVaultContentSerializerProvider (@Reference Packaging packaging) {
        this.packaging = packaging;
    }

    @Override
    public DistributionContentSerializer build(
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
            IdConflictPolicy idConflictPolicy) {
        ImportSettings importSettings = new ImportSettings(importMode, aclHandling, cugHandling, autosaveThreshold, strict,
                overwritePrimaryTypesOfFolders, idConflictPolicy);
        return new FileVaultContentSerializer(name, packaging, packageRoots, nodeFilters, propertyFilters, useBinaryReferences, exportPathMapping, importSettings);
    }
}
