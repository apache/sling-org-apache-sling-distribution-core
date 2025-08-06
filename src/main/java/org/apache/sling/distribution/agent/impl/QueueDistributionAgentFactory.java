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
import org.apache.sling.distribution.monitor.impl.QueueDistributionAgentMBean;
import org.apache.sling.distribution.monitor.impl.QueueDistributionAgentMBeanImpl;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporter;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.DistributionQueueProviderFactory;
import org.apache.sling.distribution.queue.impl.PriorityQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.trigger.DistributionTrigger;
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
 * An OSGi service factory for "queuing agents" that queue resources from the local instance (and can be eventually
 * pulled from another remote "reverse agent").
 *
 * @see {@link DistributionAgent}
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
             "webconsole.configurationFactory.nameHint=Agent name: {name}"   
        })
@Designate(ocd = QueueDistributionAgentFactory.Config.class,factory = true)
public class QueueDistributionAgentFactory extends AbstractDistributionAgentFactory<QueueDistributionAgentMBean> {

    @ObjectClassDefinition(name = "Apache Sling Distribution Agent - Queue Agents Factory",
            description = "OSGi configuration factory for queueing agents")
    public @interface Config {
        @AttributeDefinition(name = "Name",description = "The name of the agent.")
        String name() default "";
        
        @AttributeDefinition(name="Title", description = "The display friendly title of the agent.")
        String title() default "";
        
        @AttributeDefinition(name="Details", description = "The display friendly details of the agent.")
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

        @AttributeDefinition(cardinality=100, name="Allowed roots", description = "If set the agent will allow only distribution requests under the specified roots.")
        String[] allowed_roots();
        
        @AttributeDefinition(name="Request Authorization Strategy", description = "The target reference for the DistributionRequestAuthorizationStrategy used to authorize the access to distribution process," +
                "e.g. use target=(name=...) to bind to services by name.")
        String requestAuthorizationStrategy_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Queue Provider Factory", description = "The target reference for the DistributionQueueProviderFactory used to build queues," +
            "e.g. use target=(name=...) to bind to services by name.")
        String queueProviderFactory_target() default "(name=jobQueue)";
        
        @AttributeDefinition(name="Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String packageBuilder_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Triggers", description = "The target reference for DistributionTrigger used to trigger distribution, " +
            "e.g. use target=(name=...) to bind to services by name.")
        String triggers_target() default DEFAULT_TRIGGER_TARGET;
        
        @AttributeDefinition(cardinality = 100, name="Priority queues", description = "List of priority queues that should used for specific paths." +
            "The selector format is  {queuePrefix}[|{mainQueueMatcher}]={pathMatcher}, e.g. french=/content/fr.*" )
        String[] priorityQueues();
    }
    
    public static final String NAME = DistributionComponentConstants.PN_NAME;
    public static final String TITLE = "title";
    public static final String DETAILS = "details";
    private static final String SERVICE_NAME = "serviceName";
    public static final String LOG_LEVEL = AbstractDistributionAgentFactory.LOG_LEVEL;
    private static final String ALLOWED_ROOTS = "allowed.roots";
    public static final String TRIGGERS_TARGET = "triggers.target";
    private static final String PRIORITY_QUEUES = "priorityQueues";

    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;

    @Reference(name = "queueProviderFactory")
    private DistributionQueueProviderFactory queueProviderFactory;

    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Reference
    private Packaging packaging;

    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository slingRepository;

    DistributionQueueProvider queueProvider;

    public QueueDistributionAgentFactory() {
        super(QueueDistributionAgentMBean.class);
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

        queueProviderFactory.releaseProvider(queueProvider);
    }

    @Override
    protected SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config, DefaultDistributionLog distributionLog) {

        String serviceName = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(SERVICE_NAME), null));
        String[] allowedRoots = PropertiesUtil.toStringArray(config.get(ALLOWED_ROOTS), null);
        allowedRoots = SettingsUtils.removeEmptyEntries(allowedRoots);


        Map<String, String> priorityQueues = PropertiesUtil.toMap(config.get(PRIORITY_QUEUES), new String[0]);
        priorityQueues = SettingsUtils.removeEmptyEntries(priorityQueues);

        queueProvider = queueProviderFactory.getProvider(agentName, serviceName);

        MonitoringDistributionQueueProvider monitoringQueueProvider = new MonitoringDistributionQueueProvider(queueProvider, context);

        DistributionQueueDispatchingStrategy exportQueueStrategy = null;


        if (priorityQueues != null) {
            exportQueueStrategy = new PriorityQueueDispatchingStrategy(priorityQueues, new String[] { DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME });
        } else {
            exportQueueStrategy = new SingleQueueDispatchingStrategy();
        }

        DistributionPackageExporter packageExporter = new LocalDistributionPackageExporter(packageBuilder);
        DistributionRequestType[] allowedRequests = new DistributionRequestType[]{DistributionRequestType.ADD, DistributionRequestType.DELETE};

        return new SimpleDistributionAgent(agentName, false, null,
                serviceName, null, packageExporter, requestAuthorizationStrategy,
                monitoringQueueProvider, exportQueueStrategy, null, null,
                distributionEventFactory, resourceResolverFactory, slingRepository,
                distributionLog, allowedRequests, allowedRoots, 0);
    }

    @Override
    protected QueueDistributionAgentMBean createMBeanAgent(DistributionAgent agent, Map<String, Object> osgiConfiguration) {
        return new QueueDistributionAgentMBeanImpl(agent, osgiConfiguration);
    }

}
