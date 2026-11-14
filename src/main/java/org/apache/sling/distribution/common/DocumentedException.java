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
 * A type of exception that contains additional error metadata that link to external
 * documentation and allows for distributed tracing, through specific ERROR_CODES.
 */
public class DocumentedException extends Exception {
    private ErrorCode errorCode;

    public DocumentedException(Throwable e, ErrorCode errorCode) {
        super(e);
        this.errorCode = errorCode;
    }

    public DocumentedException(Throwable e) {
        super(e);
        errorCodeNotDefined();
    }

    public DocumentedException(String string, ErrorCode errorCode) {
        super(string);
        this.errorCode = errorCode;
    }

    public DocumentedException(String string) {
        super(string);
        errorCodeNotDefined();
    }

    public DocumentedException(String string, Throwable cause, ErrorCode errorCode) {
        super(string, cause);
        this.errorCode = errorCode;
    }

    public DocumentedException(String string, Throwable cause) {
        super(string, cause);
        errorCodeNotDefined();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private void errorCodeNotDefined() {
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }
}
