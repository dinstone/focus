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
import com.dinstone.focus.rpc.Call;

public class ServiceProxyFactory {

    private InvokeHandler invokeHandler;

    public ServiceProxyFactory(InvokeHandler invokeHandler) {
        this.invokeHandler = invokeHandler;
    }

    public <T> ServiceProxy<T> create(Class<T> serviceInterface, String group, int timeout, T serviceInstance)
            throws Exception {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException(serviceInterface.getName() + " is not interface");
        }
        if (serviceInstance != null && !serviceInterface.isInstance(serviceInstance)) {
            throw new IllegalArgumentException(
                    serviceInstance + " is not an instance of " + serviceInterface.getName());
        }

        ServiceProxy<T> serviceProxy = new ServiceProxy<>(serviceInterface, group, timeout);
        ProxyInvocationHandler<T> handler = new ProxyInvocationHandler<>(serviceProxy);
        T proxyInstance = serviceInterface.cast(
                Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] { serviceInterface }, handler));

        serviceProxy.setProxy(proxyInstance);
        serviceProxy.setTarget(serviceInstance);
        return serviceProxy;
    }

    private class ProxyInvocationHandler<T> implements InvocationHandler {

        private ServiceProxy<T> serviceProxy;

        public ProxyInvocationHandler(ServiceProxy<T> serviceProxy) {
            this.serviceProxy = serviceProxy;
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
                return serviceProxy.getService();
            }

            Call call = new Call(serviceProxy.getService().getName(), serviceProxy.getGroup(),
                    serviceProxy.getTimeout(), methodName, args, method.getParameterTypes());
            return invokeHandler.invoke(call).getData();
        }

    }
}
