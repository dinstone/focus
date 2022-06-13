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
package com.dinstone.focus.config;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.dinstone.focus.invoke.InvokeHandler;

/**
 * service config
 * 
 * @author dinstone
 *
 */
public class ServiceConfig {

    private Map<String, Method> methodCaches;

    private String appCode;

    private String appName;

    private String service;

    private String group;

    private int timeout;

    private String codecId;

    private Method[] methods;

    private Object target;

    private Object proxy;

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

    public Method[] getMethods() {
        return methods;
    }

    public void setMethods(Method[] methods) {
        if (methodCaches == null) {
            methodCaches = new HashMap<String, Method>();
        }
        for (Method method : methods) {
            methodCaches.put(method.getName(), method);
        }
        this.methods = methods;
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

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Method findMethod(String methodName) {
        return methodCaches.get(methodName);
    }

    public boolean hasMethod(String methodName) {
        return methodCaches.containsKey(methodName);
    }

    public String getCodecId() {
        return codecId;
    }

    public void setCodecId(String codecId) {
        this.codecId = codecId;
    }

}