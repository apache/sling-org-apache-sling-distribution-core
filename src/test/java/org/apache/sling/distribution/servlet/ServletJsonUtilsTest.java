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
package org.apache.sling.distribution.servlet;

import java.util.Collections;
import java.util.Map;

import javax.json.JsonObject;

import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServletJsonUtilsTest {

    @Test
    public void testBuildBodyWithMessageAndNoProperties() throws Exception {
        String message1 = "message #1";
        JsonObject body = ServletJsonUtils.buildBody(message1, null);
        assertEquals(message1, body.getString("message"));
    }

    @Test
    public void testBuildBodyWithMessageAndProperties() throws Exception {
        String message2 = "message #2";
        String k1 = "keyOne", v1 = "value #1";
        Map<String,String> props = Collections.singletonMap(k1, v1);
        JsonObject body = ServletJsonUtils.buildBody(message2, props);
        assertEquals(message2, body.getString("message"));
        assertEquals(v1, body.getString(k1));
    }

    @Test
    public void testBuildBodyWithDistributionResponseContainingMessage() throws Exception {
        String message1 = "message #1";
        DistributionRequestState state = DistributionRequestState.ACCEPTED;
        DistributionResponse response = new SimpleDistributionResponse(state, message1);
        JsonObject body = ServletJsonUtils.buildBody(response);
        assertEquals(message1, body.getString("message"));
    }

    @Test
    public void testBuildBodyWithDistributionResponseContainingNoMessage() throws Exception {
        DistributionRequestState state = DistributionRequestState.ACCEPTED;
        DistributionResponse response = new SimpleDistributionResponse(state, null);
        JsonObject body = ServletJsonUtils.buildBody(response);
        assertFalse(body.containsKey("message"));
    }

}