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

import java.util.Map;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.common.DistributionException;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;

/**
 * OSGi configuration factory for {@link PrivilegeDistributionRequestAuthorizationStrategy}
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Request Authorization - Privilege Request Authorization Strategy",
        description = "OSGi configuration for request based authorization strategy based on privileges",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Service(DistributionRequestAuthorizationStrategy.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Strategy name: {name}")
public class PrivilegeDistributionRequestAuthorizationStrategyFactory implements DistributionRequestAuthorizationStrategy {

    /**
     * name of this strategy.
     */
    @Property(label = "Name")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    /**
     * privilege request authorization strategy jcr privilege property
     */
    @Property(label = "Jcr Privilege", description = "Jcr privilege to check for authorizing distribution requests. The privilege is checked for the calling user session.")
    private static final String JCR_PRIVILEGE = "jcrPrivilege";

    /**
     * privilege ADD request authorization strategy jcr privilege property
     */
    @Property(cardinality = 100, label = "Additional Jcr Privileges for Add", description = "Additional Jcr privileges to check for authorizing ADD distribution requests. The privilege is checked for the calling user session.")
    private static final String JCR_ADD_PRIVILEGES = "additionalJcrPrivilegesForAdd";

    /**
     * privilege DELETE request authorization strategy jcr privilege property
     */
    @Property(cardinality = 100, label = "Additional Jcr Privileges for Delete", description = "Additional Jcr privileges to check for authorizing DELETE distribution requests. The privilege is checked for the calling user session.")
    private static final String JCR_DELETE_PRIVILEGES = "additionalJcrPrivilegesForDelete";

    private DistributionRequestAuthorizationStrategy authorizationStrategy;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {
        String jcrPrivilege = PropertiesUtil.toString(config.get(JCR_PRIVILEGE), null);
        String[] jcrAddPrivileges = PropertiesUtil.toStringArray(config.get(JCR_ADD_PRIVILEGES), null);
        String[] jcrDeletePrivileges = PropertiesUtil.toStringArray(config.get(JCR_DELETE_PRIVILEGES), null);
        authorizationStrategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, jcrAddPrivileges, jcrDeletePrivileges);
    }

    public void checkPermission(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest) throws DistributionException {
        authorizationStrategy.checkPermission(resourceResolver, distributionRequest);
    }
}
