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

package org.apache.sling.distribution.queue.impl.resource;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueProviderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionQueueProviderFactory.class,
        property= {
                DistributionComponentConstants.PN_NAME + "=resourceQueue" 
        })
@Designate(ocd=ResourceQueueProviderFactory.Config.class, factory = true)
public class ResourceQueueProviderFactory implements DistributionQueueProviderFactory {
    
    @ObjectClassDefinition(name="Apache Sling Resource Queue Provider Factory",
            description="OSGi configuration factory for Resource-backed queues")
    public @interface Config {
        @AttributeDefinition(name="Should the Resource-backed queue created with a Queue Processor (i.e., ACTIVE)")
        boolean queue_isActive() default false;
    }

    @Reference
    ResourceResolverFactory resourceResolverFactory;
    @Reference
    Scheduler scheduler;

    BundleContext context;

    private boolean isActive;

    @Activate
    protected void activate(BundleContext context, Config conf) {
        this.isActive = conf.queue_isActive();
        this.context = context;
    }

    @Override
    public DistributionQueueProvider getProvider(String agentName, String serviceName) {
        return new ResourceQueueProvider(context, resourceResolverFactory, serviceName, agentName, scheduler, isActive);
    }

    @Override
    public void releaseProvider(DistributionQueueProvider queueProvider) {
        if (queueProvider instanceof ResourceQueueProvider) {
            ((ResourceQueueProvider) queueProvider).close();
        }
    }
}
