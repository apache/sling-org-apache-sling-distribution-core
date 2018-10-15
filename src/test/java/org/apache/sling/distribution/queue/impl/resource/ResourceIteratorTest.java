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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ResourceIteratorTest {

    static final String FOLDER_TYPE = ResourceQueueUtils.RESOURCE_FOLDER;
    static final String ITEM_TYPE = "nt:unstructured";

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void testSetup() {
        MockHelper helper = MockHelper.create(context.resourceResolver());

        helper.resource("/root").p("prop", "value")
                .resource("2018").p("sling:resourceType", FOLDER_TYPE)
                .resource("10").p("sling:resourceType", FOLDER_TYPE)
                .resource("15").p("sling:resourceType", FOLDER_TYPE)
                .resource("11").p("sling:resourceType", FOLDER_TYPE)
                .resource("35").p("sling:resourceType", FOLDER_TYPE)
                .resource("item1").p("sling:resourceType", ITEM_TYPE)
                .resource("../item2").p("sling:resourceType", ITEM_TYPE)
                .resource("../../36").p("sling:resourceType", FOLDER_TYPE)
                .resource("item3").p("sling:resourceType", ITEM_TYPE);

        helper.resource("/root2").p("prop", "value")
                .resource("2018").p("sling:resourceType", FOLDER_TYPE)
                .resource("10").p("sling:resourceType", FOLDER_TYPE)
                .resource("15").p("sling:resourceType", FOLDER_TYPE)
                .resource("11").p("sling:resourceType", FOLDER_TYPE)
                .resource("35").p("sling:resourceType", FOLDER_TYPE)
                .resource("../36").p("sling:resourceType", FOLDER_TYPE)
                .resource("item3").p("sling:resourceType", ITEM_TYPE);

        try {
            helper.commit();
        } catch (PersistenceException e) {

        }

    }

    @Test
    public void testEmptyRoot() {
        Resource root =  context.resourceResolver().getResource("/root");

        ResourceIterator it =  new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, false, false);


        test(it, new String[0]);
    }

    @Test
    public void testFullPath() {
        Resource root =  context.resourceResolver().getResource("/root");

        ResourceIterator it =  new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, true, true);

        test(it, new String[] {
                "/root/2018/10/15/11/35/item1",
                "/root/2018/10/15/11/35/item2",
                "/root/2018/10/15/11/35",
                "/root/2018/10/15/11/36/item3",
                "/root/2018/10/15/11/36",
                "/root/2018/10/15/11",
                "/root/2018/10/15",
                "/root/2018/10",
                "/root/2018"
        });
    }

    @Test
    public void testFullPath2() {
        Resource root =  context.resourceResolver().getResource("/root2");

        ResourceIterator it =  new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, true, true);

        test(it, new String[] {
                "/root2/2018/10/15/11/35",
                "/root2/2018/10/15/11/36/item3",
                "/root2/2018/10/15/11/36",
                "/root2/2018/10/15/11",
                "/root2/2018/10/15",
                "/root2/2018/10",
                "/root2/2018"
        });
    }

    @Test
    public void testFullPathNoFolder() {
        Resource root =  context.resourceResolver().getResource("/root");

        ResourceIterator it =  new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, false, true);


        test(it, new String[] {
                "/root/2018/10/15/11/35/item1",
                "/root/2018/10/15/11/35/item2",
                "/root/2018/10/15/11/36/item3",
        });
    }

    @Test
    public void testFullPathNoLeafs() {
        Resource root =  context.resourceResolver().getResource("/root");

        ResourceIterator it =  new ResourceIterator(root, ResourceQueueUtils.RESOURCE_FOLDER, true, false);

        test(it, new String[] {
                "/root/2018/10/15/11/35",
                "/root/2018/10/15/11/36",
                "/root/2018/10/15/11",
                "/root/2018/10/15",
                "/root/2018/10",
                "/root/2018"
        });
    }

    public void test(ResourceIterator it, String[] paths) {
        List<String> expected = Arrays.asList(paths);

        assertEquals(expected, toPaths(it));
    }


    List<String> toPaths(ResourceIterator it) {
        List<String> paths = new ArrayList<String>();
        while (it.hasNext()) {
            paths.add(it.next().getPath());
        }
        return paths;
    }
}
