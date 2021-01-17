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
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.jcr.api.SlingRepository;
import org.jetbrains.annotations.NotNull;
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
                "webconsole.configurationFactory.nameHint=Trigger name: {name} on path {path}"
        })
@Designate(ocd=JcrEventDistributionTriggerFactory.Config.class, factory = true)
public class JcrEventDistributionTriggerFactory implements DistributionTrigger {

    @ObjectClassDefinition(name="Apache Sling Distribution Trigger - Jcr Event Triggers Factory",
            description = "Triggers a distribution request ('ADD', 'DELETE') " +
                    "for the given path (path) whenever the JCR node at the given path is modified (added, resp. removed).")
    public @interface Config {
        @AttributeDefinition(name="Name", description = "The name of the trigger.")
        String name();
        @AttributeDefinition(name="Name", description = "The path for which changes are distributed.")
        String path();
        @AttributeDefinition(cardinality = 100, 
                name = "Ignored Paths Patterns", 
                description = "The paths matching one of these patterns will be ignored.")
        String[] ignoredPathsPatterns();
        @AttributeDefinition(name="Service Name", description = "The service used to listen for jcr events")
        String serviceName();
        @AttributeDefinition(name="Use deep distribution", description = "Distribute entire subtree of the event node path. Default is 'false'.")
        boolean deep() default false;
    }


    private JcrEventDistributionTrigger trigger;

    @Reference
    private SlingRepository repository;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;


    @Activate
    public void activate(Config conf) {
        String path = conf.path();
        String serviceName = SettingsUtils.removeEmptyEntry(conf.serviceName());
        String[] ignoredPathsPatterns = conf.ignoredPathsPatterns();
        ignoredPathsPatterns = SettingsUtils.removeEmptyEntries(ignoredPathsPatterns);
        boolean deep = conf.deep();

        trigger = new JcrEventDistributionTrigger(repository, scheduler, resolverFactory, path, deep, serviceName, ignoredPathsPatterns);
        trigger.enable();
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
