/*
 * Copyright (C) 2013~2017 dinstone<dinstone@163.com>
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

package com.dinstone.focus.client.invoker;

import java.net.InetSocketAddress;

import com.dinstone.focus.client.transport.Connection;
import com.dinstone.focus.client.transport.ConnectionManager;
import com.dinstone.focus.invoker.InvocationContext;
import com.dinstone.focus.invoker.InvocationHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class RemoteInvocationHandler implements InvocationHandler {

    private ConnectionManager connectionManager;

    public RemoteInvocationHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Reply handle(Call call) throws Throwable {
        InetSocketAddress address = InvocationContext.get().getServiceAddress();
        Connection connection = connectionManager.getConnection(address);
        return connection.invoke(call);
    }

}
