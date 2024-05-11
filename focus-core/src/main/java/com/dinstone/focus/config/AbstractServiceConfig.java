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
package com.dinstone.focus.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.serialize.Serializer;

public abstract class AbstractServiceConfig implements ServiceConfig {

    protected Map<String, MethodConfig> methodConfigs = new ConcurrentHashMap<>();

    protected String provider;

    protected String consumer;

    protected String service;

    protected Handler handler;

    protected Serializer serializer;

    protected Compressor compressor;

    protected int compressThreshold;

    private Map<String, String> metadata;

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getConsumer() {
        return consumer;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    public void addMethodConfig(MethodConfig mc) {
        methodConfigs.putIfAbsent(mc.getMethodName(), mc);
    }

    @Override
    public MethodConfig lookup(String methodName) {
        return methodConfigs.get(methodName);
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Compressor getCompressor() {
        return compressor;
    }

    @Override
    public int getCompressThreshold() {
        return compressThreshold;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    protected <T> T parse(Method method, BiFunction<Method, Class<?>, T> builder) {
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
        return builder.apply(method, paramType);
    }

}
