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
package com.dinstone.focus.binding;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dinstone.clutch.ServiceDiscovery;
import com.dinstone.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.photon.utils.NetworkUtil;

public class DefaultReferenceBinding implements ReferenceBinding {

    protected InetSocketAddress consumerAddress;

    protected ServiceDiscovery serviceDiscovery;

    public DefaultReferenceBinding(ServiceDiscovery serviceDiscovery) {
        this(serviceDiscovery, null);
    }

    public DefaultReferenceBinding(ServiceDiscovery serviceDiscovery, InetSocketAddress consumerAddress) {
        this.serviceDiscovery = serviceDiscovery;

        if (consumerAddress == null) {
            try {
                InetAddress addr = NetworkUtil.getPrivateAddresses().get(0);
                consumerAddress = new InetSocketAddress(addr, 0);
            } catch (Exception e) {
                throw new RuntimeException("can't init ReferenceBinding", e);
            }
        }
        this.consumerAddress = consumerAddress;
    }

    @Override
    public <T> void binding(ServiceConfig wrapper) {
        if (serviceDiscovery != null) {
            try {
                serviceDiscovery.listen(createServiceDescription(wrapper));
            } catch (Exception e) {
                throw new RuntimeException("service reference bind error", e);
            }
        }
    }

    protected <T> ServiceInstance createServiceDescription(ServiceConfig config) {
        String group = config.getGroup();
        String host = consumerAddress.getAddress().getHostAddress();
        int port = consumerAddress.getPort();

        StringBuilder code = new StringBuilder();
        code.append(config.getAppCode()).append("@");
        code.append(host).append(":").append(port).append("$");
        code.append((group == null ? "" : group));

        ServiceInstance description = new ServiceInstance();
        description.setInstanceCode(code.toString());
        description.setEndpointCode(config.getAppCode());
        description.setServiceName(config.getService());
        description.setServiceGroup(group);
        description.setHost(host);
        description.setPort(port);

        return description;
    }

    @Override
    public List<ServiceInstance> lookup(String serviceName) {
        try {
            if (serviceDiscovery != null) {
                Collection<ServiceInstance> c = serviceDiscovery.discovery(serviceName);
                if (c != null) {
                    return new ArrayList<ServiceInstance>(c);
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("service [" + serviceName + "] discovery error", e);
        }
    }

    @Override
    public void destroy() {
        if (serviceDiscovery != null) {
            serviceDiscovery.destroy();
        }
    }

}
