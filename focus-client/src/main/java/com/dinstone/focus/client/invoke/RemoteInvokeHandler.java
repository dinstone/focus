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

package com.dinstone.focus.client.invoke;

import java.net.InetSocketAddress;

import com.dinstone.focus.client.transport.Connection;
import com.dinstone.focus.client.transport.ConnectionManager;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class RemoteInvokeHandler implements InvokeHandler {

    private ConnectionManager connectionManager;

    public RemoteInvokeHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        InetSocketAddress address = InvokeContext.getContext().get("service.address");
        Connection connection = connectionManager.getConnection(address);
        return connection.invoke(call);
    }

}
