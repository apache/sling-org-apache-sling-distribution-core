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

package org.apache.sling.distribution;

import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingDistribution;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategyFactory;
import org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory;
import org.apache.sling.distribution.serialization.impl.vlt.VaultDistributionPackageBuilderFactory;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public class DistributionBaseIT extends TestSupport {

    protected static final String AGENT_RESOURCE_QUEUE = "agentResourceQueue";
    protected static final String AGENT_JOB_QUEUE = "agentJobQueue";

    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.distribution.core");
        return new Option[]{
                baseConfiguration(),
                slingQuickstart(),
                logback(),
                // build artifact
                slingDistribution(),
                // testing
                defaultOsgiConfigs(),
                SlingOptions.webconsole(),
                CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "2.0.8"),
                junitBundles()
        };
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        final int httpPort = findFreePort(); // from TestSupport
        return composite(
                slingQuickstartOakTar(workingDirectory, httpPort) // from SlingOptions
        );
    }

    public static Option defaultOsgiConfigs() {
        return composite(
                newConfiguration("org.apache.sling.jcr.resource.internal.JcrSystemUserValidator")
                        .put("allow.only.system.user", false).asOption(),

                newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                        .put("whitelist.bypass", "true").asOption(),

                // For production the users would be: replication-service,content-writer-service
                factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                        .put("user.mapping", new String[]{"org.apache.sling.distribution.core:testService=admin"})
                        .asOption(),

                factoryConfiguration(PrivilegeDistributionRequestAuthorizationStrategyFactory.class.getName())
                        .put("name", "default")
                        .put("jcrPrivilege", "jcr:read")
                        .asOption(),

                factoryConfiguration(VaultDistributionPackageBuilderFactory.class.getName())
                        .put("name", "default")
                        .put("type", "jcrvlt")
                        .asOption(),

                factoryConfiguration(QueueDistributionAgentFactory.class.getName())
                        .put("name", AGENT_RESOURCE_QUEUE)
                        .put("serviceName", "testService")
                        .put("enabled", true)
                        .put("queueProviderFactory.target", "(name=resourceQueue)")
                        .asOption(),

                factoryConfiguration(QueueDistributionAgentFactory.class.getName())
                        .put("name", AGENT_JOB_QUEUE)
                        .put("serviceName", "testService")
                        .put("enabled", true)
                        .put("queueProviderFactory.target", "(name=jobQueue)")
                        .asOption()
        );
    }

}
