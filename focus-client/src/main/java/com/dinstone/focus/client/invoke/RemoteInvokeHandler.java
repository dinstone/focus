/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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
package com.dinstone.focus.client.invoke;

import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.connection.Connection;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;

public class RemoteInvokeHandler implements InvokeHandler {

    private static final AtomicInteger IDGENER = new AtomicInteger();

    private ProtocolCodec codec;

    public RemoteInvokeHandler(ServiceConfig serviceConfig) {
        codec = CodecManager.codec(serviceConfig.getCodecId());
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        Connection connection = InvokeContext.getContext().get("service.connection");
        if (connection == null) {
            throw new RuntimeException("can't find a service connection");
        }

        Request request = codec.encode(call);
        request.setMsgId(IDGENER.incrementAndGet());

        // remote call
        Response response = connection.sync(request);
        if (response.getStatus() == Status.SUCCESS) {
            return codec.decode(response);
        } else {
            throw ExceptionCodec.decode(response.getContent());
        }
    }

}
