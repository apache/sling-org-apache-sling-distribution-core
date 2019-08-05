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

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class InMemoryDistributionPackageBuilderTest {

    @Test
    public void testCreatePackage() throws Exception {
        InMemoryDistributionPackageBuilder builder = new InMemoryDistributionPackageBuilder("name", new InMemDistributionContentSerializer(), new String[0], new String[0]);
        DistributionPackage pkg = builder.createPackageForAdd(mock(ResourceResolver.class), new SimpleDistributionRequest(DistributionRequestType.ADD, false, "/test"));
        assertNotNull(pkg.createInputStream());
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