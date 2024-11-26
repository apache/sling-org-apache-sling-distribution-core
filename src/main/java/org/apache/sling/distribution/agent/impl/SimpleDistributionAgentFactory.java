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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.monitor.impl.SimpleDistributionAgentMBean;
import org.apache.sling.distribution.monitor.impl.SimpleDistributionAgentMBeanImpl;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * An OSGi service factory for {@link DistributionAgent}s which references already existing OSGi services.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Agent name: {name}"
        }
)

@Designate(ocd=SimpleDistributionAgentFactory.Config.class, factory = true)
public class SimpleDistributionAgentFactory extends AbstractDistributionAgentFactory<SimpleDistributionAgentMBean> {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Agent - Simple Agents Factory",
            description = "OSGi configuration factory for agents")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "The name of the agent." )
        String name() default "";
        @AttributeDefinition(name="Title", description="The display friendly title of the agent.")
        String title() default "";
        @AttributeDefinition(name="Details",description = "The display friendly details of the agent.")
        String details() default "";
        @AttributeDefinition(name="Enabled", description = "Whether or not to start the distribution agent.")
        boolean enabled() default true;
        
        @AttributeDefinition(name="Service Name", description = "The name of the service used to access the repository. " +
                "If not set, the calling user ResourceResolver will be used" )
        String serviceName() default "";
        
        @AttributeDefinition(name="Log Level", description = "The log level recorded in the transient log accessible via http.",
                options = {
                        @Option(label="debug", value="debug"),
                        @Option(label="info", value="info"),
                        @Option(label="warn", value="warn"),
                        @Option(label="error", value="error")
        })
        String log_level() default "info";
        
        @AttributeDefinition(name="Queue Processing Enabled", description = "Whether or not the distribution agent should process packages in the queues.")
        boolean queue_processing_enabled() default true;
        
        @AttributeDefinition(name="Exporter", description="The target reference for the DistributionPackageExporter used to receive (export) the distribution packages,\" +\n"
                + "            \"e.g. use target=(name=...) to bind to services by name.")
        String packageExporter_target();
        
        @AttributeDefinition(name="Importer", description = "The target reference for the DistributionPackageImporter used to send (import) the distribution packages," +
            "e.g. use target=(name=...) to bind to services by name.")
        String packageImporter_target();
        
        @AttributeDefinition(name="Request Authorization Strategy", description = "The target reference for the DistributionRequestAuthorizationStrategy used to authorize the access to distribution process," +
                "e.g. use target=(name=...) to bind to services by name.")
        String requestAuthorizationStrategy_target();
        
        @AttributeDefinition(name="Triggers",description = "The target reference for DistributionTrigger used to trigger distribution, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String triggers_target() default DEFAULT_TRIGGER_TARGET;
    }

    public static final String NAME = DistributionComponentConstants.PN_NAME;
    public static final String TITLE = "title";
    public static final String DETAILS = "details";
    private static final String SERVICE_NAME = "serviceName";
    private static final String QUEUE_PROCESSING_ENABLED = "queue.processing.enabled";


    @Reference(name = "packageExporter")
    private DistributionPackageExporter packageExporter;

    @Reference(name = "packageImporter")
    private DistributionPackageImporter packageImporter;

    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;

    public static final String TRIGGERS_TARGET = "triggers.target";


    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository slingRepository;

    public SimpleDistributionAgentFactory() {
        super(SimpleDistributionAgentMBean.class);
    }

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        super.activate(context, config);
    }

    @Reference(name = "triggers",
            policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            bind = "bindDistributionTrigger", unbind = "unbindDistributionTrigger")
    protected void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.bindDistributionTrigger(distributionTrigger, config);

    }

    protected void unbindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.unbindDistributionTrigger(distributionTrigger, config);
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        super.deactivate(context);
    }

    @Override
    protected SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config, DefaultDistributionLog distributionLog) {
        String serviceName = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(SERVICE_NAME), null));

        boolean queueProcessingEnabled = PropertiesUtil.toBoolean(config.get(QUEUE_PROCESSING_ENABLED), true);

        DistributionQueueProvider queueProvider = new JobHandlingDistributionQueueProvider(agentName, jobManager, context);
        DistributionQueueDispatchingStrategy exportQueueStrategy = new SingleQueueDispatchingStrategy();
        Set<String> processingQueues = new HashSet<String>();
        processingQueues.addAll(exportQueueStrategy.getQueueNames());

        return new SimpleDistributionAgent(agentName, queueProcessingEnabled, processingQueues,
                serviceName, packageImporter, packageExporter, requestAuthorizationStrategy,
                queueProvider, exportQueueStrategy, null, null, distributionEventFactory, resourceResolverFactory, slingRepository,
                distributionLog, null, null, 0);

    }

    @Override
    protected SimpleDistributionAgentMBean createMBeanAgent(DistributionAgent agent, Map<String, Object> osgiConfiguration) {
        return new SimpleDistributionAgentMBeanImpl(agent, osgiConfiguration);
    }

}
