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
package com.dinstone.focus.tracing;

import com.dinstone.focus.protocol.Call;

import brave.rpc.RpcClientRequest;

public class FocusClientRequest extends RpcClientRequest {

    private Call call;

    public FocusClientRequest(Call call) {
        this.call = call;
    }

    @Override
    public String method() {
        return call.getMethod();
    }

    @Override
    public String service() {
        return call.getService();
    }

    @Override
    public Object unwrap() {
        return call;
    }

    @Override
    protected void propagationField(String keyName, String value) {
        call.attach().put(keyName, value);
    }

    // @Override
    // public boolean parseRemoteIpAndPort(Span span) {
    // InetSocketAddress address = InvokeContext.getContext().get("connection.remote");
    // if (address == null) {
    // return false;
    // }
    // return span.remoteIpAndPort(Platform.get().getHostString(address), address.getPort());
    // }

}
