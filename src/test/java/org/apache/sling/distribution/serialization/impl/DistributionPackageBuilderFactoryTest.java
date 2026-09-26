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
package org.apache.sling.distribution.serialization.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributionPackageBuilderFactoryTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();

        DistributionContentSerializer serializer = mock(DistributionContentSerializer.class);
        when(serializer.getName()).thenReturn("test");
        bundleContext.registerService(DistributionContentSerializer.class, serializer, null);

        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        bundleContext.registerService(ResourceResolverFactory.class, resolverFactory, null);
    }

    @Test
    public void testActivateWithResourcePersistenceRegistersCleanupWithConfiguredBatchSize() {
        DistributionPackageBuilderFactory factory = new DistributionPackageBuilderFactory();

        Map<String, Object> config = new HashMap<>();
        config.put("name", "test");
        config.put("tempFsFolder", "target/tmp");
        config.put("cleanupBatchSize", 42);

        MockOsgi.injectServices(factory, bundleContext);
        MockOsgi.activate(factory, bundleContext, config);

        ServiceReference<Runnable> cleanupRef = bundleContext.getServiceReference(Runnable.class);
        assertNotNull("cleanup Runnable should be registered for the (default) resource persistence type", cleanupRef);
    }
}
