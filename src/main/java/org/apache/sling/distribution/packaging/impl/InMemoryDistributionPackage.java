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
import java.util.Map;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.jetbrains.annotations.NotNull;

public class InMemoryDistributionPackage implements DistributionPackage {

    private final String id;

    private final String type;

    private final long size;

    private final byte[] data;

    private final DistributionPackageInfo info;

    public InMemoryDistributionPackage(String id, String type, byte[] data, Map<String, Object> baseInfoMap) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.size = data.length;
        this.info = new DistributionPackageInfo(type);

        if (null != baseInfoMap) {
            this.info.putAll(baseInfoMap);
        }
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getType() {
        return type;
    }

    @NotNull
    @Override
    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void close() {
    }

    @Override
    public void delete() {
        // nothing to do
    }

    @NotNull
    @Override
    public DistributionPackageInfo getInfo() {
        return info;
    }
}
