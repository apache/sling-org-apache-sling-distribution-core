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

package org.apache.sling.distribution.resources.impl;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.osgi.framework.BundleContext;
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
 * A {@link org.apache.sling.api.resource.ResourceProviderFactory} for resources backing distribution services.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=ResourceProvider.class,
        properties= {
                "webconsole.configurationFactory.nameHint=Resource kind: {kind}",
                ResourceProvider.OWNS_ROOTS + "=true"
        })
@Designate(ocd=DistributionServiceResourceProviderFactory.Config.class, factory=true)
public class DistributionServiceResourceProviderFactory implements ResourceProvider {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Resources - Service Resource Provider Factory",
            description = "Distribution Service Resource Provider Factory")
    public @interface Config {
        @AttributeDefinition()
        String roots();
        @AttributeDefinition()
        String kind();
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private
    DistributionComponentProvider componentProvider;

    private ResourceProvider resourceProvider;

    @Activate
    public void activate(BundleContext context, Config conf) {

        log.debug("activating resource provider with config {}", conf);

        String kind = conf.kind();
        String resourceRoot = conf.roots();

        resourceProvider = new ExtendedDistributionServiceResourceProvider(kind,
                componentProvider,
                resourceRoot);

        log.debug("created resource provider {}", resourceProvider);
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        resourceProvider = null;
    }


    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        return resourceProvider.getResource(resourceResolver, path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return resourceProvider.listChildren(parent);
    }
}
