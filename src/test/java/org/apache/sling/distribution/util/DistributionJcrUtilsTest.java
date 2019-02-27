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

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

public class DistributionJcrUtilsTest {

    @Test
    public void testIsSafeNonJackrabbitEvent() throws Exception {
        Event event = Mockito.mock(Event.class);
        assertFalse(DistributionJcrUtils.isSafe(event));
    }

    @Test
    public void testIsSafeNonJackrabbitExternalEvent() throws Exception {
        assertFalse(DistributionJcrUtils.isSafe(buildEvent(true, null)));
        assertFalse(DistributionJcrUtils.isSafe(buildEvent(true, "user-data")));
    }

    @Test
    public void testIsSafeNonJackrabbitNonExternalEvent() throws Exception {
        assertTrue(DistributionJcrUtils.isSafe(buildEvent(false, null)));
        assertTrue(DistributionJcrUtils.isSafe(buildEvent(false, "user-data")));
        assertFalse(DistributionJcrUtils.isSafe(buildEvent(false, "do.not.distribute")));
    }

    private JackrabbitEvent buildEvent(boolean external, String userData)
            throws RepositoryException {
        JackrabbitEvent event = Mockito.mock(JackrabbitEvent.class);
        when(event.isExternal()).thenReturn(external);
        when(event.getUserData()).thenReturn(userData);
        return event;
    }
}