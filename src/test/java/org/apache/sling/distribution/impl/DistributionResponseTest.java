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

package org.apache.sling.distribution.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.DistributionResponseInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DistributionResponseTest {
    @Test
    public void nullDistributionResponse() {
        DistributionResponse res1 = new SimpleDistributionResponse(DistributionRequestState.DISTRIBUTED, "success", null);

        assertNotNull(res1.getDistributionInfo());
        assertEquals("", res1.getDistributionInfo().getId());

        DistributionResponse res2 = new SimpleDistributionResponse(DistributionRequestState.ACCEPTED, "success", null);
        List<DistributionResponse> responses = new ArrayList<>();
        responses.add(res1);
        responses.add(res2);
        CompositeDistributionResponse compositeResponse = new CompositeDistributionResponse(responses, 2, 0, 0);
        assertNotNull(compositeResponse.getDistributionInfo());
        assertEquals(list("", "").toString(), compositeResponse.getDistributionInfo().getId());
    }
    
    @Test
    public void emptyDistributionResponse() {
        DistributionResponse res1 = new SimpleDistributionResponse(DistributionRequestState.DISTRIBUTED, "success");

        assertNotNull(res1.getDistributionInfo());
        assertEquals("", res1.getDistributionInfo().getId());
        
        DistributionResponse res2 = new SimpleDistributionResponse(DistributionRequestState.ACCEPTED, "success");
        List<DistributionResponse> responses = new ArrayList<>();
        responses.add(res1);
        responses.add(res2);
        CompositeDistributionResponse compositeResponse = new CompositeDistributionResponse(responses, 2, 0, 0);
        assertNotNull(compositeResponse.getDistributionInfo());
        assertEquals(list("", "").toString(), compositeResponse.getDistributionInfo().getId());
    }

    @Test
    public void nonEmptyDistributionResponse() {
        DistributionResponse res1 = new SimpleDistributionResponse(DistributionRequestState.DISTRIBUTED, "success", 
            new DistributionResponseInfo() {
                @NotNull @Override public String getId() {
                    return "res1";
                }
        });

        assertNotNull(res1.getDistributionInfo());
        assertEquals("res1", res1.getDistributionInfo().getId());

        DistributionResponse res2 = new SimpleDistributionResponse(DistributionRequestState.ACCEPTED, "success",
            new DistributionResponseInfo() {
                @NotNull @Override public String getId() {
                    return "res2";
                }
            });
        List<DistributionResponse> responses = new ArrayList<>();
        responses.add(res1);
        responses.add(res2);
        CompositeDistributionResponse compositeResponse = new CompositeDistributionResponse(responses, 2, 0, 0);
        assertNotNull(compositeResponse.getDistributionInfo());
        assertEquals(list("res1", "res2").toString(), compositeResponse.getDistributionInfo().getId());
    }
    
    private List<String> list(String str1, String str2) {
        List<String> expectedIds = new ArrayList<>();
        expectedIds.add(str1);
        expectedIds.add(str2);
        return expectedIds;
    }
}
