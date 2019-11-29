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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ResourceDistributionPackageBuilderTest {
    BundleContext bundleContext = null;
    ResourceResolver resolver = null;

    @Test
    public void testResourceDistributionBuilder() throws DistributionException, IOException {
        final String testPath = "/a/test/path";
        final String testDeepPath = "/a/deep/test/path";
        final String[] requestPaths = {testPath, testDeepPath};

        DistributionRequest mockRequest = mock(DistributionRequest.class);
        when(mockRequest.getPaths()).thenReturn(requestPaths);
        when(mockRequest.isDeep(testDeepPath)).thenReturn(true);
        when(mockRequest.isDeep(testPath)).thenReturn(false);

        ResourceDistributionPackageBuilder builder = new ResourceDistributionPackageBuilder("test",
                new TestSerializer(), null, 0, MemoryUnit.valueOf("MEGA_BYTES"), false, null,
                new String[0],new String[0]);

        DistributionPackage createdPackage = builder.createPackageForAdd(resolver, mockRequest);

        InputStream createdPackageContentIS = createdPackage.createInputStream();
        assertNotNull("Couldn't create stream from DistributionPackage", createdPackageContentIS);
        // create a new package from the stream of created-package
        assertNotNull("Couldn't read stream from DistributionPackage",
                builder.readPackage(resolver, createdPackageContentIS));

        try {
            DistributionPackage gotPackage = builder.getPackageInternal(resolver, createdPackage.getId());
            final String[] createdPackagePaths = createdPackage.getInfo().getPaths();
            final String[] gotPackagePaths = gotPackage.getInfo().getPaths();
            final String[] createdPackageDeepPaths = (String[]) createdPackage.getInfo()
                    .get(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS);
            final String[] gotPackageDeepPaths = (String[]) gotPackage.getInfo()
                    .get(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS);
            assertTrue("packaged Paths at createPackage and getPackage not consistent. "
                    + "expected " + Arrays.toString(createdPackagePaths)
                    + ", found " + Arrays.toString(gotPackagePaths),
                    Arrays.equals(createdPackagePaths, gotPackagePaths));
            assertTrue("packaged deep Paths at createPackage and getPackage not consistent. "
                    + "expected " + Arrays.toString(createdPackageDeepPaths)
                    + ", found " + Arrays.toString(gotPackageDeepPaths),
                    Arrays.equals(createdPackageDeepPaths, gotPackageDeepPaths));
        } finally {
            createdPackage.delete();
        }
    }

    class TestSerializer implements DistributionContentSerializer {

        @Override public void exportToStream(ResourceResolver resourceResolver, DistributionExportOptions exportOptions,
                OutputStream outputStream) throws DistributionException {
            try {
                outputStream.write("test".getBytes());
            } catch (IOException ex) {
                throw new DistributionException(ex);
            }
        }

        @Override public void importFromStream(ResourceResolver resourceResolver, InputStream inputStream) throws DistributionException {
            throw new DistributionException("unsupported");
        }

        @Override public String getName() {
            return "test";
        }

        @Override public boolean isRequestFiltering() {
            return true;
        }
    }

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
        MockSling.setAdapterManagerBundleContext(bundleContext);
        resolver = MockSling.newResourceResolver(ResourceResolverType.JCR_MOCK, bundleContext);
    }

    @After
    public void tearDown() {
        resolver.close();
        MockSling.clearAdapterManagerBundleContext();
    }
}
