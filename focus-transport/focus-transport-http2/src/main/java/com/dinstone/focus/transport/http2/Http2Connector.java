/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.transport.http2;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.transport.Connector;

public class Http2Connector implements Connector {

    private final Http2ChannelFactory channelFactory;

    public Http2Connector(Http2ConnectOptions connectOptions) {
        channelFactory = new Http2ChannelFactory(connectOptions);
    }

    @Override
    public CompletableFuture<Object> send(Invocation invocation, ServiceInstance instance) {
        // create connection
        Http2Channel http2Channel = channelFactory.create(instance.getInstanceAddress());
        return http2Channel.send(invocation);
    }

    @Override
    public void destroy() {
        channelFactory.destroy();
    }

}
