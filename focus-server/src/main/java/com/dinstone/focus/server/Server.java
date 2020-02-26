/*
 * Copyright (C) 2013~2017 dinstone<dinstone@163.com>
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

import com.dinstone.focus.SchemaFactoryLoader;
import com.dinstone.focus.binding.DefaultImplementBinding;
import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.codec.CodecFactory;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.endpoint.ServiceExporter;
import com.dinstone.focus.filter.FilterChain;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.proxy.ServiceProxy;
import com.dinstone.focus.proxy.ServiceProxyFactory;
import com.dinstone.focus.registry.RegistryFactory;
import com.dinstone.focus.registry.ServiceRegistry;
import com.dinstone.focus.server.invoke.LocalInvokeHandler;
import com.dinstone.focus.server.invoke.ProvideInvokeHandler;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.Acceptor;

public class Server implements ServiceExporter {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private Acceptor acceptor;

    private ServerOptions serverOptions;

    private InetSocketAddress serviceAddress;

    private ServiceRegistry serviceRegistry;

    private ImplementBinding implementBinding;

    private ServiceProxyFactory serviceProxyFactory;

    public Server(ServerOptions serverOption) {
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

        // load and create rpc message codec
        SchemaFactoryLoader<CodecFactory> cfLoader = SchemaFactoryLoader.getInstance(CodecFactory.class);
        for (CodecFactory codecFactory : cfLoader.getSchemaFactorys()) {
            CodecManager.regist(codecFactory.getSchema(), codecFactory.createCodec());
        }

        // check registry provider
        String registrySchema = serverOptions.getRegistryConfig().getSchema();
        if (registrySchema != null && !registrySchema.isEmpty()) {
            SchemaFactoryLoader<RegistryFactory> rfLoader = SchemaFactoryLoader.getInstance(RegistryFactory.class);
            RegistryFactory registryFactory = rfLoader.getSchemaFactory(registrySchema);
            if (registryFactory == null) {
                throw new RuntimeException("can't find registry provider for schema : " + registrySchema);
            } else {
                this.serviceRegistry = registryFactory.createServiceRegistry(serverOptions.getRegistryConfig());
            }
        }

        this.implementBinding = new DefaultImplementBinding(serverOptions, serviceRegistry, serviceAddress);

        InvokeHandler invokeHandler = createInvocationHandler();
        this.serviceProxyFactory = new ServiceProxyFactory(invokeHandler);

        this.acceptor = new AcceptorFactory(serverOptions).create(invokeHandler);

        acceptor.bind(serviceAddress);
        LOG.info("focus server is created, {}", serviceAddress);
    }

    private InvokeHandler createInvocationHandler() {
        LocalInvokeHandler localInvokeHandler = new LocalInvokeHandler(implementBinding);
        return new ProvideInvokeHandler(new FilterChain(localInvokeHandler, serverOptions.getFilters()));
    }

    public synchronized Server start() {
        acceptor.bind(serviceAddress);

        LOG.info("focus server is started on {}", serviceAddress);
        return this;
    }

    public synchronized Server stop() {
        destroy();

        LOG.info("focus server is stopped on {}", serviceAddress);
        return this;
    }

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, T serviceImplement) {
        exporting(serviceInterface, "", serverOptions.getDefaultTimeout(), serviceImplement);
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, String group, T serviceImplement) {
        exporting(serviceInterface, group, serverOptions.getDefaultTimeout(), serviceImplement);
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, String group, int timeout, T serviceImplement) {
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = serverOptions.getDefaultTimeout();
        }

        try {
            ServiceProxy<T> wrapper = serviceProxyFactory.create(serviceInterface, group, timeout, serviceImplement);
            implementBinding.binding(wrapper);
        } catch (Exception e) {
            throw new RuntimeException("can't export service", e);
        }
    }

    @Override
    public void destroy() {
        if (implementBinding != null) {
            implementBinding.destroy();
        }
        if (serviceRegistry != null) {
            serviceRegistry.destroy();
        }
        if (acceptor != null) {
            acceptor.destroy();
        }

        LOG.info("focus server is destroy, {}", serviceAddress);
    }

}
