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

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageProcessor;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link LocalDistributionPackageExporter}s.
 */
@Component(service=DistributionPackageExporter.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Exporter name: {name}" 
        })
@Designate(ocd=LocalDistributionPackageExporterFactory.Config.class, factory=true)
public class LocalDistributionPackageExporterFactory implements DistributionPackageExporter {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Exporter - Local Package Exporter Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the exporter.")
        String name();
        @AttributeDefinition(name="Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String packageBuilder_target();
    }
    
    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    private DistributionPackageExporter exporter;

    @Activate
    public void activate(Map<String, Object> config) {
        exporter = new LocalDistributionPackageExporter(packageBuilder);
    }

    public void exportPackages(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionPackageProcessor packageProcessor) throws DistributionException {
        exporter.exportPackages(resourceResolver, distributionRequest, packageProcessor);
    }

    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String distributionPackageId) throws DistributionException {
        return exporter.getPackage(resourceResolver, distributionPackageId);
    }
}
