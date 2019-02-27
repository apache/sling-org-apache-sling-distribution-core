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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import static java.io.File.createTempFile;
import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static java.lang.String.format;
import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertEquals;

public class ByteBufferBackedInputStreamTest {

    private byte[] DATA = new byte[]{0x00, 0x01, 0x02};

    @Test
    public void testRead() throws Exception {
        testRead(new ByteBufferBackedInputStream(
                wrap(subData(0, 3)),
                write(subData(3, 0))));
        testRead(new ByteBufferBackedInputStream(
                wrap(subData(0, 1)),
                write(subData(1, 2))));
        testRead(new ByteBufferBackedInputStream(
                wrap(subData(0, 2)),
                write(subData(2, 1))));
        testRead(new ByteBufferBackedInputStream(
                wrap(subData(0, 0)),
                write(subData(0, 3))));
    }

    private void testRead(ByteBufferBackedInputStream bbisData)
            throws IOException {
        assertEquals(DATA[0], bbisData.read());
        assertEquals(DATA[1], bbisData.read());
        assertEquals(DATA[2], bbisData.read());
        assertEquals(-1, bbisData.read());
    }

    private byte[] subData(int offset, int length) {
        byte[] sub = new byte[length];
        arraycopy(DATA, offset, sub, 0, length);
        return sub;
    }

    private File write(byte[] data) throws IOException {
        File file = createTempFile("output", format(".%s.bin", currentTimeMillis()));
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(data);
            fos.flush();
        } finally {
            fos.close();
        }
        return file;
    }
}