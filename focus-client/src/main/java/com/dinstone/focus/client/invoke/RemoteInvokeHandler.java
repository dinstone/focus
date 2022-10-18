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
package com.dinstone.focus.client.invoke;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.transport.ConnectionFactory;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.photon.Connection;
import com.dinstone.photon.message.Request;

public class RemoteInvokeHandler implements InvokeHandler {

    private static final AtomicInteger IDGENER = new AtomicInteger();

    private ServiceConfig serviceConfig;

    private ProtocolCodec protocolCodec;

    private ConnectionFactory connectionFactory;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, ProtocolCodec protocolCodec,
            ConnectionFactory connectionFactory) {
        this.serviceConfig = serviceConfig;
        this.protocolCodec = protocolCodec;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public CompletableFuture<Reply> invoke(Call call) throws Exception {
        ServiceInstance instance = call.context().get("service.instance");
        if (instance == null) {
            throw new ConnectException("can't find a service instance to connect");
        }

        call.attach().put("provider.endpoint", instance.getEndpointCode());
        call.attach().put(Serializer.HEADER_KEY, serviceConfig.getSerializerId());
        call.attach().put(Compressor.HEADER_KEY, serviceConfig.getCompressorId());

        MethodConfig methodConfig = serviceConfig.getMethodConfig(call.getMethod());
        // process request
        Request request = protocolCodec.encode(call, methodConfig.getParamType());
        request.setMsgId(IDGENER.incrementAndGet());

        Connection connection = connectionFactory.create(instance.getServiceAddress());
        return connection.sendRequest(request).thenApply((response) -> {
            return protocolCodec.decode(response, methodConfig.getReturnType());
        });
    }

}
