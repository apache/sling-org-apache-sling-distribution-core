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

/**
 * Settings that control the package import.
 */
public class ImportSettings {
    private final ImportMode importMode;
    private final AccessControlHandling aclHandling;
    private final AccessControlHandling cugHandling;
    private final int autosaveThreshold;
    private final boolean isStrict;
    private final boolean overwritePrimaryTypesOfFolders;
    private final IdConflictPolicy idConflictPolicy;

    public ImportSettings(ImportMode importMode, AccessControlHandling aclHandling, AccessControlHandling cugHandling, int autosaveThreshold,
                          boolean isStrict, boolean overwritePrimaryTypesOfFolders, IdConflictPolicy idConflictPolicy) {
        this.importMode = importMode;
        this.aclHandling = aclHandling;
        this.cugHandling = cugHandling;
        this.autosaveThreshold = autosaveThreshold;
        this.isStrict = isStrict;
        this.overwritePrimaryTypesOfFolders = overwritePrimaryTypesOfFolders;
        this.idConflictPolicy = idConflictPolicy;
    }

    public ImportMode getImportMode() { return importMode; }

    public AccessControlHandling getAclHandling() { return aclHandling; }

    public AccessControlHandling getCugHandling() { return cugHandling; }

    public int getAutosaveThreshold() { return autosaveThreshold; }

    public boolean isStrict() { return isStrict; }

    public boolean isOverwritePrimaryTypesOfFolders() { return overwritePrimaryTypesOfFolders; }

    public IdConflictPolicy getIdConflictPolicy() { return idConflictPolicy; }
}
