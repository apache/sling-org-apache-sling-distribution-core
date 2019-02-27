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
package org.apache.sling.distribution.log.impl;

import java.util.List;

import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.junit.Test;

import static org.apache.sling.distribution.log.impl.DefaultDistributionLog.LogLevel;
import static org.apache.sling.distribution.log.impl.DefaultDistributionLog.LogLevel.DEBUG;
import static org.apache.sling.distribution.log.impl.DefaultDistributionLog.LogLevel.ERROR;
import static org.apache.sling.distribution.log.impl.DefaultDistributionLog.LogLevel.INFO;
import static org.apache.sling.distribution.log.impl.DefaultDistributionLog.LogLevel.WARN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultDistributionLogTest {

    private DistributionComponentKind COMPONENT_KIND = DistributionComponentKind.AGENT;

    private String NAME = "test-log";

    private Class<DistributionAgent> CLASS = DistributionAgent.class;

    @Test
    public void testEmptyLines() throws Exception {
        assertNotNull(buildLog(DEBUG).getLines());
    }

    @Test
    public void testLogName() throws Exception {
        assertEquals(NAME, buildLog(DEBUG).getName());
    }

    @Test
    public void testLogKind() throws Exception {
        assertEquals(COMPONENT_KIND, buildLog(DEBUG).getKind());
    }

    @Test
    public void testOutputFormatting() throws Exception {
        DefaultDistributionLog log = buildLog(DEBUG);
        log.debug("Format{}", "ted");
        List<String> lines = log.getLines();
        assertNotNull(lines);
        assertEquals(1, lines.size());
        String line = lines.get(0);
        assertTrue(line.contains("Formatted"));
    }

    @Test
    public void testLevelInOutput() throws Exception {
        DefaultDistributionLog log = buildLog(DEBUG);
        log.debug("Anything");
        String line = log.getLines().get(0);
        assertTrue(line.contains(DEBUG.name()));
    }

    @Test
    public void testDebugLevel() throws Exception {
        DefaultDistributionLog debug = buildLog(DEBUG);
        debug.debug("anything");
        assertTrue(debug.getLines().get(0).contains(DEBUG.name()));
        debug.info("in");
        debug.error("in");
        debug.warn("in");
        assertEquals(4, debug.getLines().size());
    }

    @Test
    public void testInfoLevel() throws Exception {
        DefaultDistributionLog info = buildLog(INFO);
        info.info("anything");
        assertTrue(info.getLines().get(0).contains(INFO.name()));
        info.debug("out");
        info.error("in");
        info.warn("in");
        assertEquals(3, info.getLines().size());
    }

    @Test
    public void testWarnLevel() throws Exception {
        DefaultDistributionLog warn = buildLog(WARN);
        warn.warn("anything");
        assertTrue(warn.getLines().get(0).contains(WARN.name()));
        warn.debug("out");
        warn.info("out");
        warn.error("in");
        assertEquals(2, warn.getLines().size());
    }

    @Test
    public void testErrorLevel() throws Exception {
        DefaultDistributionLog error = buildLog(ERROR);
        error.error("anything");
        assertTrue(error.getLines().get(0).contains(ERROR.name()));
        error.debug("out");
        error.info("out");
        error.warn("out");
        assertEquals(1, error.getLines().size());
    }

    @Test
    public void testMultiLine() throws Exception {
        DefaultDistributionLog log = buildLog(DEBUG);
        log.debug("this");
        log.debug("and");
        log.debug("that");
        assertEquals(3, log.getLines().size());
    }

    private DefaultDistributionLog buildLog(LogLevel level) {
        return new DefaultDistributionLog(COMPONENT_KIND, NAME, CLASS, level);
    }

}