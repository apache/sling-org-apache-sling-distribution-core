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

import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.resource.ResourceQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueueProvider;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name="Apache Sling Distribution Agent - Forward Agents Factory",
        description="OSGi configuration factory for forward agents")
public @interface ForwardDistributionAgentFactoryConfig {
    @AttributeDefinition(name = "Name", description = "The name of the agent." )
    String name() default "";
    
    @AttributeDefinition(name="Title", description="The display friendly title of the agent.")
    String title() default "";
    
    @AttributeDefinition(name="Details",description = "The display friendly details of the agent.")
    String details() default "";
    
    @AttributeDefinition(name="Enabled", description = "Whether or not to start the distribution agent.")
    boolean enabled() default true;
    
    @AttributeDefinition(name="Service Name", description = "The name of the service used to access the repository. " +
        "If not set, the calling user ResourceResolver will be used")
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
    
    @AttributeDefinition(name="Queue Processing Enabled", description = "Whether or not the distribution agent should process packages in the queues.")
    boolean queue_processing_enabled() default true;
    
    // endpoints property
    @AttributeDefinition(cardinality=100, name="Importer Endpoints", description = "List of endpoints to which packages are sent (imported). " +
            "The list can be given as a map in case a queue should be configured for each endpoint, e.g. queueName=http://...")
    String[] packageImporter_endpoints();
    
    @AttributeDefinition(cardinality=100,name="passive queues", description = "List of queues that should be disabled." +
            "These queues will gather all the packages until they are removed explicitly.")
    String[] passiveQueues();
    
    @AttributeDefinition(cardinality=100, name="Priority queues", description = "List of priority queues that should used for specific paths." +
            "The selector format is  {queuePrefix}[|{mainQueueMatcher}]={pathMatcher}, e.g. french=/content/fr.*")
    String[] priorityQueues();
    
    @AttributeDefinition(name="Retry Strategy", description = "The strategy to apply after a certain number of failed retries.",
            options= {
                    @Option(label="none",value="none"),
                    @Option(label="errorQueue", value="errorQueue")
            })
    String retry_strategy() default "none";
    
    @AttributeDefinition(name="Retry attemps", description = "The number of times to retry until the retry strategy is applied.")
    int retry_attempts() default 100;
    
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
    String triggers_target() default ForwardDistributionAgentFactory.DEFAULT_TRIGGER_TARGET;
    
    @AttributeDefinition(name="Queue provider", description="he queue provider implementation.",
            options = {
                    @Option(label=JobHandlingDistributionQueueProvider.TYPE, value="jobs"),
                    @Option(label=ResourceQueueProvider.TYPE, value="Resource Backed"),
                    @Option(label=SimpleDistributionQueueProvider.TYPE, value="In-memory"),
                    @Option(label=SimpleDistributionQueueProvider.TYPE_CHECKPOINT,value="In-file")
            })
    String queue_provider() default "jobs";
    
    @AttributeDefinition(name="Async delivery", description = "Whether or not to use a separate delivery queue to maximize transport throughput when queue has more than 100 items" )
    boolean async_delivery() default false;
    
    @AttributeDefinition(name="HTTP connection timeout", description = "The connection timeout for HTTP requests (in seconds).")
    int http_conn_timeout() default 10;
}