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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.DistributionBaseIT;
import org.apache.sling.distribution.queue.spi.DistributionQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
public class ResourceQueueIT extends DistributionBaseIT {

    static final int ITERATIONS = 1000;
    static final String QUEUE_NAME = "default";

    @Inject
    protected Distributor distributor;


    @Test
    public void testExecute() throws LoginException, DistributionException {

        DistributionAgent agent = getAgent(AGENT_RESOURCE_QUEUE);

        assertNotNull(agent);

        ResourceResolver resourceResolver =  resolverFactory.getAdministrativeResourceResolver(null);

        DistributionResponse response = agent.execute(resourceResolver,
                new SimpleDistributionRequest(DistributionRequestType.ADD, "/content" ));
        assertTrue(response.isSuccessful());

        DistributionQueue queue = agent.getQueue(QUEUE_NAME);

        assertEquals(1, queue.getStatus().getItemsCount());

        clear(queue);

    }


    @Test
    public void testFifo() {
        DistributionAgent agent = getAgent(AGENT_RESOURCE_QUEUE);

        DistributionQueue queue = agent.getQueue(QUEUE_NAME);

        queue.add(new DistributionQueueItem("packageId", 10, new HashMap<String, Object>()));

        assertEquals(1, queue.getStatus().getItemsCount());

        clear(queue);
    }

    @Test
    public void testConcurrentFifo () throws InterruptedException {
        final DistributionAgent agent = getAgent(AGENT_RESOURCE_QUEUE);
        final DistributionQueue queue = agent.getQueue(QUEUE_NAME);

        Producer p1 = new Producer(queue, "p1");
        Producer p2 = new Producer(queue, "p2");
        Producer p3 = new Producer(queue, "p3");
        Producer p4 = new Producer(queue, "p4");

        Map<String, Producer> producerMap = new HashMap<String, Producer>();
        producerMap.put("p1", p1);
        producerMap.put("p2", p2);
        producerMap.put("p3", p3);
        producerMap.put("p4", p4);

        Consumer c1 = new Consumer(queue, "c1", new String[] { "p1", "p2" });
        Consumer c2 = new Consumer(queue, "c2", new String[] { "p3", "p4" });

        Map<String, Consumer> consumerMap = new HashMap<String, Consumer>();
        consumerMap.put("c1", c1);
        consumerMap.put("c2", c2);

        List<Thread> threads = new ArrayList<Thread>();
        for (Producer p : producerMap.values()) {
            threads.add(new Thread(p));
        }
        for (Consumer c : consumerMap.values()) {
            threads.add(new Thread(c));
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join(600 * 1000);
        }

        for (Producer p : producerMap.values()) {
            assertEquals(ITERATIONS, p.created.size());
        }

        for (Consumer c : consumerMap.values()) {
            for (String source : c.sources) {
                Producer p = producerMap.get(source);
                assertEquals(p.created, c.removedBySource.get(source));
            }
        }

        assertEquals(0, queue.getStatus().getItemsCount());

        clear(queue);
    }

    void clear(DistributionQueue queue) {
        for (DistributionQueueEntry entry : queue.getItems(0, -1)) {
            queue.remove(entry.getId());
        }
    }

    class Producer implements Runnable {
        private final DistributionQueue queue;
        private final String name;
        final List<String> created = Collections.synchronizedList(new ArrayList<String>());

        Producer(DistributionQueue queue, String name) {
            this.queue = queue;
            this.name = name;
        }


        public void run() {
            for(int i=0; i<ITERATIONS; i++) {

                if (i % 1000 == 0) {
                    System.out.println("Producer " + name + " " + i);
                }

                final Map<String, Object> props = new HashMap<String, Object>();
                final String packageId = UUID.randomUUID().toString();
                final long size = 10;

                props.put("auuid", UUID.randomUUID().toString());
                props.put("no", i);
                props.put("source", name);

                final DistributionQueueItem item = new DistributionQueueItem(packageId, size, props);

                final DistributionQueueEntry entry = queue.add(item);

                created.add(entry.getId());
            }
        }
    }

    class Consumer implements Runnable {
        private final DistributionQueue queue;
        private final String name;
        private final String[] sources;

        final List<String> removed = new ArrayList<String>();

        final Map<String, List<String>> removedBySource = new HashMap<String, List<String>>();


        Consumer(DistributionQueue queue, String name, String[] sources) {

            this.queue = queue;
            this.name = name;
            this.sources = sources;
            for (String source: sources) {
                removedBySource.put(source, new ArrayList<String>());
            }
        }

        public void run() {
            while (removed.size() < sources.length * ITERATIONS) {

                final DistributionQueueEntry entry = queue.getHead();

                if (entry != null) {

                    String source = (String) entry.getItem().get("source");

                    if (removedBySource.containsKey(source)) {
                        if (removed.size() % 1000 == 0) {
                            System.out.println("Consumer " + name + " " + removed.size());
                        }

                        final DistributionQueueEntry removedEntry = queue.remove(entry.getId());

                        assertEquals(removedEntry.getId(), entry.getId());

                        removed.add(entry.getId());
                        removedBySource.get(source).add(entry.getId());
                    }
                }
            }
        }
    }
}
