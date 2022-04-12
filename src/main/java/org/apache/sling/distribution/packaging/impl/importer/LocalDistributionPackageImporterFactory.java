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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link LocalDistributionPackageImporter}s.
 */
@Component(service=DistributionPackageImporter.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Importer name: {name}"
        })
@Designate(ocd=LocalDistributionPackageImporterFactory.Config.class, factory = true)
public class LocalDistributionPackageImporterFactory implements DistributionPackageImporter {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Importer - Local Package Importer Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "THe name of the importer.")
        String name();
        
        @AttributeDefinition(name="Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String packageBuilder_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
    }

    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Reference
    private DistributionEventFactory eventFactory;

    private DistributionPackageImporter importer;

    @Activate
    public void activate(Config conf) {
        String name = conf.name();
        importer = new LocalDistributionPackageImporter(name, eventFactory, packageBuilder);
    }


    public void importPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        importer.importPackage(resourceResolver, distributionPackage);
    }

    @NotNull
    public DistributionPackageInfo importStream(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return importer.importStream(resourceResolver, stream);
    }

}
