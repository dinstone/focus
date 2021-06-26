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
package com.dinstone.focus.proxy;

import java.lang.reflect.Method;

import org.junit.Test;

import com.dinstone.focus.filter.Filter;

public class ServiceProxyFactoryTest {

    @Test
    public void test() throws Exception {
        ProxyFactory factory = new JdkProxyFactory();
        Object sp = factory.create(null, Hello.class);

        for (Method m : Hello.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " : " + m.getDeclaringClass().getName());
        }
    }

    public static interface Hello extends Filter {
        String hi(String name);
    }

}
