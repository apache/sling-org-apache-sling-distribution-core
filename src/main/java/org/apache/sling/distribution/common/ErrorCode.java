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
package org.apache.sling.distribution.common;

/**
 * This class defines specific system error codes which allow for easier propagation of exception
 * data throughout the system, and act as a single point of change for the documentation code,
 * description and the HTTP status that an exception should generate.
 */
public enum ErrorCode {
    /**
     * A generic unexpected error in the system.
     */
    UNKNOWN_ERROR("unknown", "An unexpected error has occurred.", 500),
    /**
     * Error that happens when the client tries to distribute a package that exceeds the
     * configured package size limit.
     */
    DISTRIBUTION_PACKAGE_SIZE_LIMIT_EXCEEDED("package_limit_exceeded", "Package has exceeded the" +
            " limit bytes size.", 400);

    private final String code;
    private final String description;
    private final int httpStatusCode;

    ErrorCode(String code, String description, int httpStatusCode) {
        this.code = code;
        this.description = description;
        this.httpStatusCode = httpStatusCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}
