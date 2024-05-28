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
package com.dinstone.focus.client;

import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.FocusOptions;
import com.dinstone.focus.client.config.ConsumerMethodConfig;
import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.client.invoke.ConsumerChainHandler;
import com.dinstone.focus.client.invoke.RemoteInvokeHandler;
import com.dinstone.focus.client.locate.DirectLinkServiceLocator;
import com.dinstone.focus.client.proxy.StubFactory;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.ChainHandler;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;
import com.dinstone.focus.transport.ConnectOptions;
import com.dinstone.focus.transport.Connector;
import com.dinstone.focus.transport.ConnectorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class FocusClient implements ServiceImporter, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClient.class);

    private ServiceLocator serviceLocator;

    private ClientOptions clientOptions;

    private StubFactory stubFactory;

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
            if (connectorFactory.applicable(connectOptions)) {
                this.connector = connectorFactory.create(connectOptions);
            }
        }
        if (this.connector == null) {
            throw new FocusException("can't find a connector implement");
        }

        // init service locator for service discovery, routing and load balance
        LocatorOptions locatorOptions = clientOptions.getLocatorOptions();
        ServiceLoader<LocatorFactory> lfLoader = ServiceLoader.load(LocatorFactory.class);
        for (LocatorFactory locatorFactory : lfLoader) {
            if (locatorFactory.applicable(locatorOptions)) {
                this.serviceLocator = locatorFactory.create(locatorOptions);
            }
        }
        // set default service locator
        if (this.serviceLocator == null) {
            this.serviceLocator = new DirectLinkServiceLocator(clientOptions);
        }

        this.stubFactory = new StubFactory(clientOptions.getProxyFactory());

        LOG.info("focus client created for [{}]", clientOptions.getApplication());
    }

    @Override
    public void close() {
        connector.destroy();
        serviceLocator.destroy();
        LOG.info("focus client destroy for [{}]", clientOptions.getApplication());
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
        if (serviceClass == null || !serviceClass.isInterface()) {
            throw new IllegalArgumentException("service class is not Interface");
        }

        String service = importOptions.getService();
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("service name is null");
        }

        ConsumerServiceConfig serviceConfig = new ConsumerServiceConfig();
        serviceConfig.setMetadata(clientOptions.getMetadata());
        serviceConfig.setConsumer(clientOptions.getApplication());
        serviceConfig.setProvider(importOptions.getApplication());
        serviceConfig.setService(importOptions.getService());
        // connect retry
        if (importOptions.getConnectRetry() > 0) {
            serviceConfig.setConnectRetry(importOptions.getConnectRetry());
        } else {
            serviceConfig.setConnectRetry(clientOptions.getConnectRetry());
        }
        // timeout retry
        if (importOptions.getTimeoutRetry() > 0) {
            serviceConfig.setTimeoutRetry(importOptions.getTimeoutRetry());
        } else {
            serviceConfig.setTimeoutRetry(clientOptions.getTimeoutRetry());
        }
        // timeout ms
        if (importOptions.getTimeoutMillis() > 0) {
            serviceConfig.setTimeoutMillis(importOptions.getTimeoutMillis());
        } else {
            serviceConfig.setTimeoutMillis(clientOptions.getTimeoutMillis());
        }

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
        serviceLocator.subscribe(serviceConfig.getProvider());

        LOG.info("importing {}", serviceConfig);

        return stubFactory.create(serviceClass, serviceConfig);
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
        Handler invokeHandler = new RemoteInvokeHandler(serviceConfig, serviceLocator, connector);
        ChainHandler chainHandler = new ConsumerChainHandler(serviceConfig, invokeHandler);
        return chainHandler.addInterceptor(clientOptions.getInterceptors());
    }

}
