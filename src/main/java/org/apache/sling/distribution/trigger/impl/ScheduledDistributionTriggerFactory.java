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

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.SettingsUtils;
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
        properties= {
               "webconsole.configurationFactory.nameHint=Trigger name: {name}"
        })
@Designate(ocd=ScheduledDistributionTriggerFactory.Config.class,factory=true)
public class ScheduledDistributionTriggerFactory implements DistributionTrigger {

    @ObjectClassDefinition(name="Apache Sling Distribution Trigger - Scheduled Triggers Factory",
            description = "Triggers a distribution request of the given type (action) " +
                    "for the given path (path) at a periodical time interval (seconds).")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the trigger.")
        String name();
        @AttributeDefinition(name="Distribution Type", description = "The type of the distribution request produced by "
                + "this trigger in ('ADD', 'DELETE', 'PULL', 'TEST'). Default 'PULL'.")
        String action();
        @AttributeDefinition(name="Name", description = "The path to be distributed periodically.")
        String path();
        
        @AttributeDefinition(name="Interval in Seconds", 
                description = "The number of seconds between distribution requests. Default 30 seconds.")
        int seconds() default 30;
        
        @AttributeDefinition(name="Service Name", description = "The name of the service used to trigger the distribution requests.")
        String serviceName();
    }

    private ScheduledDistributionTrigger trigger;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Scheduler scheduler;


    @Activate
    public void activate(BundleContext bundleContext, Config conf) {
        // Unfortunately we cannot make DistributionRequestType.PULL.name() a default value in the above annotation.
        // see https://stackoverflow.com/questions/13253624/how-to-supply-enum-value-to-an-annotation-from-a-constant-in-java
        String action = conf.action();
        if (action == null || action.isEmpty()) {
            action = DistributionRequestType.PULL.name();
        }
        String path = conf.path();
        int interval = conf.seconds();
        String serviceName = SettingsUtils.removeEmptyEntry(conf.serviceName());

        trigger = new ScheduledDistributionTrigger(action, path, interval, serviceName, scheduler, resolverFactory);
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
