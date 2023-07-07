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
package com.dinstone.focus.client;

import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.FocusOptions;
import com.dinstone.focus.client.config.ConsumerMethodConfig;
import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.client.invoke.ConsumerInvokeHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.locate.DefaultLocaterFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;
import com.dinstone.focus.transport.ConnectOptions;
import com.dinstone.focus.transport.Connector;
import com.dinstone.focus.transport.ConnectorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class FocusClient implements ServiceImporter {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClient.class);

    private ServiceLocater serivceLocater;

    private ClientOptions clientOptions;

    private Connector connector;

    public FocusClient(ClientOptions clientOption) {
        checkAndInit(clientOption);
    }

    private void checkAndInit(ClientOptions clientOptions) {
        if (clientOptions == null) {
            throw new IllegalArgumentException("clientOptions is null");
        }
        this.clientOptions = clientOptions;

        ConnectOptions connectOptions = clientOptions.getConnectOptions();
        if (connectOptions == null) {
            throw new FocusException("please set transport options for connector");
        }
        ServiceLoader<ConnectorFactory> cfLoader = ServiceLoader.load(ConnectorFactory.class);
        for (ConnectorFactory connectorFactory : cfLoader) {
            if (connectorFactory.appliable(connectOptions)) {
                this.connector = connectorFactory.create(connectOptions);
            }
        }
        if (this.connector == null) {
            throw new FocusException("can't find a connector implement");
        }

        // init router and load balancer
        LocaterFactory locaterFactory = clientOptions.getLocaterFactory();
        if (locaterFactory == null) {
            locaterFactory = new DefaultLocaterFactory(clientOptions);
        }
        this.serivceLocater = locaterFactory.createLocater();

        LOG.info("focus client created for [{}]", clientOptions.getApplication());
    }

    public void destroy() {
        connector.destroy();
        serivceLocater.destroy();
    }

    @Override
    public <T> T importing(Class<T> sic) {
        return importing(sic, new ImportOptions(sic.getName()));
    }

    @Override
    public <T> T importing(Class<T> sic, String application) {
        return importing(sic, new ImportOptions(application, sic.getName()));
    }

    @Override
    public GenericService generic(String application, String service) {
        return importing(GenericService.class, new ImportOptions(application, service));
    }

    @Override
    public <T> T importing(Class<T> serviceClass, ImportOptions importOptions) {
        String service = importOptions.getService();
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("serivce name is null");
        }

        ConsumerServiceConfig serviceConfig = new ConsumerServiceConfig();
        serviceConfig.setConsumer(clientOptions.getApplication());
        serviceConfig.setProvider(importOptions.getApplication());
        serviceConfig.setService(importOptions.getService());
        serviceConfig.setConnectRetry(importOptions.getConnectRetry());
        serviceConfig.setTimeoutRetry(importOptions.getTimeoutRetry());
        serviceConfig.setTimeoutMillis(importOptions.getTimeoutMillis());

        // handle
        if (serviceClass.equals(GenericService.class)) {
            importOptions.setSerializerType(FocusOptions.DEFAULT_SERIALIZER_TYPE);
        } else {
            // parse method info and set invoke config
            serviceConfig.parseMethod(serviceClass.getMethods());
            List<InvokeOptions> iol = importOptions.getInvokeOptions();
            if (iol != null) {
                ConsumerMethodConfig mc;
                for (InvokeOptions io : iol) {
                    String methodName = io.getMethodName();
                    mc = (ConsumerMethodConfig) serviceConfig.lookup(methodName);
                    if (mc != null) {
                        mc.setTimeoutMillis(io.getTimeoutMillis());
                        mc.setTimeoutRetry(io.getTimeoutRetry());
                    }
                }
            }
        }

        // init service protocol codec
        protocolCodec(serviceConfig, clientOptions, importOptions);

        // create invoke handler chain
        serviceConfig.setHandler(createInvokeHandler(serviceConfig));

        // subscribe service provider
        serivceLocater.subscribe(serviceConfig.getProvider());

        ProxyFactory proxyFactory = clientOptions.getProxyFactory();
        return proxyFactory.create(serviceClass, serviceConfig);
    }

    private void protocolCodec(ConsumerServiceConfig serviceConfig, ClientOptions clientOptions,
            ImportOptions importOptions) {
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

    private Handler createInvokeHandler(ServiceConfig serviceConfig) {
        Handler remote = new RemoteInvokeHandler(serviceConfig, serivceLocater, connector);
        return new ConsumerInvokeHandler(serviceConfig, remote).addInterceptor(clientOptions.getInterceptors());
    }

}
