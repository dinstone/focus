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
package com.dinstone.focus.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;

import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class ProxyFactoryTest {

    @Test
    public void test() throws Exception {
        // HelloService h = proxyFactory();

        HelloService h = jdkProxy();

        for (Method m : HelloService.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " : " + m.getDeclaringClass().getName());
            m.setAccessible(true);

            long s = System.currentTimeMillis();

            for (int i = 0; i < 100000000; i++) {
                m.invoke(h, "hhhhhh");
            }

            long e = System.currentTimeMillis();
            System.out.println((e - s) + "ms");
        }

        DefaultHelloService dh = new DefaultHelloService();
        long s = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            dh.hi("hhhhhh");
        }
        long e = System.currentTimeMillis();
        System.out.println((e - s) + "ms");
    }

    private HelloService proxyFactory() {
        return new JdkProxyFactory().create(HelloService.class, new InvokeHandler() {

            private Reply reply = new Reply();

            @Override
            public Reply invoke(Call call) throws Exception {
                return reply;
            }
        });
    }

    private HelloService jdkProxy() {
        return (HelloService) Proxy.newProxyInstance(HelloService.class.getClassLoader(),
                new Class[] { HelloService.class }, new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });
    }

    public static interface HelloService extends Filter {
        String hi(String name);
    }

    public static class DefaultHelloService implements HelloService {

        @Override
        public Reply invoke(FilterContext next, Call call) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String hi(String name) {
            return null;
        }
    }

}
