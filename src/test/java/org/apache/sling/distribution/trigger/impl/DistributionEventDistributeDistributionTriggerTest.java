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
package org.apache.sling.distribution.trigger.impl;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DefaultDistributionEventFactory;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testcase for {@link DistributionEventDistributeDistributionTrigger}
 */
public class DistributionEventDistributeDistributionTriggerTest {

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    @Test
    public void testRegister() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        chainDistributeDistributionTrigger.register(handler);
    }

    @Test
    public void testUnregister() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        chainDistributeDistributionTrigger.unregister(handler);
    }


    @Test
    public void testDisable() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        chainDistributeDistributionTrigger.disable();
    }

    @Test
    public void testDistributionLoop() throws Exception {

        final AtomicInteger handled = new AtomicInteger(0);
        final Map<String, Object> infoData = new HashMap<String, Object>();
        infoData.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[] { "/foo/bar" });
        infoData.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        final DistributionPackageInfo info = new DistributionPackageInfo("any", infoData);
        final DistributionEventDistributeDistributionTrigger trigger = new DistributionEventDistributeDistributionTrigger("/foo",
                osgiContext.bundleContext());
        final DistributionEventFactory eventFactory = new DefaultDistributionEventFactory();
        osgiContext.registerInjectActivateService(eventFactory);
        DistributionRequestHandler testHandler = new DistributionRequestHandler() {
            public String getName() {
                return "test";
            }

            public DistributionComponentKind getComponentKind() {
                return DistributionComponentKind.AGENT;
            }

            public void handle(ResourceResolver resourceResolver, DistributionRequest request) {
                // we simple fire an event, to cause the loop
                eventFactory.generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED,
                        DistributionComponentKind.AGENT, "test", info);
                handled.addAndGet(1);
            }
        };

        trigger.register(testHandler);

        Thread testExecution = new Thread() {
            @Override public void run() {
                eventFactory.generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED, DistributionComponentKind.AGENT,
                        "origin", info);
            }
        };

        testExecution.setDaemon(true);
        testExecution.run();

        Thread.sleep(1000);

        testExecution.interrupt();
        trigger.unregister(testHandler);

        assertEquals(1, handled.get());
    }
}