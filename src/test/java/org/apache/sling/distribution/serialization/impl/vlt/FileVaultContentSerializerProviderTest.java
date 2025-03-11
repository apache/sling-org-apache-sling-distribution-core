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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.jackrabbit.vault.fs.api.IdConflictPolicy;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionContentSerializerProvider;
import org.apache.sling.distribution.serialization.ExportSettings;
import org.apache.sling.distribution.serialization.ImportSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for {@link FileVaultContentSerializerProvider}
 */
public class FileVaultContentSerializerProviderTest {

    @Mock
    private Packaging packaging;

    private DistributionContentSerializerProvider provider;

    @Before
    public void before() {
        provider = new FileVaultContentSerializerProvider(packaging);
    }

    @Test
    public void testExportToStream() throws Exception {
        String name = UUID.randomUUID().toString();
        ImportMode importMode = ImportMode.UPDATE_PROPERTIES;
        AccessControlHandling aclHandling = AccessControlHandling.CLEAR;
        AccessControlHandling cugHandling = AccessControlHandling.MERGE_PRESERVE;
        String[] packageRoots = {"/"};
        String[] nodeFilters = {""};
        String[] propertyFilters = {""};
        boolean useBinaryReference = false;
        int autoSaveThreshold = 100;
        Map<String,String> exportPathMapping = new HashMap<>();
        boolean strict = false;
        boolean overwritePrimaryTypeFolders = false;
        IdConflictPolicy conflictPolicy = IdConflictPolicy.CREATE_NEW_ID;
        ExportSettings exportSettings = new ExportSettings(packageRoots, nodeFilters, propertyFilters, useBinaryReference, exportPathMapping);
        ImportSettings importSettings = new ImportSettings(importMode, aclHandling, cugHandling, autoSaveThreshold, strict, overwritePrimaryTypeFolders, conflictPolicy);
        DistributionContentSerializer serializer = provider.build(name, exportSettings, importSettings);

        assertNotNull(serializer);
        assertEquals(name, serializer.getName());
    }

}