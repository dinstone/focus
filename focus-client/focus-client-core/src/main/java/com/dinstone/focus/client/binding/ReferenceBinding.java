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
package com.dinstone.focus.client.binding;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;

public class ReferenceBinding {

    protected InetSocketAddress consumerAddress;

    protected ServiceDiscovery serviceDiscovery;

    public ReferenceBinding(ClutchOptions clutchOptions, InetSocketAddress consumerAddress) {
        if (clutchOptions != null) {
            ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
            for (ClutchFactory clutchFactory : serviceLoader) {
                if (clutchFactory.appliable(clutchOptions)) {
                    this.serviceDiscovery = clutchFactory.createServiceDiscovery(clutchOptions);
                    break;
                }
            }
        }

        this.consumerAddress = consumerAddress;
    }

    public <T> void binding(ServiceConfig serviceConfig) {
        if (serviceDiscovery != null) {
            try {
                serviceDiscovery.listen(createServiceInstance(serviceConfig));
            } catch (Exception e) {
                throw new RuntimeException("service reference bind error", e);
            }
        }
    }

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

    public void destroy() {
        if (serviceDiscovery != null) {
            serviceDiscovery.destroy();
        }
    }

    protected <T> ServiceInstance createServiceInstance(ServiceConfig config) {
        String group = config.getGroup();
        String host = consumerAddress.getAddress().getHostAddress();
        int port = consumerAddress.getPort();

        StringBuilder code = new StringBuilder();
        code.append(config.getEndpoint()).append("@");
        code.append(host).append(":").append(port).append("$");
        code.append((group == null ? "" : group));

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setInstanceCode(code.toString());
        serviceInstance.setEndpointCode(config.getEndpoint());
        serviceInstance.setServiceName(config.getService());
        serviceInstance.setServiceGroup(group);
        serviceInstance.setInstanceHost(host);
        serviceInstance.setInstancePort(port);

        return serviceInstance;
    }

}
