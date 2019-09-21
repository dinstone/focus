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

import com.dinstone.focus.client.transport.Connection;
import com.dinstone.focus.client.transport.ConnectionManager;
import com.dinstone.focus.invoker.Invocation;
import com.dinstone.focus.invoker.InvocationHandler;
import com.dinstone.focus.protocol.Call;

public class RemoteInvocationHandler implements InvocationHandler {

    private ConnectionManager connectionManager;

    public RemoteInvocationHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Object handle(Invocation invocation) throws Throwable {
        String service = invocation.getService();
        String group = invocation.getGroup();
        int timeout = invocation.getTimeout();
        String method = invocation.getMethod();
        Object[] args = invocation.getParams();

        Connection connection = connectionManager.getConnection(invocation.getServiceAddress());
        return connection.invoke(new Call(service, group, timeout, method, args, invocation.getParamTypes()));
    }

}
