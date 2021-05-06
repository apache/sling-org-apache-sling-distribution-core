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
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link DistributionPackage} is used for deletion of certain paths on the target instance
 */
public class SimpleDistributionPackage extends AbstractDistributionPackage implements SharedDistributionPackage {

    private static final Logger log = LoggerFactory.getLogger(SimpleDistributionPackage.class);

    private final static String PACKAGE_START = "DSTRPCK::";
    private final static String PACKAGE_START_OLD = "DSTRPCK:";
    private final static String DELIM = "|";
    private final static String PATH_DELIM = "::";
    private final static String PATH_DELIM_OLD = ",";
    private final long size;

    public SimpleDistributionPackage(DistributionRequest request, String type) {
        super(toIdString(request), type, null, null);
        String[] paths = request.getPaths();
        DistributionRequestType requestType = request.getRequestType();

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, paths);
        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, requestType);
        this.size = getId().toCharArray().length;
    }

    private static String toIdString(DistributionRequest request) {

        StringBuilder b = new StringBuilder();

        b.append(PACKAGE_START);

        b.append(request.getRequestType().toString());
        b.append(DELIM);

        String[] paths = request.getPaths();

        if (paths == null || paths.length == 0) {
            b.append(PATH_DELIM);
        } else {
            for (int i = 0; i < paths.length; i++) {
                b.append(paths[i]);
                if (i < paths.length - 1) {
                    b.append(PATH_DELIM);
                }
            }
        }

        return b.toString();
    }

    public static SimpleDistributionPackage fromIdString(String packageId, String type) {
        String id = null;
        if (packageId.startsWith(PACKAGE_START)) {
            id = packageId.substring(PACKAGE_START.length());
        } else if (packageId.startsWith(PACKAGE_START_OLD)) { //For back compatibility with old path delimiter.
            id = packageId.substring(PACKAGE_START_OLD.length());
        } else {
            return null;
        }

        String[] parts = id.split(Pattern.quote(DELIM));

        if (parts.length < 1 || parts.length > 2) {
            return null;
        }

        String actionString = parts[0];
        String pathsString = parts.length < 2 ? null : parts[1];

        DistributionRequestType distributionRequestType = DistributionRequestType.fromName(actionString);

        SimpleDistributionPackage distributionPackage = null;
        if (distributionRequestType != null) {
            String[] paths = null;
            if (pathsString != null) {
                if (packageId.startsWith(PACKAGE_START)) {
                    paths = pathsString.split(PATH_DELIM);
                } else {
                    paths = pathsString.split(PATH_DELIM_OLD); //For back compatibility with old path delimiter
                }
            } else {
                paths = new String[0];
            }

            DistributionRequest request = new SimpleDistributionRequest(distributionRequestType, paths);
            distributionPackage = new SimpleDistributionPackage(request, type);
        }

        return distributionPackage;
    }


    @NotNull
    public InputStream createInputStream() throws IOException {
        return IOUtils.toInputStream(getId(), "UTF-8");
    }

    @Override
    public long getSize() {
        return size;
    }


    public void close() {
        // there's nothing to close
    }

    public void delete() {
        // there's nothing to delete
    }

    @Override
    public String toString() {
        return getId();
    }

    public static SimpleDistributionPackage fromStream(InputStream stream, String type) {

        try {
            int size = SimpleDistributionPackage.PACKAGE_START.getBytes("UTF-8").length;
            stream.mark(size);
            byte[] buffer = new byte[size];
            int bytesRead = stream.read(buffer, 0, size);
            stream.reset();
            String s = new String(buffer, "UTF-8");

            if (bytesRead > 0 && buffer[0] > 0 && s.startsWith(SimpleDistributionPackage.PACKAGE_START)) {
                String streamString = IOUtils.toString(stream, "UTF-8");

                return fromIdString(streamString, type);
            }
        } catch (IOException e) {
            log.error("cannot read stream", e);
        }

        return null;
    }

    @Override
    public void acquire(@NotNull String... holderNames) {

    }

    @Override
    public void release(@NotNull String... holderNames) {

    }
}
