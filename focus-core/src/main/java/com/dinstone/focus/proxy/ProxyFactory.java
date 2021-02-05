package com.dinstone.focus.proxy;

import com.dinstone.focus.invoke.InvokeHandler;

public interface ProxyFactory {

    Object create(InvokeHandler invokeHandler, Class<?> sic);

}
