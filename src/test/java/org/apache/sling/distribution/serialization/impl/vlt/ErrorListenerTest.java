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

import org.junit.Before;
import org.junit.Test;

import static org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener.Mode.PATHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ErrorListenerTest {

    private ErrorListener errorListener;

    private static final String path = "/some/path";

    private static final Exception exception = new Exception();

    @Before
    public void before() {
        errorListener = new ErrorListener();
    }

    @Test
    public void testDefaultErrorNotNull() {
        assertNotNull(errorListener.getLastError());
    }

    @Test
    public void testError() {
        errorListener.onError(PATHS, path, exception);
        assertEquals(exception, errorListener.getLastError().getCause());
        assertTrue(errorListener.getLastError().getMessage().contains(path));
    }

    @Test
    public void testLastError() {
        errorListener.onError(PATHS, path, new Exception());
        errorListener.onError(PATHS, path, exception);
        assertEquals(exception, errorListener.getLastError().getCause());
    }
}