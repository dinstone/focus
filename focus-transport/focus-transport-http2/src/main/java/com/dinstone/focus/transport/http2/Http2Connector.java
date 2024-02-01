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

    private final Http2ChannelFactory commonChannelFactory;
    private final Http2ChannelFactory secureChannelFactory;

    public Http2Connector(Http2ConnectOptions connectOptions) {
        Http2ConnectOptions commonOptions = new Http2ConnectOptions(connectOptions);
        commonOptions.setEnableSsl(false);
        commonChannelFactory = new Http2ChannelFactory(commonOptions);

        Http2ConnectOptions secureOptions = new Http2ConnectOptions(connectOptions);
        secureOptions.setEnableSsl(true);
        secureChannelFactory = new Http2ChannelFactory(secureOptions);
    }

    @Override
    public CompletableFuture<Object> send(Invocation invocation, ServiceInstance instance) {
        // create connection
        if (instance.isEnableSsl()) {
            Http2Channel http2Channel = secureChannelFactory.create(instance.getInstanceAddress());
            invocation.context().setRemoteAddress(http2Channel.getRemoteAddress());
            invocation.context().setLocalAddress(http2Channel.getLocalAddress());
            return http2Channel.send(invocation);
        } else {
            Http2Channel http2Channel = commonChannelFactory.create(instance.getInstanceAddress());
            invocation.context().setRemoteAddress(http2Channel.getRemoteAddress());
            invocation.context().setLocalAddress(http2Channel.getLocalAddress());
            return http2Channel.send(invocation);
        }
    }

    @Override
    public void destroy() {
        commonChannelFactory.destroy();
        secureChannelFactory.destroy();
    }

}
