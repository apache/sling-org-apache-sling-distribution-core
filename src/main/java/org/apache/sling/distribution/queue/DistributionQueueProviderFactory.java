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

package org.apache.sling.distribution.queue;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.distribution.queue.spi.DistributionQueueProvider;

/**
 * Allows obtaining service instances of {@link DistributionQueueProviderFactory} as regsitered with
 * the OSGi runtime
 */

@ProviderType
public interface DistributionQueueProviderFactory {

    /**
     * given the agent and service name, obtain a provider of {@link DistributionQueueProvider}
     *
     * @param agentName the name of the agent this queue is intended to be used for
     * @param serviceName name to the service used to obtain repository sessions of (if needed)
     * @return a DistributionQueueProvider instance, or {@code null} if none could be obtained
     */
    DistributionQueueProvider getProvider(String agentName, String serviceName);

    /**
     * release (and clean up) the backing resources (compute or otherwise) of the
     * {@link DistributionQueueProvider} supplied
     *
     * @param queueProvider the {@link DistributionQueueProvider} to be released
     */
    void releaseProvider(DistributionQueueProvider queueProvider);

}
