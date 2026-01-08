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
import static org.mockito.Mockito.mock;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class VaultDistributionPackageBuilderFactoryTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Test
    public void testCleanupTaskHasDedicatedThreadPool() {
        context.registerService(ResourceResolverFactory.class, mock(ResourceResolverFactory.class));
        context.registerService(Packaging.class, mock(Packaging.class));

        VaultDistributionPackageBuilderFactory factory = new VaultDistributionPackageBuilderFactory();
        context.registerInjectActivateService(factory,
                "name", "test-builder",
                "type", "jcrvlt");

        ServiceReference<Runnable> ref = context.bundleContext().getServiceReference(Runnable.class);
        assertNotNull("Cleanup task should be registered", ref);

        assertEquals(DistributionComponentConstants.THREAD_POOL_NAME, ref.getProperty(Scheduler.PROPERTY_SCHEDULER_THREAD_POOL));
    }
}
