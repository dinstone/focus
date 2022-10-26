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
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
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
        return importing(sic, "", 3000);
    }

    @Override
    public <T> T importing(Class<T> sic, String group, int timeout) {
        return importing(sic, new ImportOptions(sic.getName(), group).setTimeout(timeout));
    }

    @Override
    public GenericService generic(String service, String group, int timeout) {
        return importing(GenericService.class, new ImportOptions(service, group).setTimeout(timeout));
    }

    @Override
    public <T> T importing(Class<T> serviceClass, ImportOptions importOptions) {
        String service = importOptions.getService();
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("serivce name is null");
        }

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup(importOptions.getGroup());
        serviceConfig.setService(importOptions.getService());
        serviceConfig.setEndpoint(clientOptions.getEndpoint());
        serviceConfig.setTimeout(importOptions.getTimeout());
        serviceConfig.setRetry(importOptions.getRetry());
        serviceConfig.setSerializerId(importOptions.getSerializerId());
        serviceConfig.setCompressorId(importOptions.getCompressorId());

        if (serviceClass.equals(GenericService.class)) {
            serviceConfig.setSerializerId(ImportOptions.DEFAULT_SERIALIZER_ID);
        } else {
            // parse method info and set invoke config
            serviceConfig.parseMethod(serviceClass.getDeclaredMethods());
            List<InvokeOptions> iol = importOptions.getInvokeOptions();
            if (iol != null) {
                for (InvokeOptions io : iol) {
                    MethodConfig mc = serviceConfig.getMethodConfig(io.getMethodName());
                    if (mc != null) {
                        mc.setInvokeTimeout(io.getInvokeTimeout());
                        mc.setInvokeRetry(io.getInvokeRetry());
                    }
                }
            }
        }

        serviceConfig.setHandler(createInvokeHandler(serviceConfig));
        T proxy = proxyFactory.create(serviceClass, serviceConfig);

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
