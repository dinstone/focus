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
package com.dinstone.focus.server;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;
import com.dinstone.focus.server.binding.DefaultImplementBinding;
import com.dinstone.focus.server.invoke.LocalInvokeHandler;
import com.dinstone.focus.server.invoke.ProviderInvokeHandler;
import com.dinstone.focus.transport.AcceptBootstrap;
import com.dinstone.focus.transport.AcceptBootstrapFactory;
import com.dinstone.focus.transport.AcceptOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class FocusServer implements ServiceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FocusServer.class);

    private ServerOptions serverOptions;

    private InetSocketAddress serviceAddress;

    private ImplementBinding implementBinding;

    private AcceptBootstrap acceptBootstrap;

    public FocusServer(ServerOptions serverOption) {
        checkAndInit(serverOption);
    }

    private void checkAndInit(ServerOptions serverOptions) {
        if (serverOptions == null) {
            throw new IllegalArgumentException("serverOption is null");
        }
        this.serverOptions = serverOptions;

        // check bind service address
        if (serverOptions.getServiceAddress() == null) {
            throw new RuntimeException("server not bind a service address");
        }
        this.serviceAddress = serverOptions.getServiceAddress();

        // load and create registry
        ClutchOptions clutchOptions = serverOptions.getClutchOptions();
        this.implementBinding = new DefaultImplementBinding(clutchOptions, serviceAddress);

        AcceptOptions acceptOptions = serverOptions.getAcceptOptions();
        if (acceptOptions == null) {
            throw new FocusException("please set transport options for accept bootstrap");
        }
        ServiceLoader<AcceptBootstrapFactory> cfLoader = ServiceLoader.load(AcceptBootstrapFactory.class);
        for (AcceptBootstrapFactory bootstrapFactory : cfLoader) {
            if (bootstrapFactory.appliable(acceptOptions)) {
                this.acceptBootstrap = bootstrapFactory.create(acceptOptions);
            }
        }
        if (this.acceptBootstrap == null) {
            throw new FocusException("can't find a accept bootstrap implement");
        }

        try {
            this.acceptBootstrap.bind(serviceAddress, implementBinding);
            LOG.info("focus server started, {}", serviceAddress);
        } catch (Exception e) {
            LOG.warn("focus server failure, {}", serviceAddress, e);
            throw new FocusException("start focus server error", e);
        }
    }

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance) {
        exporting(clazz, instance, new ExportOptions(clazz.getName()));
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance, String service, String group) {
        exporting(clazz, instance, new ExportOptions(service, group));
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance, ExportOptions exportOptions) {
        String service = exportOptions.getService();
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("serivce name is null");
        }

        try {
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setService(service);
            serviceConfig.setTarget(instance);
            serviceConfig.setGroup(exportOptions.getGroup());
            serviceConfig.setTimeout(exportOptions.getTimeout());
            serviceConfig.setEndpoint(serverOptions.getEndpoint());

            // create and set method configure
            serviceConfig.parseMethod(clazz.getDeclaredMethods());

            // create invoke handler chain
            serviceConfig.setHandler(createInvokeHandler(serviceConfig));

            // init service protocol codec
            protocolCodec(serviceConfig, serverOptions, exportOptions);

            implementBinding.binding(serviceConfig);
        } catch (Exception e) {
            throw new FocusException("export service error", e);
        }
    }

    private void protocolCodec(ServiceConfig serviceConfig, ServerOptions serverOptions, ExportOptions exportOptions) {
        Serializer serializer = SerializerFactory.lookup(exportOptions.getSerializerType());
        if (serializer == null) {
            serializer = SerializerFactory.lookup(serverOptions.getSerializerType());
        }
        if (serializer == null) {
            throw new IllegalArgumentException("please configure the correct serializer");
        }
        serviceConfig.setSerializer(serializer);

        Compressor compressor = CompressorFactory.lookup(exportOptions.getCompressorType());
        if (compressor == null) {
            compressor = CompressorFactory.lookup(serverOptions.getCompressorType());
        }
        int compressThreshold = exportOptions.getCompressThreshold();
        if (compressThreshold <= 0) {
            compressThreshold = serverOptions.getCompressThreshold();
        }
        serviceConfig.setCompressor(compressor);
        serviceConfig.setCompressThreshold(compressThreshold);
    }

    private InvokeHandler createInvokeHandler(ServiceConfig serviceConfig) {
        LocalInvokeHandler localHandler = new LocalInvokeHandler(serviceConfig);
        return new ProviderInvokeHandler(serviceConfig, localHandler).addFilter(serverOptions.getFilters());
    }

    @Override
    public void destroy() {
        if (implementBinding != null) {
            implementBinding.destroy();
        }
        if (acceptBootstrap != null) {
            acceptBootstrap.destroy();
        }

        LOG.info("focus server destroyed, {}", serviceAddress);
    }

}
