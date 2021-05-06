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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.junit.Test;
import org.mockito.Mockito;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleDistributionPackage}
 */
public class SimpleDistributionPackageTest {

    private static final String TYPE = "testPackageType";

    @Test
    public void testInvalid() {
        String id = "invalid";
        SimpleDistributionPackage pkg = SimpleDistributionPackage.fromIdString(id, "");
        assertThat(pkg, nullValue());
    }
    
    @Test
    public void testInvalid2() {
        String id = "DSTRPCK:";
        SimpleDistributionPackage pkg = SimpleDistributionPackage.fromIdString(id, "");
        assertThat(pkg, nullValue());
    }
    
    @Test
    public void testInvalid3() {
        String id = "DSTRPCK:a|b|c";
        SimpleDistributionPackage pkg = SimpleDistributionPackage.fromIdString(id, "");
        assertThat(pkg, nullValue());
    }
    
    @Test
    public void testFromStreamError() throws IOException {
        InputStream stream = mock(InputStream.class);
        when(stream.read(Mockito.any(byte[].class), Mockito.eq(0), Mockito.anyInt())).thenThrow(new IOException("Expected"));
        SimpleDistributionPackage pkg = SimpleDistributionPackage.fromStream(stream, "ADD");
        assertThat(pkg, nullValue());
    }

    @Test
    public void testPackageWithNamespaceInPath() {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/a/jcr:content", "/b");
        testPackageSerDeser(request);
    }

    @Test
    public void testCreatedAndReadPackagesEqualityWithCommaInName() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.DELETE, "/ab,c", "/c");
        testPackageSerDeser(request);
    }
    
    @Test
    public void testCreatedAndReadPackagesEquality() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.DELETE, "/abc", "/c");
        testPackageSerDeser(request);
    }

    @Test
    public void testSimplePackageFromTest() throws Exception {
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.TEST);
        SimpleDistributionPackage createdPackage = new SimpleDistributionPackage(distributionRequest, "VOID");
        SimpleDistributionPackage readPackage = SimpleDistributionPackage.fromStream(new ByteArrayInputStream(("DSTRPCK::TEST|").getBytes()), "VOID");
        assertNotNull(readPackage);
        assertEquals(createdPackage.getType(), readPackage.getType());
        assertEquals(createdPackage.getInfo().getRequestType(), readPackage.getInfo().getRequestType());
        assertEquals(Arrays.toString(createdPackage.getInfo().getPaths()), Arrays.toString(readPackage.getInfo().getPaths()));
        assertEquals(createdPackage.getId(), readPackage.getId());
        assertTrue(IOUtils.contentEquals(createdPackage.createInputStream(), readPackage.createInputStream()));
    }

    private void testPackageSerDeser(DistributionRequest request) {
        SimpleDistributionPackage pkgOut = new SimpleDistributionPackage(request, TYPE);
        SimpleDistributionPackage pkgIn = SimpleDistributionPackage.fromStream(toInputStream(pkgOut.toString(), Charset.defaultCharset()), TYPE);
        assertNotNull(pkgIn);
        assertArrayEquals(pkgOut.getInfo().getPaths(), pkgIn.getInfo().getPaths());
        assertEquals(pkgOut.getType(), pkgIn.getType());
        assertEquals(pkgOut.getId(), pkgIn.getId());
        assertEquals(pkgOut.getInfo().getRequestType(), pkgIn.getInfo().getRequestType());
    }
}
