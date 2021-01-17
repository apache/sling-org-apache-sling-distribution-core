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
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.jcr.api.SlingRepository;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link RepositoryDistributionPackageImporter}s.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageImporter.class,
        properties= {
                "webconsole.configurationFactory.nameHint=Importer name: {name}"     
        })
@Designate(ocd=RepositoryDistributionPackageImporterFactory.Config.class, factory = true)
public class RepositoryDistributionPackageImporterFactory implements DistributionPackageImporter {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Importer - Repository Package Importer Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the importer.")
        String name();
        @AttributeDefinition()
        String service_name() default "admin";
        @AttributeDefinition()
        String path() default "/var/sling/distribution/import";
        @AttributeDefinition()
        String privilege_name() default "jcr:read";
    }

    @Reference
    private SlingRepository repository;

    private RepositoryDistributionPackageImporter importer;

    @Activate
    protected void activate(Config conf) {

        importer = new RepositoryDistributionPackageImporter(repository,
                conf.service_name(),
                conf.path(),
                conf.privilege_name());
    }

    public void importPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        importer.importPackage(resourceResolver, distributionPackage);

    }

    @NotNull
    public DistributionPackageInfo importStream(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return importer.importStream(resourceResolver, stream);
    }
}
