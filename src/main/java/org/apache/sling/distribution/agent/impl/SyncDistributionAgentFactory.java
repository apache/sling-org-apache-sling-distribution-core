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

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.monitor.impl.MonitoringDistributionQueueProvider;
import org.apache.sling.distribution.monitor.impl.SyncDistributionAgentMBean;
import org.apache.sling.distribution.monitor.impl.SyncDistributionAgentMBeanImpl;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporter;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.ErrorQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.MultipleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.HttpConfiguration;
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

import java.util.*;

/**
 * An OSGi service factory for "synchronizing agents" that synchronize (pull and push) resources between remote instances.
 *
 * @see {@link DistributionAgent}
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Agent name: {name}"
        }
)
@Designate(ocd=SyncDistributionAgentFactory.Config.class, factory = true)
public class SyncDistributionAgentFactory extends AbstractDistributionAgentFactory<SyncDistributionAgentMBean> {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Agent - Sync Agents Factory",
            description = "OSGi configuration factory for syncing agents")
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
        

        
        @AttributeDefinition(cardinality=100,name="passive queues", description = "List of queues that should be disabled." +
                "These queues will gather all the packages until they are removed explicitly.")
        String[] passiveQueues();
        
        @AttributeDefinition(cardinality=100, name="Exporter Endpoints", description = "List of endpoints to which packages are sent (imported). " +
                "The list can be given as a map in case a queue should be configured for each endpoint, e.g. queueName=http://...")
        String[] packageExporter_endpoints();
        
        @AttributeDefinition(cardinality=100, name="Importer Endpoints", description = "List of endpoints from which packages are received (exported)")
        String[] packageImporter_endpoints();
        
        @AttributeDefinition(name="Retry Strategy", description = "The strategy to apply after a certain number of failed retries.",
                options= {
                        @Option(label="none",value="none"),
                        @Option(label="errorQueue", value="errorQueue")
                })
        String retry_strategy() default "none";
        
        @AttributeDefinition(name="Retry attempts", description = "The number of times to retry until the retry strategy is applied.")
        int retry_attempts() default 100;
        
        @AttributeDefinition(name="Pull items", description = "Number of subsequent pull requests to make.")
        int pull_items() default 100;
        
        @AttributeDefinition(name="HTTP connection timeout", description = "The connection timeout for HTTP requests (in seconds).")
        int http_conn_timeout() default 10;
        
        @AttributeDefinition(name="Request Authorization Strategy", description = "The target reference for the DistributionRequestAuthorizationStrategy used to authorize the access to distribution process," +
                "e.g. use target=(name=...) to bind to services by name.")
        String requestAuthorizationStrategy_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Transport Secret Provider", description = "The target reference for the DistributionTransportSecretProvider used to obtain the credentials used for accessing the remote endpoints, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String transportSecretProvider_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String packageBuilder_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Triggers",description = "The target reference for DistributionTrigger used to trigger distribution, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String triggers_target() default DEFAULT_TRIGGER_TARGET;
    }

    public static final String NAME = DistributionComponentConstants.PN_NAME;
    public static final String TITLE = "title";
    public static final String DETAILS = "details";
    private static final String SERVICE_NAME = "serviceName";
    private static final String QUEUE_PROCESSING_ENABLED = "queue.processing.enabled";
    private static final String EXPORTER_ENDPOINTS = "packageExporter.endpoints";
    private static final String IMPORTER_ENDPOINTS = "packageImporter.endpoints";
    private static final String PULL_ITEMS = "pull.items";
    public static final String HTTP = "http.conn.timeout";
    public static final String TRIGGERS_TARGET = "triggers.target";
    public static final String RETRY_ATTEMPTS = "retry.attempts";
    public static final String RETRY_STRATEGY = "retry.strategy";
    public static final String PASSIVE_QUEUES = "passiveQueues";


    @Reference
    private Packaging packaging;

    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;

    @Reference(name = "transportSecretProvider")
    private DistributionTransportSecretProvider transportSecretProvider;

    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

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

    public SyncDistributionAgentFactory() {
        super(SyncDistributionAgentMBean.class);
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

        String[] passiveQueues = PropertiesUtil.toStringArray(config.get(PASSIVE_QUEUES), new String[0]);
        passiveQueues = SettingsUtils.removeEmptyEntries(passiveQueues, new String[0]);

        Object exporterEndpointsValue = config.get(EXPORTER_ENDPOINTS);
        Object importerEndpointsValue = config.get(IMPORTER_ENDPOINTS);

        String[] exporterEndpoints = PropertiesUtil.toStringArray(exporterEndpointsValue, new String[0]);
        exporterEndpoints = SettingsUtils.removeEmptyEntries(exporterEndpoints);

        Map<String, String> importerEndpointsMap = SettingsUtils.toUriMap(importerEndpointsValue);

        int pullItems = PropertiesUtil.toInteger(config.get(PULL_ITEMS), Integer.MAX_VALUE);

        DistributionQueueDispatchingStrategy exportQueueStrategy;
        DistributionQueueDispatchingStrategy importQueueStrategy = null;
        DistributionPackageImporter packageImporter;
        Set<String> processingQueues = new HashSet<String>();

        Set<String> queuesMap = new TreeSet<String>();
        queuesMap.addAll(importerEndpointsMap.keySet());
        queuesMap.addAll(Arrays.asList(passiveQueues));
        processingQueues.addAll(importerEndpointsMap.keySet());
        processingQueues.removeAll(Arrays.asList(passiveQueues));

        String[] queueNames = queuesMap.toArray(new String[queuesMap.size()]);
        exportQueueStrategy = new MultipleQueueDispatchingStrategy(queueNames);

        Integer timeout = PropertiesUtil.toInteger(config.get(HTTP), 10) * 1000;
        HttpConfiguration httpConfiguration = new HttpConfiguration(timeout);

        packageImporter = new RemoteDistributionPackageImporter(distributionLog, transportSecretProvider,
                importerEndpointsMap, httpConfiguration);

        DistributionPackageExporter packageExporter = new RemoteDistributionPackageExporter(distributionLog, packageBuilder,
                transportSecretProvider, exporterEndpoints, pullItems, httpConfiguration);
        DistributionQueueProvider exportQueueProvider = new MonitoringDistributionQueueProvider(new JobHandlingDistributionQueueProvider(agentName, jobManager, context), context);
        DistributionRequestType[] allowedRequests = new DistributionRequestType[]{DistributionRequestType.PULL};

        String retryStrategy = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(RETRY_STRATEGY), null));
        int retryAttepts = PropertiesUtil.toInteger(config.get(RETRY_ATTEMPTS), 100);

        DistributionQueueProvider importQueueProvider = null;
        if ("errorQueue".equals(retryStrategy)) {
            importQueueStrategy = new ErrorQueueDispatchingStrategy(processingQueues.toArray(new String[processingQueues.size()]));
            importQueueProvider = new MonitoringDistributionQueueProvider(new JobHandlingDistributionQueueProvider(agentName, jobManager, context), context);
        }


        return new SimpleDistributionAgent(agentName, queueProcessingEnabled, processingQueues,
                serviceName, packageImporter, packageExporter, requestAuthorizationStrategy,
                exportQueueProvider, exportQueueStrategy, importQueueStrategy, importQueueProvider,
                distributionEventFactory, resourceResolverFactory, slingRepository,
                distributionLog, allowedRequests, null, retryAttepts);

    }

    @Override
    protected SyncDistributionAgentMBean createMBeanAgent(DistributionAgent agent, Map<String, Object> osgiConfiguration) {
        return new SyncDistributionAgentMBeanImpl(agent, osgiConfiguration);
    }

}
