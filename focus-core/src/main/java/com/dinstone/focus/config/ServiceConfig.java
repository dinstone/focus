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
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.invoke.InvokeHandler;

/**
 * service config
 *
 * @author dinstone
 *
 */
public class ServiceConfig {

    private static final String DEFAULT_SERVICE_GROUP = "";

    private Map<String, MethodConfig> methodConfigs = new ConcurrentHashMap<>();

    private String endpoint;

    private String service;

    private String group;

    private int timeout;

    private int retry;

    private Object target;

    private InvokeHandler handler;

    private ProtocolCodec protocolCodec;

    public ServiceConfig() {
        this.group = DEFAULT_SERVICE_GROUP;
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
        if (group != null && group.length() > 0) {
            this.group = group;
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
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

    public void parseMethod(Method... methods) {
        for (Method method : methods) {
            // public check
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            // static check
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // overload check
            if (methodConfigs.containsKey(method.getName())) {
                throw new IllegalStateException("method overload unsupported : " + method);
            }
            // parameter check
            Class<?> paramType = null;
            if (method.getParameterTypes().length > 1) {
                throw new IllegalArgumentException("only support one parameter : " + method);
            } else if (method.getParameterTypes().length == 1) {
                paramType = method.getParameterTypes()[0];
            }

            MethodConfig methodConfig = new MethodConfig(method, paramType);
            methodConfig.setInvokeTimeout(timeout);
            methodConfig.setInvokeRetry(retry);
            addMethodConfig(methodConfig);
        }
    }

    public Collection<MethodConfig> methodConfigs() {
        return methodConfigs.values();
    }

    public void addMethodConfig(MethodConfig mc) {
        methodConfigs.putIfAbsent(mc.getMethodName(), mc);
    }

    public MethodConfig getMethodConfig(String methodName) {
        return methodConfigs.get(methodName);
    }

    public void setProtocolCodec(ProtocolCodec protocolCodec) {
        this.protocolCodec = protocolCodec;
    }

    public ProtocolCodec getProtocolCodec() {
        return protocolCodec;
    }

}