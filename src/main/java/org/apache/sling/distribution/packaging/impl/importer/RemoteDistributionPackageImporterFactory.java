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
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.HttpConfiguration;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link RemoteDistributionPackageImporter}s.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageImporter.class,
        properties= {
               "webconsole.configurationFactory.nameHint=Importer name: {name}" 
        })
@Designate(ocd=RemoteDistributionPackageImporterFactory.Config.class, factory=true)
public class RemoteDistributionPackageImporterFactory implements DistributionPackageImporter {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Importer - Remote Package Importer Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the importer.")
        String name();
        @AttributeDefinition(cardinality = 100, name="Endpoints", description = "The list of endpoints to which the packages will be imported.")
        String[] endpoints();
        @AttributeDefinition(name="Transport Secret Provider", description = "The target reference for the DistributionTransportSecretProvider used to obtain the credentials used for accessing the remote endpoints, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String transportSecretProvider_target();
    }

    @Reference(name = "transportSecretProvider")
    private
    DistributionTransportSecretProvider transportSecretProvider;

    private DistributionPackageImporter importer;

    @Activate
    protected void activate(Config conf) {

        Map<String, String> endpoints = SettingsUtils.toUriMap(conf.endpoints());
        String importerName = conf.name();

        DefaultDistributionLog distributionLog = new DefaultDistributionLog(DistributionComponentKind.IMPORTER, importerName, RemoteDistributionPackageImporter.class, DefaultDistributionLog.LogLevel.ERROR);

        // default to 10s, we can expose it if needed
        HttpConfiguration httpConfiguration = new HttpConfiguration(10000);

        importer = new RemoteDistributionPackageImporter(distributionLog, transportSecretProvider, endpoints, httpConfiguration);

    }

    public void importPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        importer.importPackage(resourceResolver, distributionPackage);
    }

    @NotNull
    public DistributionPackageInfo importStream(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return importer.importStream(resourceResolver, stream);
    }
}
