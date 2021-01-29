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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageBuilderProvider;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageProcessor;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration factory for {@link AgentDistributionPackageExporter}s
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageExporter.class,
        properties = {
                "webconsole.configurationFactory.nameHint=Exporter name: {name}"
        })
@Designate(ocd=AgentDistributionPackageExporterFactory.Config.class, factory = true)
public class AgentDistributionPackageExporterFactory implements DistributionPackageExporter {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Exporter - Agent Based Package Exporter")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the exporter.")
        String name();
        @AttributeDefinition(name="Queue", description = "The name of the queue from which the packages should be exported.")
        String queue() default DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME;
        
        @AttributeDefinition(name="Drop invalid queue items", description = "Remove invalid items from the queue.")
        boolean drop_invalid_items() default false;
        
        @AttributeDefinition(name = "The target reference for the DistributionAgent that will be used to export packages.")
        String agent_target();
    }

    @Reference(name = "agent")
    private DistributionAgent agent;


    @Reference
    private DistributionPackageBuilderProvider packageBuilderProvider;

    private DistributionPackageExporter packageExporter;


    @Activate
    public void activate(Config conf) {

        String queueName = conf.queue();
        queueName = SettingsUtils.removeEmptyEntry(queueName);
        queueName = queueName == null ? DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME : queueName;

        String name = conf.name();
        boolean dropInvalidItems = conf.drop_invalid_items();


        packageExporter = new AgentDistributionPackageExporter(queueName, agent, packageBuilderProvider, name, dropInvalidItems);
    }

    public void exportPackages(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionPackageProcessor packageProcessor) throws DistributionException {
        packageExporter.exportPackages(resourceResolver, distributionRequest, packageProcessor);
    }

    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String distributionPackageId) throws DistributionException {
        return packageExporter.getPackage(resourceResolver, distributionPackageId);
    }

}
