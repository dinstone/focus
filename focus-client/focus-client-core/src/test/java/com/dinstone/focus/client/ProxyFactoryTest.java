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
package com.dinstone.focus.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;

public class ProxyFactoryTest {

    @Test
    public void test() throws Exception {
        // HelloService h = proxyFactory();

        HelloService h = jdkProxy();

        for (Method m : HelloService.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " : " + m.getDeclaringClass().getName());
            // m.setAccessible(true);
            //
            // long s = System.currentTimeMillis();
            //
            // for (int i = 0; i < 100000000; i++) {
            // m.invoke(h, "hhhhhh");
            // }
            //
            // long e = System.currentTimeMillis();
            // System.out.println((e - s) + "ms");

        }

        h.hi(null);
        h.hi("hahh");
        h.hi("");

        h.say();

        h.vfn();

        DefaultHelloService dh = new DefaultHelloService();
        long s = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            dh.hi("hhhhhh");
        }
        long e = System.currentTimeMillis();
        System.out.println((e - s) + "ms");
    }

    @Test
    public void test2() throws Exception {
        HelloService h = proxyFactory();

        h.hi(null);
        h.hi("hahh");
        h.hi("");

        h.say();

        h.vfn();

    }

    private HelloService proxyFactory() {
        ConsumerServiceConfig serviceConfig = new ConsumerServiceConfig();
        serviceConfig.parseMethod(HelloService.class.getDeclaredMethods());
        serviceConfig.setHandler(new Handler() {

            @Override
            public CompletableFuture<Object> handle(Invocation invocation) {
                return CompletableFuture.completedFuture(null);
            }
        });
        return new JdkProxyFactory().create(HelloService.class, serviceConfig);
    }

    private HelloService jdkProxy() {
        return (HelloService) Proxy.newProxyInstance(HelloService.class.getClassLoader(),
                new Class[] { HelloService.class }, new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {

                        System.out.println(m.getName() + ", ParameterTypes = " + m.getParameterTypes().length
                                + ", returnType = " + m.getReturnType());

                        System.out.println("args.length  = " + (args == null ? -1 : args.length) + ", args = "
                                + (args == null ? "" : "'" + args[0] + "'"));

                        return null;
                    }
                });
    }

    public interface HelloService {
        String hi(String name);

        String say();

        void vfn();
    }

    public static class DefaultHelloService implements HelloService {

        @Override
        public String hi(String name) {
            return null;
        }

        @Override
        public String say() {
            return null;
        }

        @Override
        public void vfn() {
        }
    }

}
