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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.PackageInstallHook;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryDistributionPackageBuilderTest {

    @Mock
    private PackageInstallHook installHook;
    
    private ResourceResolver resolver;
    private InMemoryDistributionPackageBuilder builder;
    

    @SuppressWarnings("deprecation")
    @Before
    public void before() throws LoginException {
        resolver = MockSling.newResourceResolver(ResourceResolverType.JCR_OAK);
        builder = new InMemoryDistributionPackageBuilder("name", new InMemDistributionContentSerializer(), new String[0], new String[0], installHook);
    }

    @Test
    public void testReadPackage() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("test1", "value1");
        DistributionPackageUtils.writeInfo(outputStream, info);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        DistributionPackage distPackage = builder.readPackage(resolver, inputStream);
        DistributionPackageInfo outInfo = distPackage.getInfo();
        assertThat(outInfo.get("test1", String.class), equalTo("value1"));
    }
    
    @Test
    public void testGetPackageInvalid() throws Exception {
        
    }
    
    @Test
    public void testGetPackageSimple() throws Exception {
        String id = "invalid";
        DistributionPackage distPackage = builder.getPackage(resolver, id);
        assertThat(distPackage, nullValue());
    }
    
    @Test
    public void testInstallPackage() throws Exception {
        SimpleDistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, false, "/test");
        DistributionPackage pkg = builder.createPackage(resolver, distributionRequest);
        assertNotNull(pkg.createInputStream());
        builder.installPackage(resolver, pkg);
        verify(installHook).onPostAdd(Mockito.eq(resolver), Mockito.eq(pkg));
    }
    
    @Test
    public void testDeletePackage() throws Exception {
        SimpleDistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.DELETE, false, "/test");
        DistributionPackage pkg = builder.createPackage(resolver, distributionRequest);
        builder.installPackage(resolver, pkg);
        verify(installHook).onPreRemove(Mockito.eq(resolver), Mockito.eq(pkg));
    }

    private final class InMemDistributionContentSerializer implements DistributionContentSerializer {

        @Override
        public void exportToStream(ResourceResolver resourceResolver,
                                   DistributionExportOptions exportOptions,
                                   OutputStream outputStream)
                throws DistributionException {
            try {
                IOUtils.write("test", outputStream, "UTF8");
            } catch (IOException e) {
                throw new DistributionException(e);
            }
        }

        @Override
        public void importFromStream(ResourceResolver resourceResolver,
                                     InputStream inputStream)
                throws DistributionException {

        }

        @Override
        public String getName() {
            return "serialiserName";
        }

        @Override
        public boolean isRequestFiltering() {
            return false;
        }
    }

}