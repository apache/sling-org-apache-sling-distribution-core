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
package org.apache.sling.distribution.queue.impl.resource;

import org.junit.Test;

import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceQueueUtilsTest {

    @Test
    public void testTimePath() throws Exception {


        assertEquals("2018/01/01/00/00",
                ResourceQueueUtils.getTimePath(new GregorianCalendar(2018, 0, 1, 0, 0)));


        assertEquals("2018/12/01/00/00",
                ResourceQueueUtils.getTimePath(new GregorianCalendar(2018, 11, 1, 0, 0)));

        assertEquals("2018/12/31/23/59",
                ResourceQueueUtils.getTimePath(new GregorianCalendar(2018, 11, 31, 23, 59)));
    }


    @Test
    public void testIsSafeToDelete() throws Exception {

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2017"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2019"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/01"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/03"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/14"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/16"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/02"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/03"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/04"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/03/08"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/03/09"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/03/10"));

        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/02/15/03/10"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/02/15/03/09", "2018/03/16/04/09"));




        // new year
        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2017"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2019"));


        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/11"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2019/01"));


        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/30"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/31"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2019/01/00"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/31/22"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/31/23"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2019/01/01/00"));

        assertTrue(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/31/23/58"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2018/12/31/23/59"));
        assertFalse(ResourceQueueUtils.isSafeToDelete("2018/12/31/23/59", "2019/01/01/00/00"));

    }
}
