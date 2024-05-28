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
package com.dinstone.focus.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.invoke.DefaultInvocation;
import com.dinstone.focus.invoke.Handler;

class ProxyHandler implements InvocationHandler {

    public static final String HASH_CODE = "hashCode";
    public static final String TO_STRING = "toString";
    public static final String EQUALS = "equals";

    private final ServiceConfig serviceConfig;
    private final Handler invokeHandler;

    public ProxyHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.invokeHandler = serviceConfig.getHandler();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
        case HASH_CODE:
            return System.identityHashCode(proxy);
        case EQUALS:
            return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
        case TO_STRING:
            return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
        }

        Object parameter = null;
        if (args != null && args.length > 0) {
            parameter = args[0];
        }

        MethodConfig methodConfig = serviceConfig.lookup(methodName);

        DefaultInvocation invocation = new DefaultInvocation(serviceConfig.getService(), methodConfig.getMethodName(),
                parameter);
        invocation.setTimeout(methodConfig.getTimeoutMillis());
        invocation.setProvider(serviceConfig.getProvider());
        invocation.setConsumer(serviceConfig.getConsumer());
        invocation.setServiceConfig(serviceConfig);
        invocation.setMethodConfig(methodConfig);

        // invoke
        CompletableFuture<Object> future = invokeHandler.handle(invocation);

        // reply handle
        if (methodConfig.isAsyncInvoke()) {
            return future;
        } else {
            try {
                return future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof FocusException) {
                    throw cause;
                } else {
                    if (cause instanceof TimeoutException) {
                        throw new InvokeException(ErrorCode.TIMEOUT_ERROR, cause);
                    } else {
                        throw new InvokeException(ErrorCode.INVOKE_ERROR, cause);
                    }
                }
            }
        }
    }

}