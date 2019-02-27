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
package org.apache.sling.distribution.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestUtilsTest {

    private DistributionRequestType REQ_TYPE = DistributionRequestType.ADD;

    private String[] P_PATHS = {"/some/path"};

    private String[] EMPTY_PATHS = {};

    @Test
    public void testFromServletRequest() throws Exception {
        DistributionRequest dr = RequestUtils.fromServletRequest(buildServletRequest(REQ_TYPE.toString(), P_PATHS, "true"));
        assertEquals(REQ_TYPE, dr.getRequestType());
        assertArrayEquals(P_PATHS, dr.getPaths());
    }

    @Test
    public void testFromServletRequestEmptyPath() throws Exception {
        DistributionRequest dr = RequestUtils.fromServletRequest(buildServletRequest(REQ_TYPE.toString(), null, "true"));
        assertEquals(REQ_TYPE, dr.getRequestType());
        assertArrayEquals(EMPTY_PATHS, dr.getPaths());
    }

    private HttpServletRequest buildServletRequest(String action, String[] paths, String deep) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("action"))
                .thenReturn(action);
        when(request.getParameterValues("path"))
                .thenReturn(paths);
        when(request.getParameter("deep"))
                .thenReturn(deep);
        return request;

    }
}