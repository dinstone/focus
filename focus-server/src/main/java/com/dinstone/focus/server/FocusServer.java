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

import com.dinstone.focus.binding.DefaultImplementBinding;
import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.dinstone.focus.codec.photon.PhotonProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.focus.endpoint.ServiceProvider;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.filter.FilterChainHandler;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.server.invoke.LocalInvokeHandler;
import com.dinstone.focus.server.invoke.ProvideInvokeHandler;
import com.dinstone.focus.server.transport.FocusProcessor;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.Acceptor;

public class FocusServer implements ServiceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FocusServer.class);

    private EndpointOptions<ServerOptions> serverOptions;

    private InetSocketAddress serviceAddress;

    private ServiceRegistry serviceRegistry;

    private ImplementBinding implementBinding;

    private PhotonProtocolCodec protocolCodec;

    private Acceptor acceptor;

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

        // init ProtocolCodec
        this.protocolCodec = new PhotonProtocolCodec(serverOptions);

        // load and create registry
        ClutchOptions clutchOptions = serverOptions.getClutchOptions();
        if (clutchOptions != null) {
            ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
            for (ClutchFactory clutchFactory : serviceLoader) {
                if (clutchFactory.appliable(clutchOptions)) {
                    this.serviceRegistry = clutchFactory.createServiceRegistry(clutchOptions);
                    break;
                }
            }
        }

        this.implementBinding = new DefaultImplementBinding(serviceRegistry, serviceAddress);

        this.acceptor = createAcceptor(serverOptions, protocolCodec, implementBinding);

        LOG.info("focus server startup, {}", serviceAddress);
        try {
            acceptor.bind(serviceAddress);

            LOG.info("focus server started, {}", serviceAddress);
        } catch (Exception e) {
            LOG.warn("focus server failure, {}", serviceAddress, e);
            throw new FocusException("start focus server error", e);
        }
    }

    private Acceptor createAcceptor(ServerOptions serverOptions, PhotonProtocolCodec protocolCodec,
            ImplementBinding implementBinding) {
        Acceptor acceptor = new Acceptor(serverOptions.getAcceptOptions());
        ExecutorSelector selector = serverOptions.getExecutorSelector();
        acceptor.setMessageProcessor(new FocusProcessor(implementBinding, protocolCodec, selector));
        return acceptor;
    }

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, T serviceImplement) {
        exporting(serviceInterface, "", serverOptions.getDefaultTimeout(), serviceImplement);
    }

    @Override
    public <T> void exporting(Class<T> sic, String group, int timeout, T sio) {
        exporting(sic, sic.getName(), group, serverOptions.getDefaultTimeout(), sio);
    }

    @Override
    public void exporting(Class<? extends Object> clazz, String service, String group, int timeout, Object bean) {
        if (service == null || service.isEmpty()) {
            service = clazz.getName();
        }
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = serverOptions.getDefaultTimeout();
        }

        try {
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setGroup(group);
            serviceConfig.setService(service);
            serviceConfig.setTimeout(timeout);
            serviceConfig.setTarget(bean);

            serviceConfig.parseMethod(clazz.getDeclaredMethods());
            serviceConfig.setEndpoint(serverOptions.getEndpoint());

            InvokeHandler invokeHandler = createInvokeHandler(serviceConfig);
            serviceConfig.setHandler(invokeHandler);

            implementBinding.binding(serviceConfig);
        } catch (Exception e) {
            throw new FocusException("export service error", e);
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

        LOG.info("focus server destroyed, {}", serviceAddress);
    }

}
