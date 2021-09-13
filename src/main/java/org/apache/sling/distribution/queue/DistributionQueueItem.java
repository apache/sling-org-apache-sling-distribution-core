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

import java.util.Arrays;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.jetbrains.annotations.NotNull;

/**
 * An item in a {@link DistributionQueue}
 * This is basically a proxy to {@link DistributionPackage} designed to avoid having
 * to keep the package {@link DistributionPackage#createInputStream() stream} into
 * the queues.
 */
public final class DistributionQueueItem extends ValueMapDecorator implements ValueMap {

    private final String packageId;
    private final long size;
    private final Map<String, Object> base;

    public DistributionQueueItem(@NotNull String packageId, Map<String, Object> base) {
        this(packageId, -1, base);
    }

    public DistributionQueueItem(String packageId, long size, Map<String, Object> base) {
        super(base);
        this.packageId = packageId;
        this.size = size;
        this.base = base;
    }

    @NotNull
    public String getPackageId() {
        return packageId;
    }

    /**
     * retrieve the size of the package referenced by this queue item.
     * @return the size of the underlying package or {@code -1} if not available.
     */
    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "DistributionQueueItem{" +
                "id='" + packageId + '\'' +
                ", info={" + queueInfo(base) + '}' +
                '}';
    }

    /*
     * convert the map of object values into string form
     */
    private String queueInfo(Map<String, Object> base) {
        String queueItem = "";
        for(String key : base.keySet()) {
            Object value = base.get(key);
            String valueString = "";
            if (value instanceof String[]) {
                valueString = key + "=" + Arrays.toString((String[])value);
            } else {
                valueString = key + "=" + value.toString();
            }
            queueItem = String.join(",", queueItem, valueString);
        }
        return queueItem.isEmpty() ? queueItem : queueItem.substring(1);
    }
}
