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
package org.apache.sling.distribution.serialization;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.distribution.serialization.impl.vlt.FileVaultContentSerializer;
import org.apache.sling.distribution.serialization.impl.vlt.ImportSettings;
import org.osgi.service.component.annotations.Component;

import java.util.Map;

@Component(service = DistributionContentSerializerProvider.class)
public class DistributionContentSerializerProvider {

    public DistributionContentSerializer build(String name,
                                        Packaging packaging,
                                        String[] packageRoots,
                                        String[] nodeFilters,
                                        String[] propertyFilters,
                                        boolean useBinaryReferences,
                                        Map<String, String> exportPathMapping,
                                        ImportSettings importSettings) {
        return new FileVaultContentSerializer(name, packaging, packageRoots, nodeFilters, propertyFilters, useBinaryReferences, exportPathMapping, importSettings);
    }

}
