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
package org.apache.sling.distribution.serialization.impl;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.monitor.impl.MonitoringDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.InMemoryDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageCleanup;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * A factory for package builders
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageBuilder.class,
        property= {
                "webconsole.configurationFactory.nameHint=Builder name: {name}"     
        })
@Designate(ocd=DistributionPackageBuilderFactory.Config.class, factory=true)
public class DistributionPackageBuilderFactory implements DistributionPackageBuilder {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Packaging - Package Builder Factory",
            description = "OSGi configuration for package builders")
    public @interface Config {
        @AttributeDefinition(name="name", description = "The name of the package builder.")
        String name();
        @AttributeDefinition(name="type",description = "The persistence type used by this package builder",
                options = {
                        @Option(label = "resource", value="resource package"),
                        @Option(label="file", value="file package"),
                        @Option(label="inmemory", value="in memory packages")
                })
        String type() default "resource"; // persistence
        
        @AttributeDefinition(name="Content Serializer",description = "The target reference for the DistributionSerializationFormat used to (de)serialize packages, " +
                "e.g. use target=(name=...) to bind to services by name.")
        String format_target() default SettingsUtils.COMPONENT_NAME_DEFAULT;
        
        @AttributeDefinition(name="Temp Filesystem Folder", description = "The filesystem folder where the temporary files should be saved.")
        String tempFsFolder();
        
        @AttributeDefinition(name="File threshold",description = "Once the data reaches the configurable size value, buffering to memory switches to file buffering.")
        int fileThreshold() default DEFAULT_FILE_THRESHOLD_VALUE;
        
        @AttributeDefinition(name="The memory unit for the file threshold",
        description = "The memory unit for the file threshold, Megabytes by default",
        options = {
                @Option(label = "BYTES", value = "Bytes"),
                @Option(label = "KILO_BYTES", value = "Kilobytes"),
                @Option(label = "MEGA_BYTES", value = "Megabytes"),
                @Option(label = "GIGA_BYTES", value = "Gigabytes")
        })
        String memoryUnit() default DEFAULT_MEMORY_UNIT;
        
        @AttributeDefinition(name="Flag to enable/disable the off-heap memory", description="Flag to enable/disable the off-heap memory, false by default")
        boolean useOffHeapMemory() default DEFAULT_USE_OFF_HEAP_MEMORY;
        
        @AttributeDefinition(
                name = "The digest algorithm to calculate the package checksum",
                description = "The digest algorithm to calculate the package checksum, Megabytes by default",
                options = {
                    @Option(label = "NONE", value = "Do not send digest"),
                    @Option(label = "MD2", value = "md2"),
                    @Option(label = "MD5", value = "md5"),
                    @Option(label = "SHA-1", value = "sha1"),
                    @Option(label = "SHA-256", value = "sha256"),
                    @Option(label = "SHA-384", value = "sha384"),
                    @Option(label = "SHA-512", value = "sha512")
                })
        String digestAlgorithm() default DEFAULT_DIGEST_ALGORITHM;
        
        @AttributeDefinition(
                name="The number of items for monitoring distribution packages creation/installation",
                description = "The number of items for monitoring distribution packages creation/installation, 100 by default")
        int monitoringQueueSize() default DEFAULT_MONITORING_QUEUE_SIZE;
        
        @AttributeDefinition(
                name="The delay in seconds between two runs of the cleanup phase for resource persisted packages.",
                description = "The resource persisted packages are cleaned up periodically (asynchronously) since SLING-6503." +
                        "The delay between two runs of the cleanup phase can be configured with this setting. 60 seconds by default")
        long cleanupDelay() default DEFAULT_PACKAGE_CLEANUP_DELAY;
        
        @AttributeDefinition(
                name = "Package Node Filters", 
                description = "The package node path filters. Filter format: path|+include|-exclude", 
                cardinality = 100)
        String[] package_filters();
        
        @AttributeDefinition(
                name = "Package Property Filters", 
                description = "The package property path filters. Filter format: path|+include|-exclude",
                cardinality = Integer.MAX_VALUE)
        String[] property_filters();
    }

    private MonitoringDistributionPackageBuilder packageBuilder;

    private ServiceRegistration<Runnable> packageCleanup = null;
    
    @Reference(name = "format")
    private DistributionContentSerializer contentSerializer;

    @Reference
    private ResourceResolverFactory resolverFactory;


    // 1M
    private static final int DEFAULT_FILE_THRESHOLD_VALUE = 1;
    private static final String DEFAULT_MEMORY_UNIT = "MEGA_BYTES";
    private static final boolean DEFAULT_USE_OFF_HEAP_MEMORY = false;
    private static final String DEFAULT_DIGEST_ALGORITHM = "NONE";
    private static final int DEFAULT_MONITORING_QUEUE_SIZE = 0;
    private static final long DEFAULT_PACKAGE_CLEANUP_DELAY = 60L;

    @Activate
    public void activate(BundleContext context,
                         Config conf) {

        String[] nodeFilters = SettingsUtils.removeEmptyEntries(conf.package_filters());
        String[] propertyFilters = SettingsUtils.removeEmptyEntries(conf.property_filters());
        String persistenceType = conf.type();
        String tempFsFolder = SettingsUtils.removeEmptyEntry(conf.tempFsFolder());
        String digestAlgorithm = conf.digestAlgorithm();
        long cleanupDelay = conf.cleanupDelay();
        if (DEFAULT_DIGEST_ALGORITHM.equals(digestAlgorithm)) {
            digestAlgorithm = null;
        }

        DistributionPackageBuilder wrapped;
        if ("file".equals(persistenceType)) {
            wrapped = new FileDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, digestAlgorithm, nodeFilters, propertyFilters);
        } else if ("inmemory".equals(persistenceType)) {
            wrapped = new InMemoryDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, nodeFilters, propertyFilters);
        } else {
            final int fileThreshold = conf.fileThreshold();
            String memoryUnitName = conf.memoryUnit();
            final MemoryUnit memoryUnit = MemoryUnit.valueOf(memoryUnitName);
            final boolean useOffHeapMemory = conf.useOffHeapMemory();
            ResourceDistributionPackageBuilder resourceDistributionPackageBuilder = new ResourceDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, fileThreshold, memoryUnit, useOffHeapMemory, digestAlgorithm, nodeFilters, propertyFilters);
            Runnable cleanup = new ResourceDistributionPackageCleanup(resolverFactory, resourceDistributionPackageBuilder);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, false);
            props.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, cleanupDelay);
            props.put(Scheduler.PROPERTY_SCHEDULER_THREAD_POOL, "content-distribution");
            packageCleanup = context.registerService(Runnable.class, cleanup, props);
            wrapped = resourceDistributionPackageBuilder;
        }

        int monitoringQueueSize = conf.monitoringQueueSize();
        packageBuilder = new MonitoringDistributionPackageBuilder(monitoringQueueSize, wrapped, context);
    }

    @Deactivate
    public void deactivate() {
        packageBuilder.clear();
        if (packageCleanup != null) {
            packageCleanup.unregister();
        }
    }

    public String getType() {
        return packageBuilder.getType();
    }

    @NotNull
    public DistributionPackage createPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest request) throws DistributionException {
        return packageBuilder.createPackage(resourceResolver, request);
    }

    @NotNull
    public DistributionPackage readPackage(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return packageBuilder.readPackage(resourceResolver, stream);
    }

    @Nullable
    public DistributionPackage getPackage(@NotNull ResourceResolver resourceResolver, @NotNull String id) throws DistributionException {
        return packageBuilder.getPackage(resourceResolver, id);
    }

    public boolean installPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage) throws DistributionException {
        return packageBuilder.installPackage(resourceResolver, distributionPackage);
    }

    @NotNull
    @Override
    public DistributionPackageInfo installPackage(@NotNull ResourceResolver resourceResolver, @NotNull InputStream stream) throws DistributionException {
        return packageBuilder.installPackage(resourceResolver, stream);
    }
}
