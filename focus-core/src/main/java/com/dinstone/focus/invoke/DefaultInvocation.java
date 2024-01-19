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
package com.dinstone.focus.invoke;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;

public class DefaultInvocation implements Invocation, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String EMPTY_VALUE = "";
    private final Map<String, String> attributes = new HashMap<>();

    protected final String service;

    protected final String method;

    protected final Object parameter;

    /**
     * source application identity
     */
    protected String consumer;
    /**
     * target application identity
     */
    protected String provider;

    protected int timeout;

    // ===================================
    // content filed
    // ===================================

    private InetSocketAddress remoteAddress;

    private InetSocketAddress localAddress;

    private ServiceConfig serviceConfig;

    private MethodConfig methodConfig;

    public DefaultInvocation(String service, String method, Object parameter) {
        this.service = service;
        this.method = method;
        this.parameter = parameter;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public Object getParameter() {
        return parameter;
    }

    @Override
    public String getEndpoint() {
        return service + "/" + method;
    }

    @Override
    public String getConsumer() {
        if (consumer == null) {
            return EMPTY_VALUE;
        }
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    @Override
    public String getProvider() {
        if (provider == null) {
            return EMPTY_VALUE;
        }
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public MethodConfig getMethodConfig() {
        return methodConfig;
    }

    public void setMethodConfig(MethodConfig methodConfig) {
        this.methodConfig = methodConfig;
    }
}
