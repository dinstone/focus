/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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
package com.dinstone.focus.codec.photon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Response;

public class PhotonProtocolCodecTest {

    @Test
    public void test() {
        PhotonProtocolCodec errorCodec = new PhotonProtocolCodec(null, null, 0);

        Reply er = new Reply(new InvokeException(999, "unknow exception"));

        Response response = errorCodec.encode(er, null);

        Reply ar = errorCodec.decode(response, null);

        assertEquals(er.getData().getClass(), ar.getData().getClass());

        assertEquals(((InvokeException) er.getData()).getCode(), ((InvokeException) ar.getData()).getCode());
    }

}
