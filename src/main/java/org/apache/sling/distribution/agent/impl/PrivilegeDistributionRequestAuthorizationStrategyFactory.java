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
package org.apache.sling.distribution.agent.impl;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link PrivilegeDistributionRequestAuthorizationStrategy}
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionRequestAuthorizationStrategy.class,
        immediate = true,
        property = {
                "webconsole.configurationFactory.nameHint=Strategy name: {name}"
        }
)
@Designate(ocd = PrivilegeDistributionRequestAuthorizationStrategyFactory.Config.class, factory = true)
public class PrivilegeDistributionRequestAuthorizationStrategyFactory implements DistributionRequestAuthorizationStrategy {
    
    @ObjectClassDefinition(name = "Apache Sling Distribution Request Authorization - Privilege Request Authorization Strategy",
            description = "OSGi configuration for request based authorization strategy based on privileges")
    public @interface Config {
        
        @AttributeDefinition(name="name")
        String name() default "";
        
        @AttributeDefinition(name="Jcr Privilege", description = "Jcr privilege to check for authorizing distribution requests. The privilege is checked for the calling user session.")
        String jcrPrivilege() default "";
        
        @AttributeDefinition(cardinality = 100, name="Additional Jcr Privileges for Add",description = "Additional Jcr privileges to check for authorizing ADD distribution requests. " +
            "The privilege is checked for the calling user session.")
        String[] additionalJcrPrivilegesForAdd() default "jcr:read";
        
        @AttributeDefinition(cardinality = 100, name="Additional Jcr Privileges for Delete", description = "Additional Jcr privileges to check for authorizing ADD distribution requests. " +
            "The privilege is checked for the calling user session.")
        String[] additionalJcrPrivilegesForDelete() default "jcr:removeNode";
    }

    private DistributionRequestAuthorizationStrategy authorizationStrategy;

    @Activate
    public void activate(BundleContext context, Config conf) {
        String jcrPrivilege = conf.jcrPrivilege();
        String[] jcrAddPrivileges = conf.additionalJcrPrivilegesForAdd();
        String[] jcrDeletePrivileges = conf.additionalJcrPrivilegesForDelete();
        authorizationStrategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, jcrAddPrivileges, jcrDeletePrivileges);
    }

    public void checkPermission(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest) throws DistributionException {
        authorizationStrategy.checkPermission(resourceResolver, distributionRequest);
    }
}
