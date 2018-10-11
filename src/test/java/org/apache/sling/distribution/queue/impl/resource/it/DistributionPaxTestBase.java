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

package org.apache.sling.distribution.queue.impl.resource.it;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategyFactory;
import org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.serialization.impl.vlt.VaultDistributionPackageBuilderFactory;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.inject.Inject;

import java.io.File;

import static org.apache.sling.testing.paxexam.SlingOptions.slingDistribution;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public class DistributionPaxTestBase extends TestSupport {

    protected static final String AGENT_RESOURCE_QUEUE = "agentResourceQueue";
    protected static final String AGENT_JOB_QUEUE = "agentJobQueue";


    @Inject
    protected ResourceResolverFactory resolverFactory;

    @Inject
    protected BundleContext context;


    @Configuration
    public Option[] configuration() {
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.distribution.core");

        return new Option[]{
                baseConfiguration(), // from TestSupport
                slingQuickstart(),
                // build artifact
                slingDistribution(),
                // testing
                testBundle(    "bundle.filename"),
                //CoreOptions.bundle(new File("/Users/mpetria/work/sling/distribution/core/target/org.apache.sling.distribution.core-0.3.5-SNAPSHOT.jar").toURI().toString()),
                defaultOsgiConfigs(),
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


    protected DistributionAgent getAgent(String agentName) {

        for (int i=0; i< 10; i++) {
            try {
                Filter filter = context.createFilter("(name="+agentName+")");

                ServiceReference[] srs = this.context.getServiceReferences(DistributionAgent.class.getName(), filter.toString());
                if (srs == null || srs.length == 0) {
                    Thread.sleep(1000);
                    continue;
                }
                ServiceReference sr = srs[0];

                Object service = context.getService(sr);

                return (DistributionAgent) service;
            } catch (InvalidSyntaxException e) {

            } catch (InterruptedException e) {
            }


        }

        return null;
    }

}
