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
package org.apache.sling.distribution.packaging.impl.importer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.*;
import org.jetbrains.annotations.NotNull;

/**
 * Remote implementation of {@link DistributionPackageImporter}
 */
public class RemoteDistributionPackageImporter implements DistributionPackageImporter {

    private final Map<String, DistributionTransport> transportHandlers = new HashMap<String, DistributionTransport>();
    private final DistributionTransportContext distributionContext = new DistributionTransportContext();

    public RemoteDistributionPackageImporter(DefaultDistributionLog log, DistributionTransportSecretProvider distributionTransportSecretProvider,
                                             Map<String, String> endpointsMap, HttpConfiguration httpConfiguration) {
        if (distributionTransportSecretProvider == null) {
            throw new IllegalArgumentException("distributionTransportSecretProvider is required");
        }

        for (Map.Entry<String, String> entry : endpointsMap.entrySet()) {
            String endpointKey = entry.getKey();
            String endpoint = entry.getValue();
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.put(endpointKey, new SimpleHttpDistributionTransport(log, new DistributionEndpoint(endpoint),
                        null, distributionTransportSecretProvider, httpConfiguration));
            }
        }
    }

    public void importPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        DistributionPackageInfo info = distributionPackage.getInfo();
        String queueName = DistributionPackageUtils.getQueueName(info);

        DistributionTransport distributionTransport = transportHandlers.get(queueName);

        if (distributionTransport != null) {
            distributionTransport.deliverPackage(resourceResolver, distributionPackage, distributionContext);
        } else {
            for(DistributionTransport transportHandler: transportHandlers.values()) {
                transportHandler.deliverPackage(resourceResolver, distributionPackage, distributionContext);
            }
        }
    }

    @NotNull
    public DistributionPackageInfo importStream(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        throw new DistributionException("not supported");
    }

}
