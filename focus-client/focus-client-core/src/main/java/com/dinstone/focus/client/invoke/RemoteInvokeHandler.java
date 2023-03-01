/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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

import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.transport.Connector;

public class RemoteInvokeHandler implements InvokeHandler {

    private ServiceConfig serviceConfig;

    private Connector connector;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, Connector connector) {
        this.serviceConfig = serviceConfig;
        this.connector = connector;
    }

    @Override
    public CompletableFuture<Reply> invoke(Call call) throws Exception {
        ServiceInstance serviceInstance = InvokeContext.getContext().get(InvokeContext.SERVICE_INSTANCE_KEY);
        if (serviceInstance == null) {
            throw new ConnectException("can't find a service instance to connect");
        }

        call.attach().put(Attach.PROVIDER_KEY, serviceInstance.getEndpointCode());

        return connector.send(call, serviceConfig, serviceInstance);
    }

}
