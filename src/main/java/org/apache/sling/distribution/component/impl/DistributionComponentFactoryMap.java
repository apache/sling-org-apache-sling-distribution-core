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
package org.apache.sling.distribution.component.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.ReverseDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SyncDistributionAgentFactory;
import org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporterFactory;
import org.apache.sling.distribution.serialization.impl.vlt.VaultDistributionPackageBuilderFactory;
import org.apache.sling.distribution.transport.impl.UserCredentialsDistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.impl.DistributionEventDistributeDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.JcrEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.PersistedJcrEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.ResourceEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.ScheduledDistributionTriggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.sling.commons.osgi.PropertiesUtil.*;
import static org.apache.sling.distribution.component.impl.DistributionComponentKind.*;

@Service(value = DistributionComponentFactoryMap.class)
@Component(metatype = false)
public class DistributionComponentFactoryMap {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] MAPPING_AGENT_DEFAULT = {
            format("simple:%s", SimpleDistributionAgentFactory.class.getName()),
            format("sync:%s", SyncDistributionAgentFactory.class.getName()),
            format("forward:%s", ForwardDistributionAgentFactory.class.getName()),
            format("reverse:%s", ReverseDistributionAgentFactory.class.getName()),
            format("queue:%s", QueueDistributionAgentFactory.class.getName()) };

    private static final String[] MAPPING_IMPORTER_DEFAULT = {
            format("local:%s", LocalDistributionPackageImporterFactory.class.getName()),
            format("remote:%s", RemoteDistributionPackageImporterFactory.class.getName()) };

    private static final String[] MAPPING_EXPORTER_DEFAULT = {
            format("local:%s", LocalDistributionPackageExporterFactory.class.getName()),
            format("remote:%s", RemoteDistributionPackageExporterFactory.class.getName()),
            format("agent:%s", AgentDistributionPackageExporterFactory.class.getName()) };

    private static final String[] MAPPING_QUEUE_PROVIDER_DEFAULT = {
            format("simple:%s", SimpleDistributionAgentFactory.class.getName()),
            format("sync:%s", SyncDistributionAgentFactory.class.getName()),
            format("forward:%s", ForwardDistributionAgentFactory.class.getName()),
            format("reverse:%s", ReverseDistributionAgentFactory.class.getName()),
            format("queue:%s", QueueDistributionAgentFactory.class.getName()) };

    private static final String[] MAPPING_QUEUE_STRATEGY_DEFAULT = {
            format("simple:%s", SimpleDistributionAgentFactory.class.getName()),
            format("sync:%s", SyncDistributionAgentFactory.class.getName()),
            format("forward:%s", ForwardDistributionAgentFactory.class.getName()),
            format("reverse:%s", ReverseDistributionAgentFactory.class.getName()),
            format("queue:%s", QueueDistributionAgentFactory.class.getName()) };

    private static final String[] MAPPING_TRANSPORT_SECRET_PROVIDER_DEFAULT = {
            format("user:%s", UserCredentialsDistributionTransportSecretProvider.class.getName()) };

    private static final String[] MAPPING_PACKAGE_BUILDER_DEFAULT = {
            format("filevlt:%s", VaultDistributionPackageBuilderFactory.class.getName()),
            format("jcrvlt:%s", VaultDistributionPackageBuilderFactory.class.getName()) };

    private static final String[] MAPPING_REQUEST_AUTHORIZATION_DEFAULT = {
            format("privilege:%s", PrivilegeDistributionRequestAuthorizationStrategy.class.getName()) };

    private static final String[] MAPPING_TRIGGER_DEFAULT = {
            format("resourceEvent:%s", ResourceEventDistributionTriggerFactory.class.getName()),
            format("scheduledEvent:%s", ScheduledDistributionTriggerFactory.class.getName()),
            format("distributionEvent:%s", DistributionEventDistributeDistributionTriggerFactory.class.getName()),
            format("persistedJcrEvent:%s", PersistedJcrEventDistributionTriggerFactory.class.getName()),
            format("jcrEvent:%s", JcrEventDistributionTriggerFactory.class.getName()) };


    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_AGENT = "mapping.agent";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_IMPORTER = "mapping.importer";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_EXPORTER = "mapping.exporter";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_QUEUE_PROVIDER = "mapping.queueProvider";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_QUEUE_STRATEGY = "mapping.queueStrategy";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_TRANSPORT_SECRET_PROVIDER = "mapping.transportSecretProvider";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_PACKAGE_BUILDER = "mapping.packageBuilder";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_REQUEST_AUTHORIZATION = "mapping.requestAuthorization";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String MAPPING_TRIGGER = "mapping.trigger";

    private final Map<DistributionComponentKind, Map<String, String>> mapping =
            new HashMap<DistributionComponentKind, Map<String, String>>();

    @Activate
    protected void activate(Map<String, Object> config) {
        mapping.put(AGENT, parse(toStringArray(config.get(MAPPING_AGENT)), MAPPING_AGENT_DEFAULT));
        mapping.put(IMPORTER, parse(toStringArray(config.get(MAPPING_IMPORTER)), MAPPING_IMPORTER_DEFAULT));
        mapping.put(EXPORTER, parse(toStringArray(config.get(MAPPING_EXPORTER)), MAPPING_EXPORTER_DEFAULT));
        mapping.put(QUEUE_PROVIDER, parse(toStringArray(config.get(MAPPING_QUEUE_PROVIDER)), MAPPING_QUEUE_PROVIDER_DEFAULT));
        mapping.put(QUEUE_STRATEGY, parse(toStringArray(config.get(MAPPING_QUEUE_STRATEGY)), MAPPING_QUEUE_STRATEGY_DEFAULT));
        mapping.put(TRANSPORT_SECRET_PROVIDER, parse(toStringArray(config.get(MAPPING_TRANSPORT_SECRET_PROVIDER)), MAPPING_TRANSPORT_SECRET_PROVIDER_DEFAULT));
        mapping.put(PACKAGE_BUILDER, parse(toStringArray(config.get(MAPPING_PACKAGE_BUILDER)), MAPPING_PACKAGE_BUILDER_DEFAULT));
        mapping.put(REQUEST_AUTHORIZATION, parse(toStringArray(config.get(MAPPING_REQUEST_AUTHORIZATION)), MAPPING_REQUEST_AUTHORIZATION_DEFAULT));
        mapping.put(TRIGGER, parse(toStringArray(config.get(MAPPING_TRIGGER)), MAPPING_TRIGGER_DEFAULT));
    }

    String getType(DistributionComponentKind kind, @NotNull String factoryPid) {
        Map<String,String> entries = getEntries(kind);
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (factoryPid.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    String getFactoryPid(DistributionComponentKind kind, String type) {
        return getEntries(kind).get(type);
    }

    List<String> getFactoryPids(DistributionComponentKind kind) {
        return new ArrayList<String>(getEntries(kind).values());
    }

    //

    private Map<String,String> parse(@Nullable String[] mappings, @NotNull String[] defaultMappings) {
        Map<String,String> parsed = new HashMap<String, String>();
        parsed.putAll(parse(defaultMappings));
        if (mappings != null) {
            parsed.putAll(parse(mappings));
        }
        return parsed;
    }

    private Map<String,String> parse(@NotNull String[] mappings) {
        Map<String, String> map = new HashMap<String, String>();
        for (String mapping : mappings) {
            String[] chunks = mapping.split(":");
            if (chunks.length != 2) {
                log.info(format("Skipping invalid mapping entry %s", mapping));
            } else {
                map.put(chunks[0], chunks[1]);
            }
        }
        return map;
    }

    private Map<String,String> getEntries(DistributionComponentKind kind) {
        Map<String,String> entries = mapping.get(kind);
        if (entries == null) {
            throw new IllegalArgumentException(format("No mapping for components kind %s", kind));
        }
        return entries;
    }
}
