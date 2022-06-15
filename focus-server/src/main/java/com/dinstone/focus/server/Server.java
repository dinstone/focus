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

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.clutch.RegistryFactory;
import com.dinstone.clutch.ServiceRegistry;
import com.dinstone.focus.binding.DefaultImplementBinding;
import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.codec.CodecFactory;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.focus.endpoint.ServiceExporter;
import com.dinstone.focus.filter.FilterChainHandler;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.server.invoke.LocalInvokeHandler;
import com.dinstone.focus.server.invoke.ProvideInvokeHandler;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.Acceptor;

public class Server implements ServiceExporter {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private Acceptor acceptor;

    private EndpointOptions<ServerOptions> serverOptions;

    private InetSocketAddress serviceAddress;

    private ServiceRegistry serviceRegistry;

    private ImplementBinding implementBinding;

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
        ServiceLoader<CodecFactory> cfLoader = ServiceLoader.load(CodecFactory.class);
        for (CodecFactory codecFactory : cfLoader) {
            CodecManager.regist(codecFactory.createCodec());
        }

        // load and create registry
        RegistryConfig registryConfig = serverOptions.getRegistryConfig();
        if (registryConfig != null) {
            ServiceLoader<RegistryFactory> serviceLoader = ServiceLoader.load(RegistryFactory.class);
            for (RegistryFactory registryFactory : serviceLoader) {
                if (registryFactory.canApply(registryConfig)) {
                    this.serviceRegistry = registryFactory.createServiceRegistry(registryConfig);
                    break;
                }
            }
        }

        this.implementBinding = new DefaultImplementBinding(serviceRegistry, serviceAddress);

        this.acceptor = new AcceptorFactory(serverOptions).create(implementBinding);

        LOG.info("focus server startup, {}", serviceAddress);
        acceptor.bind(serviceAddress);
        LOG.info("focus server started, {}", serviceAddress);
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
    public <T> void exporting(Class<T> sic, String group, int timeout, T sio) {
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = serverOptions.getDefaultTimeout();
        }

        try {
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setGroup(group);
            serviceConfig.setTimeout(timeout);
            serviceConfig.setService(sic.getName());
            serviceConfig.setMethods(sic.getDeclaredMethods());
            serviceConfig.setTarget(sio);

            serviceConfig.setAppCode(serverOptions.getAppCode());
            serviceConfig.setAppName(serverOptions.getAppName());

            InvokeHandler invokeHandler = createInvokeHandler(serviceConfig);
            serviceConfig.setHandler(invokeHandler);

            implementBinding.binding(serviceConfig);
        } catch (Exception e) {
            throw new RuntimeException("can't export service", e);
        }
    }

    private InvokeHandler createInvokeHandler(ServiceConfig serviceConfig) {
        LocalInvokeHandler localHandler = new LocalInvokeHandler(serviceConfig);
        FilterChainHandler filterChain = createFilterChain(serviceConfig, localHandler);
        return new ProvideInvokeHandler(filterChain);
    }

    private FilterChainHandler createFilterChain(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        FilterChainHandler chain = new FilterChainHandler(serviceConfig, invokeHandler);
        return chain.addFilter(serverOptions.getFilters());
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
