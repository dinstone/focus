/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.rpc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.dinstone.focus.protocol.Attach;

public class AttachTest {

    @Test
    public void testCodec01() throws IOException {
        Attach attach = null;
        byte[] bs = Attach.encode(attach);
        assertEquals(4, bs.length);

        Attach a = Attach.decode(bs);
        assertEquals(a, null);

        attach = new Attach();
        bs = Attach.encode(attach);
        assertEquals(4, bs.length);

        a = Attach.decode(bs);
        assertEquals(a, null);

        attach.put(null, "null value");
        attach.put("", "empty value");
        attach.put("key", null);
        attach.put("ekey", "");
        bs = Attach.encode(attach);
        assertEquals(64, bs.length);

        a = Attach.decode(bs);
        assertEquals("null value", a.get(null));
        assertEquals("empty value", a.get(""));
        assertEquals(null, a.get("key"));
        assertEquals("", a.get("ekey"));
    }
}
