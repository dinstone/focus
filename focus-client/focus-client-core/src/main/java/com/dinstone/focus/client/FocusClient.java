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

import com.dinstone.focus.client.binding.ReferenceBinding;
import com.dinstone.focus.client.invoke.ConsumerInvokeHandler;
import com.dinstone.focus.client.invoke.LocationInvokeHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.client.transport.ConnectBootstrap;
import com.dinstone.focus.client.transport.ConnectBootstrapFactory;
import com.dinstone.focus.client.transport.ConnectOptions;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;

public class FocusClient implements ServiceConsumer {

    private ClientOptions clientOptions;

    private ConnectBootstrap connectBootstrap;

    private ReferenceBinding referenceBinding;

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

        ConnectOptions connectOptions = clientOptions.getConnectOptions();
        if (connectOptions == null) {
            throw new FocusException("please set transport options for connect bootstrap");
        }
        ServiceLoader<ConnectBootstrapFactory> cfLoader = ServiceLoader.load(ConnectBootstrapFactory.class);
        for (ConnectBootstrapFactory bootstrapFactory : cfLoader) {
            if (bootstrapFactory.appliable(connectOptions)) {
                this.connectBootstrap = bootstrapFactory.create(connectOptions);
            }
        }
        if (this.connectBootstrap == null) {
            throw new FocusException("can't find a connect bootstrap implement");
        }

        // init reference binding
        ClutchOptions clutchOptions = clientOptions.getClutchOptions();
        InetSocketAddress consumerAddress = clientOptions.getConsumerAddress();
        this.referenceBinding = new ReferenceBinding(clutchOptions, consumerAddress);
    }

    @Override
    public void destroy() {
        connectBootstrap.destroy();
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

        // handle
        if (serviceClass.equals(GenericService.class)) {
            importOptions.setSerializerType(EndpointOptions.DEFAULT_SERIALIZER_Type);
        } else {
            // parse method info and set invoke config
            serviceConfig.parseMethod(serviceClass.getMethods());
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

        // init service protocol codec
        protocolCodec(serviceConfig, clientOptions, importOptions);

        // create invoke handler chain
        serviceConfig.setHandler(createInvokeHandler(serviceConfig));

        referenceBinding.lookup(serviceConfig.getService());
        referenceBinding.binding(serviceConfig);

        return proxyFactory.create(serviceClass, serviceConfig);
    }

    private void protocolCodec(ServiceConfig serviceConfig, ClientOptions clientOptions, ImportOptions importOptions) {
        Serializer serializer = SerializerFactory.lookup(importOptions.getSerializerType());
        if (serializer == null) {
            serializer = SerializerFactory.lookup(clientOptions.getSerializerType());
        }
        if (serializer == null) {
            throw new IllegalArgumentException("please configure the correct serializer");
        }
        serviceConfig.setSerializer(serializer);

        Compressor compressor = CompressorFactory.lookup(importOptions.getCompressorType());
        if (compressor == null) {
            compressor = CompressorFactory.lookup(clientOptions.getCompressorType());
        }
        int compressThreshold = importOptions.getCompressThreshold();
        if (compressThreshold <= 0) {
            compressThreshold = clientOptions.getCompressThreshold();
        }
        serviceConfig.setCompressor(compressor);
        serviceConfig.setCompressThreshold(compressThreshold);
    }

    private InvokeHandler createInvokeHandler(ServiceConfig serviceConfig) {
        RemoteInvokeHandler remote = new RemoteInvokeHandler(serviceConfig, connectBootstrap);
        InvokeHandler locate = new LocationInvokeHandler(serviceConfig, remote, referenceBinding, clientOptions);
        return new ConsumerInvokeHandler(serviceConfig, locate).addFilter(clientOptions.getFilters());
    }

}
