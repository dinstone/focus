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
package com.dinstone.focus.config;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.invoke.InvokeHandler;

/**
 * service config
 * 
 * @author dinstone
 *
 */
public class ServiceConfig {

    private Map<String, MethodInfo> methodCaches = new ConcurrentHashMap<String, MethodInfo>();

    private String endpoint;

    private String service;

    private String group;

    private int timeout;

    private Object target;

    private Object proxy;

    private String serializerId;

    private String compressorId;

    private InvokeHandler handler;

    public ServiceConfig() {
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object getProxy() {
        return proxy;
    }

    public void setProxy(Object proxy) {
        this.proxy = proxy;
    }

    public InvokeHandler getHandler() {
        return handler;
    }

    public void setHandler(InvokeHandler handler) {
        this.handler = handler;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getSerializerId() {
        return serializerId;
    }

    public void setSerializerId(String serializerId) {
        this.serializerId = serializerId;
    }

    public String getCompressorId() {
        return compressorId;
    }

    public void setCompressorId(String compressorId) {
        this.compressorId = compressorId;
    }

    public void parseMethodInfos(Method[] methods) {
        for (Method method : methods) {
            // overload check
            if (methodCaches.containsKey(method.getName())) {
                throw new IllegalArgumentException("unsupported method overload : " + method);
            }
            // parameter check
            Class<?> paramType = null;
            if (method.getParameterTypes().length > 1) {
                throw new IllegalArgumentException("call only support one parameter");
            } else if (method.getParameterTypes().length == 1) {
                paramType = method.getParameterTypes()[0];
            }

            addMethodInfo(new MethodInfo(method, paramType));
        }
    }

    public Collection<MethodInfo> methodInfos() {
        return methodCaches.values();
    }

    public void addMethodInfo(MethodInfo mi) {
        methodCaches.putIfAbsent(mi.getMethodName(), mi);
    }

    public MethodInfo getMethodInfo(String methodName) {
        return methodCaches.get(methodName);
    }

}