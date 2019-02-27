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
package org.apache.sling.distribution.util.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class DigestUtilsTest {

    private Set<String> DIGEST_ALGORITHM = new HashSet<String>(asList("md2","md5","sha1"));

    private byte[] DATA = new byte[]{0x01, 0x02, 0x03};

    private String DATA_MD5_DIGEST = "d41d8cd98f00b204e9800998ecf8427e";

    @Test
    public void testOpenDigestInputStream() throws Exception {
        for (String digest : DIGEST_ALGORITHM) {
            DigestUtils.openDigestInputStream(mock(InputStream.class), digest);
        }
    }

    @Test
    public void testOpenDigestOutputStream() throws Exception {
        for (String digest : DIGEST_ALGORITHM) {
            DigestUtils.openDigestOutputStream(mock(OutputStream.class), digest);
        }
    }

    @Test
    public void testReadInputStreamDigestMessage() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(DATA);
        DigestInputStream dis = DigestUtils.openDigestInputStream(bis, "md5");
        String digest = DigestUtils.readDigestMessage(dis);
        assertEquals(DATA_MD5_DIGEST, digest);
    }

    @Test
    public void testReadOutputStreamDigestMessage() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(DATA.length);
        bos.write(DATA);
        bos.flush();
        DigestOutputStream dos = DigestUtils.openDigestOutputStream(bos, "md5");
        String digest = DigestUtils.readDigestMessage(dos);
        assertEquals(DATA_MD5_DIGEST, digest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedDigestMessage() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(DATA.length);
        bos.write(DATA);
        bos.flush();
        DigestOutputStream dos = DigestUtils.openDigestOutputStream(bos, "unsupported");
    }

}