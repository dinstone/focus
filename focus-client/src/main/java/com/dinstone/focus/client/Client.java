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

package com.dinstone.focus.client;

import java.net.InetSocketAddress;
import java.util.List;

import com.dinstone.focus.binding.DefaultReferenceBinding;
import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.invoker.LocationInvocationHandler;
import com.dinstone.focus.client.invoker.RemoteInvocationHandler;
import com.dinstone.focus.client.transport.ConnectionManager;
import com.dinstone.focus.endpoint.ServiceImporter;
import com.dinstone.focus.invoker.InvocationHandler;
import com.dinstone.focus.invoker.ServiceInvoker;
import com.dinstone.focus.proxy.ServiceProxy;
import com.dinstone.focus.proxy.ServiceProxyFactory;
import com.dinstone.focus.registry.LocalRegistryFactory;
import com.dinstone.focus.registry.RegistryFactory;
import com.dinstone.focus.registry.ServiceDiscovery;

public class Client implements ServiceImporter {

    private ClientEndpointOption endpointConfig;

    private ServiceDiscovery serviceDiscovery;

    private ReferenceBinding referenceBinding;

    private ConnectionManager connectionManager;

    private ServiceProxyFactory serviceProxyFactory;

    Client(ClientEndpointOption endpointOption, List<InetSocketAddress> serviceAddresses) {
        checkAndInit(endpointOption, serviceAddresses);
    }

    private void checkAndInit(ClientEndpointOption endpointOption, List<InetSocketAddress> serviceAddresses) {
        if (endpointOption == null) {
            throw new IllegalArgumentException("endpointConfig is null");
        }
        this.endpointConfig = endpointOption;

        // check transport provider
        this.connectionManager = new ConnectionManager(endpointOption.getTransportConfig());

        RegistryFactory registryFactory = new LocalRegistryFactory();
        this.serviceDiscovery = registryFactory.createServiceDiscovery(endpointOption.getRegistryConfig());

        this.referenceBinding = new DefaultReferenceBinding(endpointOption, serviceDiscovery);

        InvocationHandler invocationHandler = createInvocationHandler(serviceAddresses);
        this.serviceProxyFactory = new ServiceProxyFactory(new ServiceInvoker(invocationHandler));
    }

    private InvocationHandler createInvocationHandler(List<InetSocketAddress> serviceAddresses) {
        RemoteInvocationHandler rpcInvocationHandler = new RemoteInvocationHandler(connectionManager);
        return new LocationInvocationHandler(rpcInvocationHandler, referenceBinding, serviceAddresses);
    }

    @Override
    public void destroy() {
        connectionManager.destroy();
        referenceBinding.destroy();

        if (serviceDiscovery != null) {
            serviceDiscovery.destroy();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.dinstone.jrpc.endpoint.ServiceImporter#importService(java.lang.Class)
     */
    @Override
    public <T> T importing(Class<T> sic) {
        return importing(sic, "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.dinstone.jrpc.endpoint.ServiceImporter#importService(java.lang.Class,
     *      java.lang.String)
     */
    @Override
    public <T> T importing(Class<T> sic, String group) {
        return importing(sic, group, endpointConfig.getDefaultTimeout());
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.dinstone.jrpc.endpoint.ServiceImporter#importService(java.lang.Class,
     *      java.lang.String, int)
     */
    @Override
    public <T> T importing(Class<T> sic, String group, int timeout) {
        if (group == null) {
            group = "";
        }
        if (timeout <= 0) {
            timeout = endpointConfig.getDefaultTimeout();
        }

        try {
            ServiceProxy<T> wrapper = serviceProxyFactory.create(sic, group, timeout, null);
            referenceBinding.binding(wrapper);
            return wrapper.getProxy();
        } catch (Exception e) {
            throw new RuntimeException("can't import service", e);
        }
    }

}
