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
package org.apache.sling.distribution.monitor.impl;

import javax.management.ObjectName;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.apache.sling.distribution.queue.spi.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.spi.DistributionQueueProvider;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DistributionQueueProvider} that supports monitoring via JMX.
 */
public class MonitoringDistributionQueueProvider implements DistributionQueueProvider {
    Logger log = LoggerFactory.getLogger(MonitoringDistributionQueueProvider.class);

    private final Set<String> monitoredQueues = new HashSet<String>();

    private final List<ServiceRegistration<DistributionQueueMBean>> mBeans = new LinkedList<>();

    private final DistributionQueueProvider wrapped;

    private final BundleContext context;

    public MonitoringDistributionQueueProvider(DistributionQueueProvider wrapped, BundleContext context) {
        this.wrapped = wrapped;
        this.context = context;
    }

    @Override
    public DistributionQueue getQueue(String queueName) throws DistributionException {
        DistributionQueue distributionQueue = wrapped.getQueue(queueName);
        monitorQueue(distributionQueue);
        return distributionQueue;
    }

    @Override
    public DistributionQueue getQueue(String queueName, DistributionQueueType type) {
        DistributionQueue distributionQueue = wrapped.getQueue(queueName, type);
        monitorQueue(distributionQueue);
        return distributionQueue;
    }

    @Override
    public void enableQueueProcessing(DistributionQueueProcessor queueProcessor, String... queueNames) throws DistributionException {
        wrapped.enableQueueProcessing(queueProcessor, queueNames);
    }

    @Override
    public void disableQueueProcessing() throws DistributionException {
        wrapped.disableQueueProcessing();

        for (ServiceRegistration<DistributionQueueMBean> mBean : mBeans) {
            safeUnregister(mBean);
        }

        mBeans.clear();
        monitoredQueues.clear();
    }

    private void monitorQueue(DistributionQueue distributionQueue) {
        try {
            if (monitoredQueues.add(distributionQueue.getName())) {
                DistributionQueueMBean mBean = new DistributionQueueMBeanImpl(distributionQueue);

                Dictionary<String, String> mBeanProps = new Hashtable<String, String>();
                mBeanProps.put("jmx.objectname", "org.apache.sling.distribution:type=queue,id="
                        + ObjectName.quote(distributionQueue.getName()));

                ServiceRegistration<DistributionQueueMBean> mBeanRegistration = context.registerService(DistributionQueueMBean.class, mBean, mBeanProps);
                mBeans.add(mBeanRegistration);
            }
        } catch (Throwable e) {
            log.error("cannot register queue mbean", e);
        }

    }
    
    private static void safeUnregister(ServiceRegistration<?> serviceRegistration) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

}
