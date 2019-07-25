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
package org.apache.sling.distribution.packaging.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.PackageInstallHook;

/**
 * Default hook that simply does nothing
 */
@Component
@Service(PackageInstallHook.class)
@Property(name = "name", value = PackageInstallHook.NOOP_NAME)
public class NoopPackageInstallHook implements PackageInstallHook {

    @Override
    public void onPostAdd(ResourceResolver resourceResolver, DistributionPackage distPackage) {
        // Noop
    }

    @Override
    public void onPreRemove(ResourceResolver resourceResolver, DistributionPackage distPackage) {
        // Noop
    }
}
