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

import com.dinstone.focus.client.binding.ReferenceBinding;
import com.dinstone.focus.client.invoke.ConsumerInvokeHandler;
import com.dinstone.focus.client.invoke.LocationInvokeHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.client.transport.ConnectionFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.codec.photon.PhotonProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.GenericService;
import com.dinstone.focus.endpoint.ServiceConsumer;
import com.dinstone.focus.invoke.InvokeHandler;

public class FocusClient implements ServiceConsumer {

    private ClientOptions clientOptions;

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

        this.proxyFactory = new JdkProxyFactory();

        // init ProtocolCodec
        this.protocolCodec = new PhotonProtocolCodec(clientOptions);

        // check transport provider
        this.connectionFactory = new ConnectionFactory(clientOptions);

        // init reference binding
        ClutchOptions clutchOptions = clientOptions.getClutchOptions();
        InetSocketAddress consumerAddress = clientOptions.getConsumerAddress();
        this.referenceBinding = new ReferenceBinding(clutchOptions, consumerAddress);
    }

    @Override
    public void destroy() {
        connectionFactory.destroy();
        referenceBinding.destroy();
    }

    @Override
    public <T> T importing(Class<T> sic) {
        return importing(sic, "", clientOptions.getDefaultTimeout());
    }

    @Override
    public <T> T importing(Class<T> sic, String group, int timeout) {
        return importing(sic, sic.getName(), group, timeout);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T importing(Class<T> sic, String service, String group, int timeout) {
        if (sic.equals(GenericService.class)) {
            return (T) genericService(service, group, timeout);
        } else {
            return specialService(sic, service, group, timeout);
        }
    }

    private <T> T specialService(Class<T> sic, String service, String group, int timeout) {
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
        serviceConfig.parseMethod(sic.getDeclaredMethods());

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

    private GenericService genericService(String service, String group, int timeout) {
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
        InvokeHandler locate = new LocationInvokeHandler(serviceConfig, remote, referenceBinding, clientOptions);
        return new ConsumerInvokeHandler(serviceConfig, locate).addFilter(clientOptions.getFilters());
    }

}
