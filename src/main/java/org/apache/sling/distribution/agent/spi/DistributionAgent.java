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
package org.apache.sling.distribution.agent.spi;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.log.spi.DistributionLog;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A distribution agent is responsible for handling {@link org.apache.sling.distribution.DistributionRequest}s.
 *
 * This means executing actions of e.g.: a specific {@link org.apache.sling.distribution.DistributionRequestType}s on
 * specific path(s) which will resume pulling resources from a certain Sling instance and / or pushing resources to
 * other instances.
 */
@ConsumerType
public interface DistributionAgent {

    /**
     * Retrieves the names of the queues for this agent.
     *
     * @return the list of queue names
     */
    @NotNull
    Iterable<String> getQueueNames();

    /**
     * Get the agent queue with the given name
     *
     * @param name a queue name
     * @return a {@link DistributionQueue} with the given name bound to this agent, if it exists,
     * {@code null} otherwise
     */
    @Nullable
    DistributionQueue getQueue(@NotNull String name);

    /**
     * Get the agent log
     * @return the log for this agent
     */
    @NotNull
    DistributionLog getLog();

    /**
     * returns the state of the agent
     * @return the agent state
     */
    @NotNull
    DistributionAgentState getState();

    /**
     * Perform a {@link org.apache.sling.distribution.DistributionRequest} to distribute content from a source
     * instance to a target instance.
     * The content to be sent will be assembled according to the information contained in the request.
     * A {@link org.apache.sling.distribution.DistributionResponse} holding the {@link org.apache.sling.distribution.DistributionRequestState}
     * of the provided request will be returned.
     * Synchronous {@link DistributionAgent}s will usually block until the execution has finished
     * while asynchronous agents will usually return the response as soon as the content to be distributed has been assembled
     * and scheduled for distribution.
     *
     * @param distributionRequest the distribution request
     * @param resourceResolver    the resource resolver used for authorizing the request,
     * @return a {@link org.apache.sling.distribution.DistributionResponse}
     * @throws DistributionException if any error happens during the execution of the request or if the authentication fails
     */
    @NotNull
    DistributionResponse execute(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest) throws DistributionException;

}
