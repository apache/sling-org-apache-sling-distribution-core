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

package org.apache.sling.distribution.queue.impl.jobhandling;

import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.queue.impl.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueProviderFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

@Component(service=DistributionQueueProviderFactory.class,
    properties= {
            DistributionComponentConstants.PN_NAME + "=jobQueue"    
    })
public class JobHandlingQueueDistributionProviderFactory implements DistributionQueueProviderFactory {

    @Reference
    JobManager jobManager;

    BundleContext context;


    @Activate
    protected void activate(BundleContext context, Map<String, Object> config)
    {
        this.context = context;
    }

    @Override
    public DistributionQueueProvider getProvider(String agentName, String serviceName) {
        return new JobHandlingDistributionQueueProvider(agentName, jobManager, context);
    }

    @Override
    public void releaseProvider(DistributionQueueProvider queueProvider) {

    }
}
