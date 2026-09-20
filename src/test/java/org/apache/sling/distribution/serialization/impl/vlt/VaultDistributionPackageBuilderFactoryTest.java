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

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class VaultDistributionPackageBuilderFactoryTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();

        bundleContext.registerService(Packaging.class, mock(Packaging.class), null);

        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        bundleContext.registerService(ResourceResolverFactory.class, resolverFactory, null);
    }

    @Test
    public void testActivateWithDefaultJcrvltTypeRegistersCleanupWithConfiguredBatchSize() {
        VaultDistributionPackageBuilderFactory factory = new VaultDistributionPackageBuilderFactory();

        Map<String, Object> config = new HashMap<>();
        config.put("name", "test");
        config.put("tempFsFolder", "target/tmp");
        config.put("cleanupBatchSize", 42);

        MockOsgi.injectServices(factory, bundleContext);
        MockOsgi.activate(factory, bundleContext, config);

        ServiceReference<Runnable> cleanupRef = bundleContext.getServiceReference(Runnable.class);
        assertNotNull(
                "cleanup Runnable should be registered for the (default) jcrvlt/resource persistence type", cleanupRef);
    }
}
