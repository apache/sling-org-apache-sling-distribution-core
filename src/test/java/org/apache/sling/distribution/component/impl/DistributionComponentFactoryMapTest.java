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
package org.apache.sling.distribution.component.impl;

import java.util.Collections;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DistributionComponentFactoryMapTest {
    
    @Rule
    public final OsgiContext context = new OsgiContext();

    @Test
    public void testGetDefaultType() throws Exception {
        DistributionComponentFactoryMap dcfm = new DistributionComponentFactoryMap();
        context.registerInjectActivateService(dcfm);
        String type = dcfm.getType(DistributionComponentKind.AGENT,
                "org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory");
        assertEquals("simple", type);
    }

    @Test
    public void testGetDefaultFactoryPid() throws Exception {
        DistributionComponentFactoryMap dcfm = new DistributionComponentFactoryMap();
        context.registerInjectActivateService(dcfm);
        String factoryPid = dcfm.getFactoryPid(DistributionComponentKind.AGENT, "simple");
        assertEquals("org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory",
                factoryPid);
    }

    @Test
    public void testGetDefaultFactoryPids() throws Exception {
        DistributionComponentFactoryMap dcfm = new DistributionComponentFactoryMap();
        context.registerInjectActivateService(dcfm);
        assertEquals(5, dcfm.getFactoryPids(DistributionComponentKind.AGENT).size());
    }

    @Test
    public void testAddMapping() throws Exception {
        DistributionComponentFactoryMap dcfm = new DistributionComponentFactoryMap();
        String customFactoryPid = "org.test.CustomAgentFactory";
        context.registerInjectActivateService(dcfm,Collections.<String, Object>singletonMap("mapping.agent", String.format("custom:%s", customFactoryPid)));
        assertEquals(6, dcfm.getFactoryPids(DistributionComponentKind.AGENT).size());
        String type = dcfm.getType(DistributionComponentKind.AGENT, customFactoryPid);
        assertEquals("custom", type);
    }

    @Test
    public void testAddWrongMapping() throws Exception {
        DistributionComponentFactoryMap dcfm = new DistributionComponentFactoryMap();
        String customFactoryPid = "org.test.CustomAgentFactory";
        context.registerInjectActivateService(dcfm,Collections.<String, Object>singletonMap("mapping.agent", String.format("custom-%s", customFactoryPid)));
        assertEquals(5, dcfm.getFactoryPids(DistributionComponentKind.AGENT).size());
        String type = dcfm.getType(DistributionComponentKind.AGENT, customFactoryPid);
        assertNull(type);
    }
}