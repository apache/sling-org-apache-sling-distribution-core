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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceDistributionPackageCleanupTest {

    private BundleContext bundleContext;
    private ResourceResolver resolver;
    private ResourceDistributionPackageBuilder builder;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
        MockSling.setAdapterManagerBundleContext(bundleContext);
        resolver = MockSling.newResourceResolver(ResourceResolverType.JCR_MOCK, bundleContext);
        builder = new ResourceDistributionPackageBuilder(
                "test",
                new TestSerializer(),
                null,
                0,
                MemoryUnit.valueOf("MEGA_BYTES"),
                false,
                null,
                new String[0],
                new String[0]);
    }

    @After
    public void tearDown() {
        if (resolver.isLive()) {
            resolver.close();
        }
        MockSling.clearAdapterManagerBundleContext();
    }

    @Test
    public void testAllDisposablePackagesAreDeletedInBatches() throws Exception {
        int total = 7;
        for (int i = 0; i < total; i++) {
            createDisposablePackage();
        }
        assertEquals(total, countPackages(resolver));

        ResourceResolver serviceResolver = Mockito.spy(resolver);
        ResourceResolverFactory resolverFactory = mockResolverFactory(serviceResolver);
        ResourceDistributionPackageCleanup cleanup =
                new ResourceDistributionPackageCleanup(resolverFactory, builder, 3);
        cleanup.run();

        assertEquals("all disposable packages should be deleted", 0, countPackages(newVerificationResolver()));
        // ceil(7 / 3) = 3 commits: after the 3rd and 6th deletions, plus a final commit for the
        // 7th (the "if hasChanges()" fallback after the loop).
        verify(serviceResolver, times(3)).commit();
    }

    @Test
    public void testNonDisposablePackagesAreLeftAlone() throws Exception {
        createDisposablePackage();
        createDisposablePackage();
        createNonDisposablePackage();
        assertEquals(3, countPackages(resolver));

        ResourceResolverFactory resolverFactory = mockResolverFactory(resolver);
        ResourceDistributionPackageCleanup cleanup =
                new ResourceDistributionPackageCleanup(resolverFactory, builder, 100);
        cleanup.run();

        assertEquals("only the non-disposable package should remain", 1, countPackages(newVerificationResolver()));
    }

    @Test
    public void testExactMultipleOfBatchSizeCommitsOnlyOncePerBatch() throws Exception {
        int total = 6;
        for (int i = 0; i < total; i++) {
            createDisposablePackage();
        }

        ResourceResolver serviceResolver = Mockito.spy(resolver);
        ResourceResolverFactory resolverFactory = mockResolverFactory(serviceResolver);
        ResourceDistributionPackageCleanup cleanup =
                new ResourceDistributionPackageCleanup(resolverFactory, builder, 3);
        cleanup.run();

        assertEquals(0, countPackages(newVerificationResolver()));
        // 6 / 3 = exactly 2 commits, no extra empty commit at the end since nothing is pending.
        verify(serviceResolver, times(2)).commit();
    }

    @Test
    public void testNonPositiveBatchSizeFallsBackToSingleCommit() throws Exception {
        int total = 5;
        for (int i = 0; i < total; i++) {
            createDisposablePackage();
        }

        ResourceResolver serviceResolver = Mockito.spy(resolver);
        ResourceResolverFactory resolverFactory = mockResolverFactory(serviceResolver);
        ResourceDistributionPackageCleanup cleanup =
                new ResourceDistributionPackageCleanup(resolverFactory, builder, 0);
        cleanup.run();

        assertEquals(0, countPackages(newVerificationResolver()));
        verify(serviceResolver, times(1)).commit();
    }

    @Test
    public void testDeprecatedTwoArgConstructorFallsBackToSingleCommit() throws Exception {
        int total = 4;
        for (int i = 0; i < total; i++) {
            createDisposablePackage();
        }

        ResourceResolver serviceResolver = Mockito.spy(resolver);
        ResourceResolverFactory resolverFactory = mockResolverFactory(serviceResolver);
        ResourceDistributionPackageCleanup cleanup = new ResourceDistributionPackageCleanup(resolverFactory, builder);
        cleanup.run();

        assertEquals(0, countPackages(newVerificationResolver()));
        verify(serviceResolver, times(1)).commit();
    }

    private ResourceResolverFactory mockResolverFactory(ResourceResolver serviceResolver) throws LoginException {
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        when(resolverFactory.getServiceResourceResolver(null)).thenReturn(serviceResolver);
        return resolverFactory;
    }

    /**
     * {@code cleanup.run()} closes the resolver it was given (per its own {@code finally} block),
     * so post-run assertions need a fresh resolver against the same underlying (JCR_MOCK) repository
     * rather than reusing the one passed to the cleanup run. {@code MockSling.newResourceResolver()}
     * can't be called a second time on the same bundle context (it tries to register a second
     * {@code ResourceResolverFactory} service) -- fetch the one already registered in {@link #setUp()}
     * instead.
     */
    private ResourceResolver newVerificationResolver() throws LoginException {
        ServiceReference<ResourceResolverFactory> ref =
                bundleContext.getServiceReference(ResourceResolverFactory.class);
        ResourceResolverFactory factory = bundleContext.getService(ref);
        return factory.getAdministrativeResourceResolver(null);
    }

    private void createDisposablePackage() throws DistributionException, IOException {
        ResourceDistributionPackage pkg = createPackage();
        pkg.acquire("holder");
        pkg.release("holder");
    }

    private void createNonDisposablePackage() throws DistributionException, IOException {
        ResourceDistributionPackage pkg = createPackage();
        pkg.acquire("holder");
        // never released -> not disposable
    }

    private ResourceDistributionPackage createPackage() throws DistributionException, IOException {
        DistributionRequest mockRequest = mock(DistributionRequest.class);
        String path = "/content/" + java.util.UUID.randomUUID();
        when(mockRequest.getPaths()).thenReturn(new String[] {path});
        when(mockRequest.isDeep(path)).thenReturn(false);
        DistributionPackage pkg = builder.createPackageForAdd(resolver, mockRequest);
        return (ResourceDistributionPackage) pkg;
    }

    private int countPackages(ResourceResolver withResolver) throws DistributionException {
        int count = 0;
        for (Iterator<ResourceDistributionPackage> it = builder.getPackages(withResolver); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

    private static class TestSerializer implements DistributionContentSerializer {

        @Override
        public void exportToStream(
                ResourceResolver resourceResolver, DistributionExportOptions exportOptions, OutputStream outputStream)
                throws DistributionException {
            try {
                outputStream.write("test".getBytes());
            } catch (IOException ex) {
                throw new DistributionException(ex);
            }
        }

        @Override
        public void importFromStream(ResourceResolver resourceResolver, InputStream inputStream)
                throws DistributionException {
            throw new DistributionException("unsupported");
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isRequestFiltering() {
            return true;
        }
    }
}
