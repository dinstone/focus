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

import com.dinstone.focus.binding.DefaultImplementBinding;
import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.endpoint.ServiceExporter;
import com.dinstone.focus.invoker.InvocationHandler;
import com.dinstone.focus.invoker.ServiceInvoker;
import com.dinstone.focus.proxy.ServiceProxy;
import com.dinstone.focus.proxy.ServiceProxyFactory;
import com.dinstone.focus.registry.LocalRegistryFactory;
import com.dinstone.focus.registry.RegistryFactory;
import com.dinstone.focus.registry.ServiceRegistry;
import com.dinstone.focus.server.invoker.LocalInvocationHandler;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.Acceptor;

public class Server implements ServiceExporter {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private Acceptor acceptor;

    private ServerEndpointOption endpointConfig;

    private InetSocketAddress serviceAddress;

    private ServiceRegistry serviceRegistry;

    private ImplementBinding implementBinding;

    private ServiceProxyFactory serviceProxyFactory;

    Server(ServerEndpointOption endpointOption, InetSocketAddress serviceAddress) {
        checkAndInit(endpointOption, serviceAddress);
    }

    private void checkAndInit(ServerEndpointOption endpointOption, InetSocketAddress serviceAddress) {
        if (endpointOption == null) {
            throw new IllegalArgumentException("endpointOption is null");
        }
        this.endpointConfig = endpointOption;

        // check bind service address
        if (serviceAddress == null) {
            throw new RuntimeException("server not bind service address");
        }
        this.serviceAddress = serviceAddress;

        RegistryFactory registryFactory = new LocalRegistryFactory();
        this.serviceRegistry = registryFactory.createServiceRegistry(endpointOption.getRegistryConfig());

        this.implementBinding = new DefaultImplementBinding(endpointOption, serviceRegistry, serviceAddress);

        InvocationHandler invocationHandler = createInvocationHandler(implementBinding);
        ServiceInvoker serviceInvoker = new ServiceInvoker(invocationHandler);
        this.serviceProxyFactory = new ServiceProxyFactory(serviceInvoker);

        AcceptorFactory acceptorFactory = new AcceptorFactory();
        this.acceptor = acceptorFactory.create(endpointOption.getTransportConfig(), serviceInvoker);
    }

    private InvocationHandler createInvocationHandler(ImplementBinding implementBinding2) {
        return new LocalInvocationHandler(implementBinding);
    }

    public synchronized Server start() {
        acceptor.bind(serviceAddress);

        LOG.info("JRPC server is started on {}", serviceAddress);

        return this;
    }

    public synchronized Server stop() {
        destroy();

        LOG.info("JRPC server is stopped on {}", serviceAddress);

        return this;
    }

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, T serviceImplement) {
        exporting(serviceInterface, "", endpointConfig.getDefaultTimeout(), serviceImplement);
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, String group, T serviceImplement) {
        exporting(serviceInterface, group, endpointConfig.getDefaultTimeout(), serviceImplement);
    }

    @Override
    public <T> void exporting(Class<T> serviceInterface, String group, int timeout, T serviceImplement) {
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = endpointConfig.getDefaultTimeout();
        }

        try {
            ServiceProxy<T> wrapper = serviceProxyFactory.create(serviceInterface, group, timeout, serviceImplement);
            implementBinding.binding(wrapper, endpointConfig);
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
    }

}
