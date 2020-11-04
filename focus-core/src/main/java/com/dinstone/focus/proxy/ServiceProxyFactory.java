/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
package com.dinstone.focus.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class ServiceProxyFactory {

    public <T> ServiceProxy<T> create(InvokeHandler invoker, Class<T> sic, String group, int timeout, T sio)
            throws Exception {
        if (!sic.isInterface()) {
            throw new IllegalArgumentException(sic.getName() + " is not interface");
        }
        if (sio != null && !sic.isInstance(sio)) {
            throw new IllegalArgumentException(sio + " is not an instance of " + sic.getName());
        }

        ServiceProxy<T> serviceProxy = new ServiceProxy<>(sic, group, timeout);
        ProxyInvocationHandler<T> handler = new ProxyInvocationHandler<>(serviceProxy, invoker);
        T pio = sic.cast(Proxy.newProxyInstance(sic.getClassLoader(), new Class[] { sic }, handler));

        serviceProxy.setProxy(pio);
        serviceProxy.setTarget(sio);
        serviceProxy.setInvokeHandler(invoker);
        return serviceProxy;
    }

    private static class ProxyInvocationHandler<T> implements InvocationHandler {

        private ServiceProxy<T> serviceProxy;
        private InvokeHandler invokeHandler;

        public ProxyInvocationHandler(ServiceProxy<T> serviceProxy, InvokeHandler invokeHandler) {
            this.serviceProxy = serviceProxy;
            this.invokeHandler = invokeHandler;
        }

        @Override
        public Object invoke(Object proxyObj, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            Object instance = serviceProxy.getProxy();
            if (methodName.equals("hashCode")) {
                return Integer.valueOf(System.identityHashCode(instance));
            } else if (methodName.equals("equals")) {
                return (instance == args[0] ? Boolean.TRUE : Boolean.FALSE);
            } else if (methodName.equals("toString")) {
                return instance.getClass().getName() + '@' + Integer.toHexString(instance.hashCode());
            } else if (methodName.equals("getClass")) {
                return serviceProxy.getClazz();
            }

            Call call = new Call(serviceProxy.getClazz().getName(), serviceProxy.getGroup(), serviceProxy.getTimeout(),
                    methodName, args, method.getParameterTypes());
            Reply reply = invokeHandler.invoke(call);
            if (reply.getData() instanceof Throwable) {
                throw (Throwable) reply.getData();
            }
            return reply.getData();
        }

    }
}
