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

import java.util.ServiceLoader;

import com.dinstone.focus.binding.DefaultReferenceBinding;
import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.invoke.ConsumeInvokeHandler;
import com.dinstone.focus.client.invoke.LocationInvokeHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.client.transport.ConnectionFactory;
import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.codec.photon.PhotonProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.ServiceConsumer;
import com.dinstone.focus.filter.FilterChainHandler;
import com.dinstone.focus.invoke.InvokeHandler;

public class FocusClient implements ServiceConsumer {

    private ClientOptions clientOptions;

    private ServiceDiscovery serviceDiscovery;

    private ReferenceBinding referenceBinding;

    private ConnectionFactory connectionFactory;

    private ProtocolCodec protocolCodec;

    private ProxyFactory proxyFactory;

    public FocusClient(ClientOptions clientOption) {
        checkAndInit(clientOption);
    }

    private void checkAndInit(ClientOptions clientOptions) {
        if (clientOptions == null) {
            throw new IllegalArgumentException("clientOptions is null");
        }
        this.clientOptions = clientOptions;

        // check transport provider
        this.connectionFactory = new ConnectionFactory(clientOptions);

        // init ProtocolCodec
        this.protocolCodec = new PhotonProtocolCodec(clientOptions);

        // load and create registry
        ClutchOptions clutchOptions = clientOptions.getClutchOptions();
        if (clutchOptions != null) {
            ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
            for (ClutchFactory clutchFactory : serviceLoader) {
                if (clutchFactory.appliable(clutchOptions)) {
                    this.serviceDiscovery = clutchFactory.createServiceDiscovery(clutchOptions);
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
        return importing(sic, "", clientOptions.getDefaultTimeout());
    }

    @Override
    public <T> T importing(Class<T> sic, String group, int timeout) {
        return importing(sic, sic.getName(), group, timeout);
    }

    @Override
    public <T> T importing(Class<T> sic, String service, String group, int timeout) {
        if (service == null || service.isEmpty()) {
            service = sic.getName();
        }
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = clientOptions.getDefaultTimeout();
        }

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup(group);
        serviceConfig.setTimeout(timeout);
        serviceConfig.setService(service);
        serviceConfig.parseMethodInfos(sic.getDeclaredMethods());

        serviceConfig.setEndpoint(clientOptions.getEndpoint());
        serviceConfig.setSerializerId(clientOptions.getSerializerId());
        serviceConfig.setCompressorId(clientOptions.getCompressorId());

        InvokeHandler invokeHandler = createInvokeHandler(serviceConfig);
        T proxy = proxyFactory.create(sic, serviceConfig, invokeHandler);

        serviceConfig.setHandler(invokeHandler);
        serviceConfig.setProxy(proxy);

        referenceBinding.lookup(serviceConfig.getService());
        referenceBinding.binding(serviceConfig);

        return proxy;
    }

    public GenericService genericService(String service, String group, int timeout) {
        if (service == null) {
            throw new IllegalArgumentException("serivce name is null");
        }
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = clientOptions.getDefaultTimeout();
        }

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup(group);
        serviceConfig.setTimeout(timeout);
        serviceConfig.setService(service);

        serviceConfig.setEndpoint(clientOptions.getEndpoint());
        serviceConfig.setSerializerId(ClientOptions.DEFAULT_SERIALIZER_ID);
        serviceConfig.setCompressorId(clientOptions.getCompressorId());

        InvokeHandler invokeHandler = createInvokeHandler(serviceConfig);
        GenericService proxy = proxyFactory.create(GenericService.class, serviceConfig, invokeHandler);
        serviceConfig.setHandler(invokeHandler);
        serviceConfig.setTarget(proxy);

        referenceBinding.lookup(serviceConfig.getService());
        referenceBinding.binding(serviceConfig);

        return proxy;
    }

    private InvokeHandler createInvokeHandler(ServiceConfig serviceConfig) {
        RemoteInvokeHandler remote = new RemoteInvokeHandler(serviceConfig, protocolCodec, connectionFactory);
        FilterChainHandler chain = createFilterChain(serviceConfig, remote);
        InvokeHandler invokeHandler = new LocationInvokeHandler(serviceConfig, chain, referenceBinding, clientOptions);
        return new ConsumeInvokeHandler(serviceConfig, invokeHandler);
    }

    private FilterChainHandler createFilterChain(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        FilterChainHandler chain = new FilterChainHandler(serviceConfig, invokeHandler);
        return chain.addFilter(clientOptions.getFilters());
    }

}
