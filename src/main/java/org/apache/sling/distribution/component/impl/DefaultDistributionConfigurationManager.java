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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.component.impl.DistributionConfigurationManager} implementation based on OSGI configs.
 * For each tree of properties a set of OSGI configs is generated and registered in ConfigurationAdmin.
 * To delete a component all configs owned by that component will be unregistered from ConfigurationAdmin.
 */
@Component(service=DistributionConfigurationManager.class)
@Designate(ocd=DefaultDistributionConfigurationManager.Config.class)
public class DefaultDistributionConfigurationManager implements DistributionConfigurationManager {
    
    @ObjectClassDefinition()
    public @interface Config {
        @AttributeDefinition(name="Resource Config enabled",description = "If storing config in resource tree is enabled.")
        boolean resource_config_enabled() default false;
        
        @AttributeDefinition(name="Resource Config prefix",description = "The prefix of properties to be stored in content")
        String resourceConfigPrefix() default "etc.";
        
        @AttributeDefinition(name="Resource Config Root", description = "The resource config root")
        String resourceConfigRoot() default "/etc/distribution";
        
        @AttributeDefinition(name="Resource Config Properties", description = "The resource config properties")
        String[] resourceConfigProperties() default {"enabled"};
        
        @AttributeDefinition(name="Resource Config Defaults", description = "The default values for resource config properties")
        String[] resourceConfigDefaults() default {"serializationType=distribution"};
    }

    @Reference
    ConfigurationAdmin configurationAdmin;

    @Reference
    private DistributionComponentFactoryMap componentFactoryMap;

    private DistributionConfigurationManager osgiManager;
    private DistributionConfigurationManager resourceManager;

    private String resourcePrefix;
    static final String OSGI_PREFIX = "";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    void activate(ComponentContext ctx, Config conf) {
        boolean configEnabled = conf.resource_config_enabled();

        String configRoot = SettingsUtils.removeEmptyEntry(conf.resourceConfigRoot());
        resourcePrefix = SettingsUtils.removeEmptyEntry(conf.resourceConfigPrefix());

        String[] configProperties = SettingsUtils.removeEmptyEntries(conf.resourceConfigProperties());

        if (configEnabled && configRoot != null && configProperties != null) {
            Map<String, String> configDefaults = PropertiesUtil.toMap(conf.resourceConfigDefaults(), new String[0]);

            resourceManager = new ResourceConfigurationManager(configRoot, configProperties, configDefaults);
        }

        osgiManager = new OsgiConfigurationManager(configurationAdmin, componentFactoryMap, ctx.getServiceReference());
    }

    @Deactivate
    void deactivate() {
        resourceManager = null;
        osgiManager = null;
    }


    @Override
    public List<DistributionConfiguration> getConfigs(ResourceResolver resolver, DistributionComponentKind kind) {
        List<DistributionConfiguration> result = osgiManager.getConfigs(resolver, kind);

        if (resourceManager != null) {
            List<DistributionConfiguration> resourceConfigs = resourceManager.getConfigs(resolver, kind);
            result = mergeConfigs(result, resourceConfigs, resourcePrefix);
        }

        return result;
    }

    @Override
    public DistributionConfiguration getConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {

        DistributionConfiguration resultConfig = osgiManager.getConfig(resolver, kind, name);

        if (resourceManager != null) {
            DistributionConfiguration resourceConfig = resourceManager.getConfig(resolver, kind, name);
            resultConfig = mergeConfig(resultConfig, resourceConfig, resourcePrefix);
        }

        return resultConfig;
    }

    @Override
    public void saveConfig(ResourceResolver resolver, DistributionConfiguration config) {
        log.debug("saving config {}", config);

        if (resourceManager != null) {
            Map<String, DistributionConfiguration> splitConfig = splitConfig(config, resourcePrefix);

            DistributionConfiguration defaultConfig = splitConfig.get(OSGI_PREFIX);

            osgiManager.saveConfig(resolver, defaultConfig);

            DistributionConfiguration resourceConfig = splitConfig.get(resourcePrefix);
            log.debug("retrieved config {}", resourceConfig);

            if (resourceConfig != null) {
                resourceManager.saveConfig(resolver, resourceConfig);
                log.debug("saved resource config: {}", resourceConfig);
            }
        } else {
            osgiManager.saveConfig(resolver, config);
            log.debug("saved osgi config: {}", config);
        }
    }

    @Override
    public void deleteConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {
        osgiManager.deleteConfig(resolver, kind, name);

        if (resourceManager != null) {
            resourceManager.deleteConfig(resolver, kind, name);
        }
    }

    static DistributionConfiguration mergeConfig(DistributionConfiguration main, DistributionConfiguration extension, String prefix) {

        if (main == null) {
            return null;
        }

        Map<String, DistributionConfiguration> configMap = new HashMap<String, DistributionConfiguration>();

        configMap.put(OSGI_PREFIX, main);
        configMap.put(prefix, extension);

        return mergeConfig(main.getKind(), main.getName(), configMap);
    }

    static DistributionConfiguration mergeConfig(DistributionComponentKind kind, String name, Map<String, DistributionConfiguration> configMap) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (String prefixKey : configMap.keySet()) {
            DistributionConfiguration config = configMap.get(prefixKey);
            if (config == null) {
                continue;
            }
            Map<String, Object> properties = config.getProperties();

            if (prefixKey.equals(OSGI_PREFIX)) {
                result.putAll(properties);
            } else {
                for (String propertyKey : properties.keySet()) {
                    Object propertyValue = properties.get(propertyKey);
                    result.put(prefixKey + propertyKey, propertyValue);
                }
            }
        }

        return new DistributionConfiguration(kind, name, result);
    }

    static List<DistributionConfiguration> mergeConfigs(List<DistributionConfiguration> target, List<DistributionConfiguration> source, String prefix) {
        List<DistributionConfiguration> result = new ArrayList<DistributionConfiguration>();

        Map<String, DistributionConfiguration> sourceMap = new HashMap<String, DistributionConfiguration>();
        for (DistributionConfiguration config : source) {
            sourceMap.put(config.getName(), config);
        }

        for (DistributionConfiguration targetConfig : target) {
            DistributionConfiguration sourceConfig = sourceMap.get(targetConfig.getName());

            DistributionConfiguration mergedConfig = mergeConfig(targetConfig, sourceConfig, prefix);

            result.add(mergedConfig);
        }

        return result;
    }

    /**
     * Split a {@link DistributionConfiguration} into two configurations, so that properties starting with a certain prefix
     * go to a separate configuration while the remaining ones will live in a new configuration
     * @param config a configuration
     * @param prefix a prefix
     * @return a {@link Map} of prefix -> configuration
     */
    private static Map<String, DistributionConfiguration> splitConfig(DistributionConfiguration config, String prefix) {
        Map<String, Object> distributionConfigurationProperties = config.getProperties();

        // properties for OSGi configuration
        Map<String, Object> defaultMap = new HashMap<String, Object>();

        // properties for resource configuration
        Map<String, Object> prefixMap = new HashMap<String, Object>();

        // split the properties of the given configuration between the OSGi config and the persisted config
        for (String configurationPropertyKey : distributionConfigurationProperties.keySet()) {
            if (configurationPropertyKey.startsWith(prefix)) {
                prefixMap.put(configurationPropertyKey.substring(prefix.length()), distributionConfigurationProperties.get(configurationPropertyKey));
            } else {
                defaultMap.put(configurationPropertyKey, distributionConfigurationProperties.get(configurationPropertyKey));
            }
        }

        // create an OSGi and a persisted configuration
        Map<String, DistributionConfiguration> result = new HashMap<String, DistributionConfiguration>();
        result.put(OSGI_PREFIX, new DistributionConfiguration(config.getKind(), config.getName(), defaultMap));
        if (prefixMap.size() > 0) {
            result.put(prefix, new DistributionConfiguration(config.getKind(), config.getName(), prefixMap));
        }

        return result;
    }
}
