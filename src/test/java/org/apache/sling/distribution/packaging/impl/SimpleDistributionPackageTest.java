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
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
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

    private static final String DSTRPCK_DELETE = "DSTRPCK:DELETE|/abc:/c";
    private static final String DSTRPCK_ITEM_WITH_COMMA_DELETE = "DSTRPCK:DELETE|/ab,c:/c";

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
    public void testCreatedAndReadPackagesEqualityWithCommaInName() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.DELETE, "/ab,c", "/c");
        SimpleDistributionPackage createdPackage = new SimpleDistributionPackage(request, "VOID");
        assertThat(createdPackage.toString(), equalTo(DSTRPCK_ITEM_WITH_COMMA_DELETE));
        // Just to run the code
        createdPackage.acquire();
        createdPackage.release();
        createdPackage.close();
        createdPackage.delete();

        SimpleDistributionPackage readPackage = SimpleDistributionPackage.fromStream(new ByteArrayInputStream(DSTRPCK_ITEM_WITH_COMMA_DELETE.getBytes()), "VOID");
        assertNotNull(readPackage);
        assertEquals(Arrays.toString(createdPackage.getInfo().getPaths()), Arrays.toString(readPackage.getInfo().getPaths()));
    }
    
    @Test
    public void testCreatedAndReadPackagesEquality() throws Exception {
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.DELETE, "/abc", "/c");
        SimpleDistributionPackage createdPackage = new SimpleDistributionPackage(request, "VOID");
        assertThat(createdPackage.toString(), equalTo(DSTRPCK_DELETE));
        // Just to run the code
        createdPackage.acquire();
        createdPackage.release();
        createdPackage.close();
        createdPackage.delete();
        
        SimpleDistributionPackage readPackage = SimpleDistributionPackage.fromStream(new ByteArrayInputStream(DSTRPCK_DELETE.getBytes()), "VOID");
        assertNotNull(readPackage);
        assertEquals(DSTRPCK_DELETE.length(), readPackage.getSize());
        assertEquals(createdPackage.getType(), readPackage.getType());
        assertEquals(createdPackage.getInfo().getRequestType(), readPackage.getInfo().getRequestType());
        assertEquals(Arrays.toString(createdPackage.getInfo().getPaths()), Arrays.toString(readPackage.getInfo().getPaths()));
        assertEquals(createdPackage.getId(), readPackage.getId());
        assertTrue(IOUtils.contentEquals(createdPackage.createInputStream(), readPackage.createInputStream()));
        
    }

    @Test
    public void testSimplePackageFromTest() throws Exception {
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.TEST);
        SimpleDistributionPackage createdPackage = new SimpleDistributionPackage(distributionRequest, "VOID");
        SimpleDistributionPackage readPackage = SimpleDistributionPackage.fromStream(new ByteArrayInputStream(("DSTRPCK:TEST|").getBytes()), "VOID");
        assertNotNull(readPackage);
        assertEquals(createdPackage.getType(), readPackage.getType());
        assertEquals(createdPackage.getInfo().getRequestType(), readPackage.getInfo().getRequestType());
        assertEquals(Arrays.toString(createdPackage.getInfo().getPaths()), Arrays.toString(readPackage.getInfo().getPaths()));
        assertEquals(createdPackage.getId(), readPackage.getId());
        assertTrue(IOUtils.contentEquals(createdPackage.createInputStream(), readPackage.createInputStream()));
    }
}
