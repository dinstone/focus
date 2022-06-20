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
package com.dinstone.focus.client;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.clutch.RegistryFactory;
import com.dinstone.clutch.ServiceDiscovery;
import com.dinstone.focus.binding.DefaultReferenceBinding;
import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.invoke.ConsumeInvokeHandler;
import com.dinstone.focus.client.invoke.LocationInvokeHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.client.transport.ConnectionFactory;
import com.dinstone.focus.codec.CodecFactory;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.ServiceImporter;
import com.dinstone.focus.filter.FilterChainHandler;
import com.dinstone.focus.invoke.InvokeHandler;

public class Client implements ServiceImporter {

    private ClientOptions clientOptions;

    private ServiceDiscovery serviceDiscovery;

    private ReferenceBinding referenceBinding;

    private ConnectionFactory connectionFactory;

    private ProxyFactory proxyFactory;

    public Client(ClientOptions clientOption) {
        checkAndInit(clientOption);
    }

    private void checkAndInit(ClientOptions clientOptions) {
        if (clientOptions == null) {
            throw new IllegalArgumentException("clientOptions is null");
        }
        this.clientOptions = clientOptions;

        // check transport provider
        this.connectionFactory = new ConnectionFactory(clientOptions);

        // load and create rpc message codec
        ServiceLoader<CodecFactory> cfLoader = ServiceLoader.load(CodecFactory.class);
        for (CodecFactory codecFactory : cfLoader) {
            CodecManager.regist(codecFactory.createCodec());
        }

        // load and create registry
        RegistryConfig registryConfig = clientOptions.getRegistryConfig();
        if (registryConfig != null) {
            ServiceLoader<RegistryFactory> serviceLoader = ServiceLoader.load(RegistryFactory.class);
            for (RegistryFactory registryFactory : serviceLoader) {
                if (registryFactory.canApply(registryConfig)) {
                    this.serviceDiscovery = registryFactory.createServiceDiscovery(registryConfig);
                    break;
                }
            }
        }

        this.referenceBinding = new DefaultReferenceBinding(serviceDiscovery);
        this.proxyFactory = new JdkProxyFactory();
    }

    @Override
    public void destroy() {
        connectionFactory.destroy();
        referenceBinding.destroy();

        if (serviceDiscovery != null) {
            serviceDiscovery.destroy();
        }
    }

    @Override
    public <T> T importing(Class<T> sic) {
        return importing(sic, "");
    }

    @Override
    public <T> T importing(Class<T> sic, String group) {
        return importing(sic, group, clientOptions.getDefaultTimeout());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T importing(Class<T> sic, String group, int timeout) {
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = clientOptions.getDefaultTimeout();
        }

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup(group);
        serviceConfig.setTimeout(timeout);
        serviceConfig.setService(sic.getName());
        serviceConfig.setMethods(sic.getDeclaredMethods());

        serviceConfig.setAppCode(clientOptions.getAppCode());
        serviceConfig.setAppName(clientOptions.getAppName());
        serviceConfig.setCodecId(clientOptions.getCodecId());

        InvokeHandler invokeHandler = createInvokeHandler(serviceConfig);
        Object proxy = proxyFactory.create(sic, invokeHandler);

        serviceConfig.setHandler(invokeHandler);
        serviceConfig.setProxy(proxy);

        referenceBinding.lookup(serviceConfig.getService());
        referenceBinding.binding(serviceConfig);

        return (T) proxy;
    }

    private InvokeHandler createInvokeHandler(ServiceConfig serviceConfig) {
        RemoteInvokeHandler remote = new RemoteInvokeHandler(serviceConfig);
        FilterChainHandler chain = createFilterChain(serviceConfig, remote);
        List<InetSocketAddress> addresses = clientOptions.getServiceAddresses();
        InvokeHandler invokeHandler = new LocationInvokeHandler(chain, referenceBinding, connectionFactory, addresses);
        return new ConsumeInvokeHandler(serviceConfig, invokeHandler);
    }

    private FilterChainHandler createFilterChain(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        FilterChainHandler chain = new FilterChainHandler(serviceConfig, invokeHandler);
        return chain.addFilter(clientOptions.getFilters());
    }

}
