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

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.management.ObjectName;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public final class MonitoringDistributionPackageBuilder implements DistributionPackageBuilder {

    private final DistributionPackageBuilder wrapped;

    private final BundleContext context;

    private final int queueCapacity;

    private final Queue<ServiceRegistration<DistributionPackageMBean>> mBeans;

    public MonitoringDistributionPackageBuilder(int queueCapacity, DistributionPackageBuilder wrapped, BundleContext context) {
        this.wrapped = wrapped;
        this.context = context;
        this.queueCapacity = queueCapacity;

        mBeans = new LinkedBlockingDeque<ServiceRegistration<DistributionPackageMBean>>();
    }

    @Override
    public String getType() {
        return wrapped.getType();
    }

    @Nullable
    @Override
    public DistributionPackage createPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest request) throws DistributionException {
        long start = System.currentTimeMillis();
        DistributionPackage distributionPackage = wrapped.createPackage(resourceResolver, request);
        if (queueCapacity > 0 && distributionPackage != null) {
            registerDistributionPackageMBean(start, distributionPackage);
        }
        return distributionPackage;
    }

    @NotNull
    @Override
    public DistributionPackage readPackage(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return wrapped.readPackage(resourceResolver, stream);
    }

    @Override
    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String id) throws DistributionException {
        return wrapped.getPackage(resourceResolver, id);
    }

    @Override
    public boolean installPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        return wrapped.installPackage(resourceResolver, distributionPackage);
    }

    @NotNull
    @Override
    public DistributionPackageInfo installPackage(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return wrapped.installPackage(resourceResolver, stream);
    }

    private void registerDistributionPackageMBean(long start, DistributionPackage distributionPackage) {
        long processingTime = System.currentTimeMillis() - start;

        DistributionPackageMBean mBean = new DistributionPackageMBeanImpl(distributionPackage,
                                                                          wrapped.getType(),
                                                                          processingTime);

        Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
        mbeanProps.put("jmx.objectname", "org.apache.sling.distribution:type=distributionpackage,id="
                                         + ObjectName.quote(distributionPackage.getId()));

        ServiceRegistration<DistributionPackageMBean> mBeanRegistration = context.registerService(DistributionPackageMBean.class, mBean, mbeanProps);

        if (queueCapacity == mBeans.size()) {
            ServiceRegistration<DistributionPackageMBean> toBeRemoved = mBeans.poll();
            safeUnregister(toBeRemoved);
        }

        mBeans.offer(mBeanRegistration);
    }

    public void clear() {
        while (!mBeans.isEmpty()) {
            ServiceRegistration<DistributionPackageMBean> toBeRemoved = mBeans.poll();
            safeUnregister(toBeRemoved);
        }
    }

    private static void safeUnregister(ServiceRegistration<?> serviceRegistration) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

}
