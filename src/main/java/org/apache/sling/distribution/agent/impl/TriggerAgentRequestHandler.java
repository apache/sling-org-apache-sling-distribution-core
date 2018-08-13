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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link DistributionRequestHandler} to trigger an agent.
 */
class TriggerAgentRequestHandler implements DistributionRequestHandler {
    private final SimpleDistributionAgentAuthenticationInfo authenticationInfo;
    private final DefaultDistributionLog log;
    private final boolean active;
    private final DistributionAgent agent;
    private final String agentName;

    public TriggerAgentRequestHandler(@NotNull DistributionAgent agent,
                                      @NotNull String agentName,
                                      @NotNull SimpleDistributionAgentAuthenticationInfo authenticationInfo,
                                      @NotNull DefaultDistributionLog log,
                                      boolean active) {
        this.authenticationInfo = authenticationInfo;
        this.log = log;
        this.active = active;
        this.agent = agent;
        this.agentName = agentName;
    }

    public String getName() {
        return agentName;
    }

    public DistributionComponentKind getComponentKind() {
        return DistributionComponentKind.AGENT;
    }

    public void handle(@Nullable ResourceResolver resourceResolver, @NotNull DistributionRequest request) {

        if (!active) {
            log.warn("skipping agent handler as agent is disabled");
            return;
        }

        if (resourceResolver != null) {
            try {
                agent.execute(resourceResolver, request);
            } catch (Throwable t) {
                log.error("Error executing handler {}", request, t);
            }
        } else {
            ResourceResolver agentResourceResolver = null;

            try {
                agentResourceResolver = DistributionUtils.getResourceResolver(null, authenticationInfo.getAgentService(),
                        authenticationInfo.getSlingRepository(), authenticationInfo.getSubServiceName(),
                        authenticationInfo.getResourceResolverFactory());

                agent.execute(agentResourceResolver, request);
            } catch (Throwable e) {
                log.error("Error executing handler {}", request, e);
            } finally {
                DistributionUtils.ungetResourceResolver(agentResourceResolver);
            }
        }

    }

    @Override
    public String toString() {
        return "TriggerAgentRequestHandler{" +
                "agentName='" + agentName+ '\'' +
                '}';
    }
}
