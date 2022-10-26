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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

class SpecialHandler implements InvocationHandler {

    private ServiceConfig serviceConfig;
    private InvokeHandler invokeHandler;

    public SpecialHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.invokeHandler = serviceConfig.getHandler();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("hashCode")) {
            return Integer.valueOf(System.identityHashCode(proxy));
        } else if (methodName.equals("equals")) {
            return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
        } else if (methodName.equals("toString")) {
            return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
        }

        Object parameter = null;
        if (args != null && args.length > 0) {
            parameter = args[0];
        }

        MethodConfig methodConfig = serviceConfig.getMethodConfig(methodName);
        if (methodConfig == null) {
            methodConfig = new MethodConfig(method, parameter.getClass());
            methodConfig.setInvokeTimeout(serviceConfig.getTimeout());
            methodConfig.setInvokeRetry(serviceConfig.getRetry());
            serviceConfig.addMethodConfig(methodConfig);
        }

        Call call = new Call(methodName, parameter);
        call.setGroup(serviceConfig.getGroup());
        call.setService(serviceConfig.getService());
        call.setTimeout(serviceConfig.getTimeout());

        // invoke
        CompletableFuture<Reply> future = invokeHandler.invoke(call);

        // reply handle
        if (methodConfig.isAsyncInvoke()) {
            return future.thenApply(reply -> parseReply(reply));
        } else {
            return parseReply(future.get(call.getTimeout(), TimeUnit.MILLISECONDS));
        }

    }

    private Object parseReply(Reply reply) {
        Object data = reply.getData();
        if (data instanceof InvokeException) {
            throw (InvokeException) data;
        } else {
            return data;
        }
    }

}