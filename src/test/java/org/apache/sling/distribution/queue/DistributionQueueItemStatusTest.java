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
package org.apache.sling.distribution.queue;

import java.util.Calendar;

import org.junit.Test;

import static org.apache.sling.distribution.queue.DistributionQueueItemState.ERROR;
import static org.apache.sling.distribution.queue.DistributionQueueItemState.QUEUED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class DistributionQueueItemStatusTest {

    @Test
    public void getNoErrorByDefault() {
        DistributionQueueItemStatus status = new DistributionQueueItemStatus(Calendar.getInstance(),
                QUEUED, 0, "queue-name");
        assertNull(status.getError());
    }

    @Test
    public void getErrorByDefault() {
        Throwable cause = mock(Throwable.class);
        DistributionQueueItemStatus status = new DistributionQueueItemStatus(Calendar.getInstance(),
                ERROR, 10, "queue-name", cause);
        assertNotNull(status.getError());
        assertEquals(cause, status.getError());
    }
}