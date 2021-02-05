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
