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
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.exception.ExchangeException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class JdkProxyFactory implements ProxyFactory {

    private static class ProxyInvocationHandler implements InvocationHandler {

        private InvokeHandler invokeHandler;
        private Class<?> serviceClazz;

        public ProxyInvocationHandler(Class<?> serviceClazz, InvokeHandler invokeHandler) {
            this.invokeHandler = invokeHandler;
            this.serviceClazz = serviceClazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("hashCode")) {
                return Integer.valueOf(System.identityHashCode(proxy));
            } else if (methodName.equals("equals")) {
                return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
            } else if (methodName.equals("toString")) {
                return serviceClazz.getName() + '@' + Integer.toHexString(proxy.hashCode());
            }

            Object parameter = null;
            if (args != null && args.length > 0) {
                parameter = args[0];
            }

            try {
                Call call = new Call(methodName, parameter);
                Reply reply = invokeHandler.invoke(call);

                Object data = reply.getData();
                if (data instanceof Exception) {
                    throw (Exception) data;
                } else {
                    return data;
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw e;
                }
                if (e instanceof TimeoutException) {
                    throw new ExchangeException(188, "invoke timeout", e);
                }
                throw new ExchangeException(189, "wrapped invoke exception", e);
            }
        }

    }

    @Override
    public <T> T create(Class<T> sic, InvokeHandler invokeHandler) {
        if (!sic.isInterface()) {
            throw new IllegalArgumentException(sic.getName() + " is not interface");
        }

        ProxyInvocationHandler handler = new ProxyInvocationHandler(sic, invokeHandler);
        return sic.cast(Proxy.newProxyInstance(sic.getClassLoader(), new Class[] { sic }, handler));
    }
}
