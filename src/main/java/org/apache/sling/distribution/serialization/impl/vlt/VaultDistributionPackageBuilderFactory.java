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
package org.apache.sling.distribution.serialization.impl.vlt;

import org.apache.jackrabbit.vault.fs.api.IdConflictPolicy;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * A package builder for Apache Jackrabbit FileVault based implementations.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionPackageBuilder.class,
        property= {
                "webconsole.configurationFactory.nameHint=Builder name: {name}"
        })
@Designate(ocd=VaultDistributionPackageBuilderFactory.Config.class, factory=true)
public class VaultDistributionPackageBuilderFactory implements DistributionPackageBuilder {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Packaging - Vault Package Builder Factory",
            description = "OSGi configuration for vault package builders")
    public @interface Config {
        @AttributeDefinition(name="name",description = "The name of the package builder.")
        String name();
        @AttributeDefinition(options = {
                @Option(label = "jcr packages", value = "jcrvlt"),
                @Option(label = "file packages",value = "filevlt"),
                @Option(label = "in memory packages",value = "inmemory")},
                name = "type", description = "The type of this package builder")
        String type() default "jcrvlt";
        @AttributeDefinition(name="Import Mode", description = "The vlt import mode for created packages.")
        String importMode();
        @AttributeDefinition(name="Acl Handling", description = "The vlt acl handling mode for created packages.")
        String aclHandling();
        @AttributeDefinition(name="Cug Handling", description = "The vlt cug handling mode for created packages.")
        String cugHandling();
        @AttributeDefinition(name="Package Roots", description = "The package roots to be used for created packages. "
                + "(this is useful for assembling packages with an user that cannot read above the package root)")
        String[] package_roots();
        
        @AttributeDefinition(name="Package Node Filters", 
                description = "The package node path filters. Filter format: path|+include|-exclude", 
                cardinality = 100)
        String[] package_filters();
        
        @AttributeDefinition(name="Package Property Filters", 
                description = "The package property path filters. Filter format: path|+include|-exclude")
        String[] property_filters();
        
        @AttributeDefinition(name="Temp Filesystem Folder", 
                description = "The filesystem folder where the temporary files should be saved.")
        String tempFsFolder();
        
        @AttributeDefinition(name="Use Binary References", 
                description = "If activated, it avoids sending binaries in the distribution package.")
        boolean useBinaryReferences() default false;
        
        @AttributeDefinition(name="Autosave threshold", description = "The value after which autosave is triggered for intermediate changes.")
        int autoSaveThreshold() default -1;
        
        @AttributeDefinition(
                name = "The delay in seconds between two runs of the cleanup phase for resource persisted packages.",
                description = "The resource persisted packages are cleaned up periodically (asynchronously) since SLING-6503." +
                        "The delay between two runs of the cleanup phase can be configured with this setting. 60 seconds by default")
        long cleanupDelay() default DEFAULT_PACKAGE_CLEANUP_DELAY;
        
        @AttributeDefinition(
                name = "File threshold (in bytes)",
                description = "Once the data reaches the configurable size value, buffering to memory switches to file buffering.")
        int fileThreshold() default DEFAULT_FILE_THRESHOLD_VALUE;
        
        @AttributeDefinition(
                name = "The memory unit for the file threshold",
                description = "The memory unit for the file threshold, Megabytes by default",
                options = {
                        @Option(label = "BYTES", value = "Bytes"),
                        @Option(label = "KILO_BYTES", value = "Kilobytes"),
                        @Option(label = "MEGA_BYTES", value = "Megabytes"),
                        @Option(label = "GIGA_BYTES", value = "Gigabytes")
                })
        String MEGA_BYTES() default DEFAULT_MEMORY_UNIT;
        
        @AttributeDefinition(
                name = "Flag to enable/disable the off-heap memory",
                description = "Flag to enable/disable the off-heap memory, false by default")
        boolean useOffHeapMemory() default DEFAULT_USE_OFF_HEAP_MEMORY;
        
        @AttributeDefinition(
                name = "The digest algorithm to calculate the package checksum",
                description = "The digest algorithm to calculate the package checksum, Megabytes by default",
                options = {
                        @Option(label = DEFAULT_DIGEST_ALGORITHM, value = "Do not send digest"),
                        @Option(label = "MD2", value = "md2"),
                        @Option(label = "MD5", value = "md5"),
                        @Option(label = "SHA-1", value = "sha1"),
                        @Option(label = "SHA-256", value = "sha256"),
                        @Option(label = "SHA-384", value = "sha384"),
                        @Option(label = "SHA-512", value = "sha512")
                })
        String digestAlgorithm() default DEFAULT_DIGEST_ALGORITHM;
        
        @AttributeDefinition(
                name = "The number of items for monitoring distribution packages creation/installation",
                description = "The number of items for monitoring distribution packages creation/installation, 100 by default")
        int monitoringQueueSize() default DEFAULT_MONITORING_QUEUE_SIZE;
        
        @AttributeDefinition(cardinality = 100,
                name = "Paths mapping",
                description = "List of paths that require be mapped." +
                "The format is {sourcePattern}={destinationPattern}, e.g. /etc/(.*)=/var/$1/some or simply /data=/bak")
        String[] pathsMapping();
    }

    private static final long DEFAULT_PACKAGE_CLEANUP_DELAY = 60L;
    // 1M
    private static final int DEFAULT_FILE_THRESHOLD_VALUE = 1;
    private static final String DEFAULT_MEMORY_UNIT = "MEGA_BYTES";
    private static final boolean DEFAULT_USE_OFF_HEAP_MEMORY = false;
    private static final String DEFAULT_DIGEST_ALGORITHM = "NONE";
    private static final int DEFAULT_MONITORING_QUEUE_SIZE = 0;

    // importer setting constants, can be removed once JCRVLT-656 is implemented
    private static final String PACKAGING_CONFIGURATION_PID = "org.apache.jackrabbit.vault.packaging.impl.PackagingImpl";
    private static final String PROPERTY_IS_STRICT = "isStrict";
    private static final String PROPERTY_DEFAULT_ID_CONFLICT_POLICY = "defaultIdConflictPolicy";
    private static final String PROPERTY_OVERWRITE_PRIMARY_TYPES_OF_FOLDER = "overwritePrimaryTypesOfFolders";
    private static final boolean DEFAULT_IS_STRICT = true;
    private static final boolean DEFAULT_OVERWRITE_PRIMARY_TYPES_OF_FOLDER = true;
    private static final IdConflictPolicy DEFAULT_ID_CONFLICT_POLICY = IdConflictPolicy.FAIL;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    private ServiceRegistration<Runnable> packageCleanup = null;

    private MonitoringDistributionPackageBuilder packageBuilder;

    @Activate
    public void activate(BundleContext context, Config conf) {

        String name = conf.name();
        String type = conf.type();
        String importModeString = SettingsUtils.removeEmptyEntry(conf.importMode());
        String aclHandlingString = SettingsUtils.removeEmptyEntry(conf.aclHandling());
        String cugHandlingString = SettingsUtils.removeEmptyEntry(conf.cugHandling());

        String[] packageRoots = SettingsUtils.removeEmptyEntries(conf.package_roots());
        String[] packageNodeFilters = SettingsUtils.removeEmptyEntries(conf.package_filters());
        String[] packagePropertyFilters = SettingsUtils.removeEmptyEntries(conf.property_filters());

        long cleanupDelay = conf.cleanupDelay();

        String tempFsFolder = SettingsUtils.removeEmptyEntry(conf.tempFsFolder());
        boolean useBinaryReferences = conf.useBinaryReferences();

        String digestAlgorithm = conf.digestAlgorithm();
        if (DEFAULT_DIGEST_ALGORITHM.equals(digestAlgorithm)) {
            digestAlgorithm = null;
        }

        // check the mount path patterns, if any
        Map<String, String> pathsMapping = toMap(conf.pathsMapping(), new String[0]);
        pathsMapping = SettingsUtils.removeEmptyEntries(pathsMapping);

        // import settings
        ImportSettings importSettings = ImportSettings.builder().build();
        importSettings.setAutosaveThreshold(conf.autoSaveThreshold());

        if (importModeString != null) {
            importSettings.setImportMode(ImportMode.valueOf(importModeString.trim()));
        }

        if (aclHandlingString != null) {
            importSettings.setAclHandling(AccessControlHandling.valueOf(aclHandlingString.trim()));
        }

        if (cugHandlingString != null) {
            importSettings.setCugHandling(AccessControlHandling.valueOf(cugHandlingString.trim()));
        }

        // read settings from Packaging configuration
        importSettings.setStrict(DEFAULT_IS_STRICT);
        importSettings.setOverwritePrimaryTypesOfFolders(DEFAULT_OVERWRITE_PRIMARY_TYPES_OF_FOLDER);
        importSettings.setIdConflictPolicy(DEFAULT_ID_CONFLICT_POLICY);
        try {
            Configuration packagingConfig = configurationAdmin.getConfiguration(PACKAGING_CONFIGURATION_PID);
            Dictionary<String, Object> properties = packagingConfig.getProperties();
            if (properties != null) {
                importSettings.setStrict((Boolean) properties.get(PROPERTY_IS_STRICT));
                importSettings.setOverwritePrimaryTypesOfFolders((Boolean) properties.get(PROPERTY_OVERWRITE_PRIMARY_TYPES_OF_FOLDER));
                importSettings.setIdConflictPolicy((IdConflictPolicy) properties.get(PROPERTY_DEFAULT_ID_CONFLICT_POLICY));
            }
        } catch (IOException e) {
            log.warn("Could not read the OSGi configuration {}, falling back on default values.", PACKAGING_CONFIGURATION_PID);
        }

        DistributionContentSerializer contentSerializer = new FileVaultContentSerializer(name, packaging, packageRoots, packageNodeFilters,
                packagePropertyFilters, useBinaryReferences, pathsMapping, importSettings);

        DistributionPackageBuilder wrapped;
        if ("filevlt".equals(type)) {
            wrapped = new FileDistributionPackageBuilder(name, contentSerializer, tempFsFolder, digestAlgorithm, packageNodeFilters, packagePropertyFilters);
        } else if ("inmemory".equals(type)) {
            wrapped = new InMemoryDistributionPackageBuilder(name, contentSerializer, packageNodeFilters, packagePropertyFilters);
        } else {
            final int fileThreshold = conf.fileThreshold();
            String memoryUnitName = conf.MEGA_BYTES();
            final MemoryUnit memoryUnit = MemoryUnit.valueOf(memoryUnitName);
            final boolean useOffHeapMemory = conf.useOffHeapMemory();
            ResourceDistributionPackageBuilder resourceDistributionPackageBuilder = new ResourceDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, fileThreshold, memoryUnit, useOffHeapMemory, digestAlgorithm, packageNodeFilters, packagePropertyFilters);
            Runnable cleanup = new ResourceDistributionPackageCleanup(resolverFactory, resourceDistributionPackageBuilder);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, false);
            props.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, cleanupDelay);
            props.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_SINGLE);
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
    
    
   /**
    * (taken and adjusted from PropertiesUtil, because the handy toMap() function is not available as
    * part of the metatype functionality
    * 
    * @param values The path mappings.
    * @param defaultArray The default array converted to map.
    * @return Map value
    */
   private static Map<String, String> toMap(String[] values, String[] defaultArray) {

       if (values == null) {
           values = defaultArray;
       }

       //in property values
       Map<String, String> result = new LinkedHashMap<String, String>();
       for (String kv : values) {
           int indexOfEqual = kv.indexOf('=');
           if (indexOfEqual > 0) {
               String key = trimToNull(kv.substring(0, indexOfEqual));
               String value = trimToNull(kv.substring(indexOfEqual + 1));
               if (key != null) {
                   result.put(key, value);
               }
           }
       }
       return result;
   }
   
   private static String trimToNull(String str)    {
       String ts = trim(str);
       return isEmpty(ts) ? null : ts;
   }
   
   private static String trim(String str){
       return str == null ? null : str.trim();
   }
   
   private static boolean isEmpty(String str){
       return str == null || str.length() == 0;
   }
    
}
