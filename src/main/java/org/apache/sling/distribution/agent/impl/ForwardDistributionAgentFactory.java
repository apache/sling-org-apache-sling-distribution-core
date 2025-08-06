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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.monitor.impl.ForwardDistributionAgentMBean;
import org.apache.sling.distribution.monitor.impl.ForwardDistributionAgentMBeanImpl;
import org.apache.sling.distribution.monitor.impl.MonitoringDistributionQueueProvider;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporter;
import org.apache.sling.distribution.queue.impl.AsyncDeliveryDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.ErrorQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.MultipleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.PriorityQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.resource.ResourceQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueueProvider;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.HttpConfiguration;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;




/**
 * An OSGi service factory for {@link DistributionAgent}s which references already existing OSGi services.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property= {"webconsole.configurationFactory.nameHint=Agent name: {name}"}
)
@Designate(ocd =ForwardDistributionAgentFactoryConfig.class, factory=true)
public class ForwardDistributionAgentFactory extends AbstractDistributionAgentFactory<ForwardDistributionAgentMBean> {
    
    /**
     * Need to keep the constants around so the code below is still working.
     */
    private static final String ALLOWED_ROOTS = "allowed.roots";
    private static final String SERVICE_NAME = "serviceName";
    private static final Object QUEUE_PROCESSING_ENABLED = "queue.processing.enabled";
    private static final String PASSIVE_QUEUES = "passiveQueues";
    private static final String PRIORITY_QUEUES = "priorityQueues";
    private static final String QUEUE_PROVIDER = "queue.provider";
    private static final String ASYNC_DELIVERY = "async.delivery";
    private static final String RETRY_STRATEGY = "retry.strategy";
    private static final String RETRY_ATTEMPTS = "retry.attempts";
    private static final String IMPORTER_ENDPOINTS = "packageImporter.endpoints";
    private static final String HTTP = "http.conn.timeout";

    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;

    @Reference(name = "transportSecretProvider")
    private DistributionTransportSecretProvider transportSecretProvider;

    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Reference
    private Packaging packaging;

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

    @Reference
    private Scheduler scheduler;

    @Reference
    private ConfigurationAdmin configAdmin;

    public ForwardDistributionAgentFactory() {
        super(ForwardDistributionAgentMBean.class);
    }

    @Reference(name = "triggers", service = DistributionTrigger.class,
            policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            bind = "bindDistributionTrigger", unbind = "unbindDistributionTrigger")
    protected void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.bindDistributionTrigger(distributionTrigger, config);
    }

    protected void unbindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.unbindDistributionTrigger(distributionTrigger, config);
    }
    
    /**
     * In the first round it's not possible to utilize the signature with the above
     * config class because of the way the configuration is processed ...
     * 
     */
    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        super.activate(context, config);
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        super.deactivate(context);
    }

    @Override
    protected SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config, DefaultDistributionLog distributionLog) {
        String serviceName = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(SERVICE_NAME), null));
        String[] allowedRoots = PropertiesUtil.toStringArray(config.get(ALLOWED_ROOTS), null);
        allowedRoots = SettingsUtils.removeEmptyEntries(allowedRoots);

        boolean queueProcessingEnabled = PropertiesUtil.toBoolean(config.get(QUEUE_PROCESSING_ENABLED), true);

        String[] passiveQueues = PropertiesUtil.toStringArray(config.get(PASSIVE_QUEUES), new String[0]);
        passiveQueues = SettingsUtils.removeEmptyEntries(passiveQueues, new String[0]);

        Map<String, String> priorityQueues = PropertiesUtil.toMap(config.get(PRIORITY_QUEUES), new String[0]);
        priorityQueues = SettingsUtils.removeEmptyEntries(priorityQueues);

        Integer timeout = PropertiesUtil.toInteger(config.get(HTTP), 10) * 1000;
        HttpConfiguration httpConfiguration = new HttpConfiguration(timeout);

        DistributionPackageExporter packageExporter = new LocalDistributionPackageExporter(packageBuilder);

        DistributionQueueProvider queueProvider;
        String queueProviderName = PropertiesUtil.toString(config.get(QUEUE_PROVIDER), JobHandlingDistributionQueueProvider.TYPE);
        if (JobHandlingDistributionQueueProvider.TYPE.equals(queueProviderName)) {
            queueProvider = new JobHandlingDistributionQueueProvider(agentName, jobManager, context, configAdmin);
        } else if (SimpleDistributionQueueProvider.TYPE.equals(queueProviderName)) {
            queueProvider = new SimpleDistributionQueueProvider(scheduler, agentName, false);
        } else if (ResourceQueueProvider.TYPE.equals(queueProviderName)) {
            queueProvider = new ResourceQueueProvider(context,
                    resourceResolverFactory, SimpleDistributionAgent.DEFAULT_AGENT_SERVICE, agentName, scheduler, true);
        } else { // when SimpleDistributionQueueProvider.TYPE_CHECKPOINT is "queueProviderName"
            queueProvider = new SimpleDistributionQueueProvider(scheduler, agentName, true);
        }
        queueProvider = new MonitoringDistributionQueueProvider(queueProvider, context);

        DistributionQueueDispatchingStrategy exportQueueStrategy;
        DistributionQueueDispatchingStrategy errorQueueStrategy = null;

        DistributionPackageImporter packageImporter;
        Map<String, String> importerEndpointsMap = SettingsUtils.toUriMap(config.get(IMPORTER_ENDPOINTS));
        Set<String> processingQueues = new HashSet<String>();

        Set<String> endpointNames = importerEndpointsMap.keySet();

        Set<String> endpointsAndPassiveQueues = new TreeSet<String>();
        endpointsAndPassiveQueues.addAll(endpointNames);
        endpointsAndPassiveQueues.addAll(Arrays.asList(passiveQueues));

        // names of all the queues
        String[] queueNames = endpointsAndPassiveQueues.toArray(new String[endpointsAndPassiveQueues.size()]);

        if (priorityQueues != null) {
            PriorityQueueDispatchingStrategy dispatchingStrategy = new PriorityQueueDispatchingStrategy(priorityQueues, queueNames);
            Map<String, String> queueAliases = dispatchingStrategy.getMatchingQueues(null);
            importerEndpointsMap = SettingsUtils.expandUriMap(importerEndpointsMap, queueAliases);
            exportQueueStrategy = dispatchingStrategy;
            endpointNames = importerEndpointsMap.keySet();
        } else {
            boolean asyncDelivery = PropertiesUtil.toBoolean(config.get(ASYNC_DELIVERY), false);
            if (asyncDelivery) {
                // delivery queues' names
                Map<String, String> deliveryQueues = new HashMap<String, String>();
                for (String e : endpointNames) {
                    deliveryQueues.put(e, "delivery-" + e);
                }

                processingQueues.addAll(deliveryQueues.values());
                exportQueueStrategy = new AsyncDeliveryDispatchingStrategy(deliveryQueues);
            } else {
                if (endpointNames.size() == 1) {
                    exportQueueStrategy = new SingleQueueDispatchingStrategy(endpointNames
                            .toArray(new String[0])[0]);
                } else {
                    exportQueueStrategy = new MultipleQueueDispatchingStrategy(endpointNames
                            .toArray(new String[endpointNames.size()]));
                }
            }
        }

        processingQueues.addAll(endpointNames);
        processingQueues.removeAll(Arrays.asList(passiveQueues));

        packageImporter = new RemoteDistributionPackageImporter(distributionLog, transportSecretProvider,
                importerEndpointsMap, httpConfiguration);

        DistributionRequestType[] allowedRequests = new DistributionRequestType[]{DistributionRequestType.ADD, DistributionRequestType.DELETE};

        String retryStrategy = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(RETRY_STRATEGY), null));
        int retryAttepts = PropertiesUtil.toInteger(config.get(RETRY_ATTEMPTS), 100);

        if ("errorQueue".equals(retryStrategy)) {
            errorQueueStrategy = new ErrorQueueDispatchingStrategy(processingQueues.toArray(new String[processingQueues.size()]));
        }

        return new SimpleDistributionAgent(agentName, queueProcessingEnabled, processingQueues,
                serviceName, packageImporter, packageExporter, requestAuthorizationStrategy,
                queueProvider, exportQueueStrategy, errorQueueStrategy, distributionEventFactory, resourceResolverFactory, slingRepository,
                distributionLog, allowedRequests, allowedRoots, retryAttepts);


    }

    @Override
    protected ForwardDistributionAgentMBean createMBeanAgent(DistributionAgent agent, Map<String, Object> osgiConfiguration) {
        return new ForwardDistributionAgentMBeanImpl(agent, osgiConfiguration);
    }

}
