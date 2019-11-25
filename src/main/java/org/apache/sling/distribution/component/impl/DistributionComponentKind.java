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

import static org.apache.sling.distribution.resources.DistributionResourceTypes.AGENT_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.AGENT_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.DEFAULT_SERVICE_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.EXPORTER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.EXPORTER_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.IMPORTER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.IMPORTER_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.TRIGGER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.TRIGGER_RESOURCE_TYPE;

import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.agent.impl.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageImporter;
import org.apache.sling.distribution.queue.spi.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;

/**
 * Enum that represents the main distribution component kinds that can be configured for distribution.
 */
public enum DistributionComponentKind {

    AGENT("agent", AGENT_RESOURCE_TYPE, AGENT_LIST_RESOURCE_TYPE, DistributionAgent.class),

    IMPORTER("importer", IMPORTER_RESOURCE_TYPE, IMPORTER_LIST_RESOURCE_TYPE, DistributionPackageImporter.class),

    EXPORTER("exporter", EXPORTER_RESOURCE_TYPE, EXPORTER_LIST_RESOURCE_TYPE, DistributionPackageExporter.class),

    QUEUE_PROVIDER("queueProvider", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionQueueProvider.class),

    QUEUE_STRATEGY("queueStrategy", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionQueueDispatchingStrategy.class),

    TRANSPORT_SECRET_PROVIDER("transportSecretProvider", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionTransportSecretProvider.class),

    PACKAGE_BUILDER("packageBuilder", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionPackageBuilder.class),

    REQUEST_AUTHORIZATION("requestAuthorization", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionRequestAuthorizationStrategy.class),

    TRIGGER("trigger", TRIGGER_RESOURCE_TYPE, TRIGGER_LIST_RESOURCE_TYPE, DistributionTrigger.class);

    private final String name;

    private final String resourceType;

    private final String rootResourceType;

    private final Class<?> type;

    DistributionComponentKind(String name,
                              String resourceType,
                              String rootResourceType,
                              Class<?> type) {
        this.name = name;
        this.resourceType = resourceType;
        this.rootResourceType = rootResourceType;
        this.type = type;
    }

    public Class<?> asClass() {
        return type;
    }

    public static DistributionComponentKind fromClass(Class<?> type) {
        for (DistributionComponentKind kind : values()) {
            Class<?> kindClass = kind.asClass();

            if (kindClass.equals(type)) {
                return kind;
            }
        }

        return null;
    }

    public static DistributionComponentKind fromName(String name) {
        for (DistributionComponentKind kind : values()) {

            if (kind.getName().equals(name)) {
                return kind;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getRootResourceType() {
        return rootResourceType;
    }

}