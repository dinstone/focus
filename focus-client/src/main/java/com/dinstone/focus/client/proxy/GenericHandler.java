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
package com.dinstone.focus.client.proxy;

import java.util.concurrent.Future;

import com.dinstone.focus.config.MethodInfo;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.GenericService;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

class GenericHandler implements GenericService {

    private final ServiceConfig serviceConfig;
    private final InvokeHandler invokeHandler;

    public GenericHandler(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        this.serviceConfig = serviceConfig;
        this.invokeHandler = invokeHandler;
    }

    @Override
    public <R, P> R sync(Class<R> returnType, String methodName, P parameter) throws Exception {
        return async(returnType, methodName, parameter).get();
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <R, P> Future<R> async(Class<R> returnType, String methodName, P parameter) throws Exception {
        if (serviceConfig.getMethodInfo(methodName) == null) {
            serviceConfig
                    .addMethodInfo(new MethodInfo(methodName, parameter.getClass(), returnType).setAsyncMethod(true));
        }

        Call call = new Call(methodName, parameter);
        call.setGroup(serviceConfig.getGroup());
        call.setService(serviceConfig.getService());
        call.setTimeout(serviceConfig.getTimeout());

        Reply reply = invokeHandler.invoke(call);

        Object data = reply.getData();
        if (data instanceof Exception) {
            throw (Exception) data;
        } else {
            return (Future<R>) data;
        }
    }
}