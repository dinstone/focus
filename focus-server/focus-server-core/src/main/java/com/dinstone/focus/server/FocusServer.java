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
package com.dinstone.focus.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;
import com.dinstone.focus.server.config.ProviderServiceConfig;
import com.dinstone.focus.server.invoke.LocalInvokeHandler;
import com.dinstone.focus.server.invoke.ProviderChainHandler;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;
import com.dinstone.focus.transport.AcceptOptions;
import com.dinstone.focus.transport.Acceptor;
import com.dinstone.focus.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class FocusServer implements ServiceExporter, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FocusServer.class);

    private final Map<String, ServiceConfig> serviceConfigMap = new ConcurrentHashMap<>();

    private AcceptorFactory acceptorFactory;

    private ServiceResolver serviceResolver;

    private ServerOptions serverOptions;

    private Acceptor acceptor;

    public FocusServer(ServerOptions serverOption) {
        checkAndInit(serverOption);
    }

    private void checkAndInit(ServerOptions serverOptions) {
        if (serverOptions == null) {
            throw new IllegalArgumentException("serverOption is null");
        }
        this.serverOptions = serverOptions;

        // check listen address
        if (serverOptions.getListenAddress() == null) {
            throw new IllegalArgumentException("listen address is null");
        }
        // check acceptor options and factory
        AcceptOptions acceptOptions = serverOptions.getAcceptOptions();
        if (acceptOptions == null) {
            throw new IllegalArgumentException("accept options is null");
        }
        ServiceLoader<AcceptorFactory> cfLoader = ServiceLoader.load(AcceptorFactory.class);
        for (AcceptorFactory acceptorFactory : cfLoader) {
            if (acceptorFactory.applicable(acceptOptions)) {
                this.acceptorFactory = acceptorFactory;
            }
        }
        if (this.acceptorFactory == null) {
            throw new FocusException("can't find a acceptor implement for " + acceptOptions);
        }

        // create service resolver
        ResolverOptions resolverOptions = serverOptions.getResolverOptions();
        ServiceLoader<ResolverFactory> rfLoader = ServiceLoader.load(ResolverFactory.class);
        for (ResolverFactory resolverFactory : rfLoader) {
            if (resolverFactory.applicable(resolverOptions)) {
                this.serviceResolver = resolverFactory.create(resolverOptions);
            }
        }
        if (this.serviceResolver == null) {
            LOG.warn("will use the default service resolver");
            this.serviceResolver = new DefaultServiceResolver();
        }

        LOG.info("focus server created for [{}]", serverOptions.getApplication());
    }

    public synchronized FocusServer start() {
        InetSocketAddress listenAddress = serverOptions.getListenAddress();
        try {
            if (this.acceptor == null) {
                this.acceptor = acceptorFactory.create(serverOptions.getAcceptOptions());
            }

            // startup acceptor
            this.acceptor.bind(listenAddress, this::lookupService);

            // register application
            this.serviceResolver.publish(createServiceInstance(serverOptions));

            LOG.info("focus server starting on {}", listenAddress);
        } catch (Exception e) {
            LOG.warn("focus server starting on {}", listenAddress, e);
            throw new FocusException("start focus server error", e);
        }

        return this;
    }

    @Override
    public synchronized void close() {
        LOG.info("focus server stopping on {}", serverOptions.getListenAddress());

        if (serviceResolver != null) {
            serviceResolver.destroy();
        }
        if (acceptor != null) {
            acceptor.destroy();
            acceptor = null;
        }

        LOG.info("focus server destroy for [{}]", serverOptions.getApplication());
    }

    public InetSocketAddress getListenAddress() {
        return serverOptions.getListenAddress();
    }

    public List<ServiceConfig> getServices() {
        return new ArrayList<>(serviceConfigMap.values());
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance) {
        exporting(clazz, instance, new ExportOptions(clazz.getName()));
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance, String service) {
        exporting(clazz, instance, new ExportOptions(service));
    }

    @Override
    public <T> void exporting(Class<T> clazz, T instance, ExportOptions exportOptions) {
        String service = exportOptions.getService();
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("service name is null");
        }

        try {
            ProviderServiceConfig serviceConfig = new ProviderServiceConfig();
            serviceConfig.setService(service);
            serviceConfig.setTarget(instance);
            serviceConfig.setProvider(serverOptions.getApplication());

            // create and set method configure
            serviceConfig.parseMethod(clazz.getDeclaredMethods());

            // create invoke handler chain
            serviceConfig.setHandler(createInvokeHandler(serviceConfig));

            // init service protocol codec
            protocolCodec(serviceConfig, serverOptions, exportOptions);

            // registry service handler
            registryService(serviceConfig);

            LOG.info("exporting {}", serviceConfig);
        } catch (Exception e) {
            throw new FocusException("export service error:" + e.getMessage(), e);
        }
    }

    private ServiceInstance createServiceInstance(ServerOptions serverOptions) {
        String app = serverOptions.getApplication();
        String host = serverOptions.getListenAddress().getHostString();
        int port = serverOptions.getListenAddress().getPort();

        DefaultInstance instance = new DefaultInstance();
        String code = host + ":" + port;
        instance.setInstanceCode(code);
        instance.setInstanceHost(host);
        instance.setInstancePort(port);
        instance.setServiceName(app);

        AcceptOptions acceptOptions = serverOptions.getAcceptOptions();
        instance.setProtocolType(acceptOptions.getProtocol());
        instance.setEnableSsl(acceptOptions.isEnableSsl());
        instance.setMetadata(serverOptions.getMetadata());
        return instance;
    }

    private void registryService(ProviderServiceConfig serviceConfig) {
        String serviceName = serviceConfig.getService();
        if (serviceConfigMap.containsKey(serviceName)) {
            throw new RuntimeException("multiple object register with the service name : " + serviceName);
        }
        serviceConfigMap.putIfAbsent(serviceName, serviceConfig);
    }

    private ServiceConfig lookupService(String serviceName) {
        return serviceConfigMap.get(serviceName);
    }

    private void protocolCodec(ProviderServiceConfig serviceConfig, ServerOptions serverOptions,
            ExportOptions exportOptions) {
        Serializer serializer = SerializerFactory.lookup(exportOptions.getSerializerType());
        if (serializer == null) {
            serializer = SerializerFactory.lookup(serverOptions.getSerializerType());
        }
        if (serializer == null) {
            throw new IllegalArgumentException("serializer type is error");
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

    private Handler createInvokeHandler(ServiceConfig serviceConfig) {
        LocalInvokeHandler localHandler = new LocalInvokeHandler(serviceConfig);
        return new ProviderChainHandler(serviceConfig, localHandler).addInterceptor(serverOptions.getInterceptors());
    }

}
