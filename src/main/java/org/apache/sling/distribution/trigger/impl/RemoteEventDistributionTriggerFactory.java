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
package org.apache.sling.distribution.trigger.impl;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionTrigger.class,
        property= {
                "webconsole.configurationFactory.nameHint=Trigger name: {name}"
        }
)
@Designate(ocd=RemoteEventDistributionTriggerFactory.Config.class)
public class RemoteEventDistributionTriggerFactory implements DistributionTrigger {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Trigger - Remote Event Triggers Factory")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the trigger.")
        String name();
        @AttributeDefinition(name="Endpoint", description = "The endpoint from which the remote requests should be polled.")
        String endpoint();
        @AttributeDefinition()
        String transportSecretProvider_target();
    }

    @Reference(name = "transportSecretProvider")
    private
    DistributionTransportSecretProvider transportSecretProvider;


    @Reference
    private Scheduler scheduler;

    private RemoteEventDistributionTrigger trigger;


    @Activate
    public void activate(BundleContext bundleContext, Config conf) {
        String endpoint = conf.endpoint();
        trigger = new RemoteEventDistributionTrigger(endpoint, transportSecretProvider, scheduler);
    }

    @Deactivate
    public void deactivate() {
        trigger.disable();

    }

    public void register(@NotNull DistributionRequestHandler requestHandler) throws DistributionException {
        trigger.register(requestHandler);
    }

    public void unregister(@NotNull DistributionRequestHandler requestHandler) throws DistributionException {
        trigger.unregister(requestHandler);
    }
}
