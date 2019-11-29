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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryDistributionPackageTest {

    @SuppressWarnings("serial")
    @Test
    public void testGetInfo() throws Exception {
        int size = 1000;
        byte[] data = new byte[size];
        final String testPath = "/a/test/path";
        final String testDeepPath = "/a/test/deepPath";
        new Random().nextBytes(data);
        Map <String, Object> baseInfoMap = new HashMap<String, Object>() {{
            put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[] {testPath, testDeepPath});
            put(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS, new String[] {testDeepPath});
        }};
        DistributionPackage pkg = new InMemoryDistributionPackage("id", "type", data, baseInfoMap);
        Assert.assertEquals("type", pkg.getType());
        Assert.assertEquals("id", pkg.getId());
        Assert.assertEquals(size, pkg.getSize());
        Assert.assertTrue("DistributionRequest provided Paths and those retrieved from"
                + "DistributionPackage.getInfo() don't match",
                Arrays.equals(new String[] {testPath, testDeepPath},
                        (String[])pkg.getInfo().get(DistributionPackageInfo.PROPERTY_REQUEST_PATHS))
                );
        Assert.assertTrue("DistributionRequest deep Paths and those retrieved from"
                + "DistributionPackage.getInfo() don't match",
                Arrays.equals(new String[] {testDeepPath},
                        (String[]) pkg.getInfo().get(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS))
                );
    }

    @Test
    public void testCreateInputStream() throws Exception {
        byte[] data = new byte[1000];
        new Random().nextBytes(data);
        DistributionPackage pkg = new InMemoryDistributionPackage("id", "type", data, null);
        Assert.assertNotEquals(pkg.createInputStream(), pkg.createInputStream());
    }
}