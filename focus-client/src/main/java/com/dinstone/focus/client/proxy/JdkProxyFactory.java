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

import com.dinstone.focus.client.GenericService;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class JdkProxyFactory implements ProxyFactory {

    private static class ProxyInvocationHandler implements InvocationHandler {

        private ServiceConfig serviceConfig;
        private InvokeHandler invokeHandler;

        public ProxyInvocationHandler(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
            this.serviceConfig = serviceConfig;
            this.invokeHandler = invokeHandler;
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

            Call call = new Call(methodName, parameter);
            call.setGroup(serviceConfig.getGroup());
            call.setService(serviceConfig.getService());
            call.setTimeout(serviceConfig.getTimeout());

            Reply reply = invokeHandler.invoke(call);

            Object data = reply.getData();
            if (data instanceof Exception) {
                throw (Exception) data;
            } else {
                return data;
            }
        }

    }

    private <T> T createProxy(Class<T> sic, ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        if (!sic.isInterface()) {
            throw new IllegalArgumentException(sic.getName() + " is not interface");
        }

        ProxyInvocationHandler handler = new ProxyInvocationHandler(serviceConfig, invokeHandler);
        return sic.cast(Proxy.newProxyInstance(sic.getClassLoader(), new Class[] { sic }, handler));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T create(Class<T> sic, ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        if (sic.equals(GenericService.class)) {
            return (T) new GenericServiceProxy(serviceConfig, invokeHandler);
        }
        return createProxy(sic, serviceConfig, invokeHandler);
    }

}
