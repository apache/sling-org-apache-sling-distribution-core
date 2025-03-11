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

import java.util.Map;

/**
 * Settings that control the package export.
 */
public class ExportSettings {
	private final String[] packageRoots;
	private final String[] nodeFilters;
	private final String[] propertyFilters;
	private final boolean useBinaryReferences;
	private final Map<String, String> exportPathMapping;
	
	/**
	 * 
	 * @param packageRoots The serializer package roots
	 * @param nodeFilters The serializer node path filters
	 * @param propertyFilters The serializer property path filters
	 * @param useBinaryReferences {@code true} to pass binaries by reference ;
	 *                            {@code false} to inline binaries
	 * @param exportPathMapping The mapping for exported paths
	 */
	public ExportSettings(String[] packageRoots, String[] nodeFilters, String[] propertyFilters,
			boolean useBinaryReferences, Map<String, String> exportPathMapping) {
		super();
		this.packageRoots = packageRoots;
		this.nodeFilters = nodeFilters;
		this.propertyFilters = propertyFilters;
		this.useBinaryReferences = useBinaryReferences;
		this.exportPathMapping = exportPathMapping;
	}

	public String[] getPackageRoots() {
		return packageRoots;
	}

	public String[] getNodeFilters() {
		return nodeFilters;
	}

	public String[] getPropertyFilters() {
		return propertyFilters;
	}

	public boolean isUseBinaryReferences() {
		return useBinaryReferences;
	}

	public Map<String, String> getExportPathMapping() {
		return exportPathMapping;
	}

}
