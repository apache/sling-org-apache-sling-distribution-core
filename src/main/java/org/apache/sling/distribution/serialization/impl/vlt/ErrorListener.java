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

import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.packaging.PackageException;

import static java.lang.String.format;

/**
 * Keeps track of the latest error
 */
public class ErrorListener implements ProgressTrackerListener {

    private Exception last = new PackageException("Failed to import");

    public Exception getLastError() {
        return last;
    }

    @Override
    public void onMessage(Mode mode, String action, String path) {
    }

    @Override
    public void onError(Mode mode, String path, Exception e) {
        last = exception(mode, path, e);
    }

    private Exception exception(Mode mode, String path, Exception e) {
        return new PackageException(format("Failed to import %s (%s)", path, e.toString()), e);
    }
}
