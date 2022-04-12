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

import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.distribution.component.impl.DistributionConfigurationManager;
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
 * A {@link ResourceProviderFactory} for distribution configuration resources.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=ResourceProviderFactory.class,
        property= {
                "webconsole.configurationFactory.nameHint=Resource kind: {kind}",
                "provider.ownsRoots=true"
        })
@Designate(ocd=DistributionConfigurationResourceProviderFactory.Config.class, factory = true)
public class DistributionConfigurationResourceProviderFactory implements ResourceProviderFactory {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Resources - Configuration Resource Provider Factory",
            description="Distribution Configuration Resource Provider Factory")
    public @interface Config {
        @AttributeDefinition()
        String provider_roots();
        @AttributeDefinition()
        String kind();
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private
    DistributionConfigurationManager configurationManager;

    private String resourceRoot;
    private String kind;

    @Activate
    public void activate(BundleContext context, Config conf) {
        log.debug("activating resource provider with config {}", conf);
        resourceRoot = conf.provider_roots();
        kind = conf.kind();
    }

    @Deactivate
    public void deactivate(BundleContext context) {

    }

    public ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return new DistributionConfigurationResourceProvider(configurationManager, kind, resourceRoot);
    }

    public ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceProvider(authenticationInfo);
    }
}
