/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.ApplicationOptions;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.serialize.Serializer;

/**
 * service level config
 *
 * @author dinstone
 *
 */
public abstract class ServiceConfig {

    protected static final String DEFAULT_SERVICE_GROUP = "default";

    protected Map<String, MethodConfig> methodConfigs = new ConcurrentHashMap<>();

    protected String identity;

    protected String namespace;

    protected String application;

    protected String service;

    protected int timeout;

    protected int retry;

    protected Object target;

    protected Handler handler;

    protected Serializer serializer;

    protected Compressor compressor;

    protected int compressThreshold;

    public ServiceConfig(String identity, String namespace) {
        this.identity = identity;

        if (namespace != null && namespace.length() > 0) {
            this.namespace = namespace;
        } else {
            this.namespace = ApplicationOptions.DEFAULT_NAMESPACE;
        }
    }

    public String getIdentity() {
        return identity;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getService() {
        return service;
    }

    public String getApplication() {
        return application;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getRetry() {
        return retry;
    }

    public Object getTarget() {
        return target;
    }

    public Handler getHandler() {
        return handler;
    }

    public void addMethodConfig(MethodConfig mc) {
        methodConfigs.putIfAbsent(mc.getMethodName(), mc);
    }

    public MethodConfig getMethodConfig(String methodName) {
        return methodConfigs.get(methodName);
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public Compressor getCompressor() {
        return compressor;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    protected MethodConfig createMethodConfig(Method method) {
        // public check
        if (!Modifier.isPublic(method.getModifiers())) {
            return null;
        }
        // static check
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
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

        return new MethodConfig(method, paramType);
    }

}