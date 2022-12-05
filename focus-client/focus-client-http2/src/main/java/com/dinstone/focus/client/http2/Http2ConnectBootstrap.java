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
package com.dinstone.focus.client.http2;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.client.transport.ConnectBootstrap;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class Http2ConnectBootstrap implements ConnectBootstrap {

    private Http2ChannelFactory factory;

    public Http2ConnectBootstrap(Http2ConnectOptions connectOptions) {
        factory = new Http2ChannelFactory(connectOptions);
    }

    @Override
    public CompletableFuture<Reply> send(Call call, ServiceConfig serviceConfig, ServiceInstance instance)
            throws Exception {
        // create connection
        Http2Channel http2Channel = factory.create(instance.getServiceAddress());
        return http2Channel.send(call, serviceConfig);
    }

    @Override
    public void destroy() {
        factory.destroy();
    }

}
