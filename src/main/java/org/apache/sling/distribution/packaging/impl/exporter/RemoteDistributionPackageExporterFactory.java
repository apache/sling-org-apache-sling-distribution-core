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
package org.apache.sling.distribution.packaging.impl.exporter;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageProcessor;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.HttpConfiguration;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi configuration factory for {@link RemoteDistributionPackageExporter}s.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageExporter.class,
        properties= {
                "webconsole.configurationFactory.nameHint=Exporter name: {name}"
        })
@Designate(ocd=RemoteDistributionPackageExporterFactory.Config.class, factory=true)
public class RemoteDistributionPackageExporterFactory implements DistributionPackageExporter {

    @ObjectClassDefinition(name="Apache Sling Distribution Exporter - Remote Package Exporter Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the exporter.")
        String name();
        @AttributeDefinition(cardinality = 100, name="Endpoints", description = "The list of endpoints from which the packages will be exported.")
        String[] endpoints();
        @AttributeDefinition(name="Pull Items", description = "number of subsequent pull requests to make")
        int pull_items() default Integer.MAX_VALUE;
        @AttributeDefinition(name="Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String packageBuilder_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Transport Secret Provider", description = "The target reference for the DistributionTransportSecretProvider used to obtain the credentials used for accessing the remote endpoints, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String transportSecretProvider_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Reference(name = "transportSecretProvider")
    private
    DistributionTransportSecretProvider transportSecretProvider;

    private DistributionPackageExporter exporter;

    @Activate
    protected void activate(Config conf) {
        log.info("activating remote exporter with packagebuilder {} and transportSecretProvider {}", packageBuilder, transportSecretProvider);

        String[] endpoints = conf.endpoints();
        endpoints = SettingsUtils.removeEmptyEntries(endpoints);
        int pollItems = conf.pull_items();
        String exporterName = conf.name();

        DefaultDistributionLog distributionLog = new DefaultDistributionLog(DistributionComponentKind.EXPORTER, exporterName, RemoteDistributionPackageExporter.class, DefaultDistributionLog.LogLevel.ERROR);

        // default to 10s, we can expose it if needed
        HttpConfiguration httpConfiguration = new HttpConfiguration(10000);
        exporter = new RemoteDistributionPackageExporter(distributionLog, packageBuilder, transportSecretProvider,
                endpoints, pollItems, httpConfiguration);
    }


    @Deactivate
    protected void deactivate() {
        exporter = null;
    }

    public void exportPackages(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionPackageProcessor packageProcessor) throws DistributionException {
        exporter.exportPackages(resourceResolver, distributionRequest, packageProcessor);
    }

    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String distributionPackageId) throws DistributionException {
        return exporter.getPackage(resourceResolver, distributionPackageId);
    }


}
