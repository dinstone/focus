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
package com.dinstone.focus.client.binding;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;

public class DefaultReferenceBinding implements ReferenceBinding {

    protected InetSocketAddress consumerAddress;

    protected ServiceDiscovery serviceDiscovery;

    public DefaultReferenceBinding(ClutchOptions clutchOptions, InetSocketAddress consumerAddress) {
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

    @Override
    public void binding(ServiceConfig serviceConfig) {
        if (serviceDiscovery != null) {
            try {
                serviceDiscovery.subscribe(serviceConfig.getProvider());
            } catch (Exception e) {
                throw new RuntimeException("service reference bind error", e);
            }
        }
    }

    @Override
    public List<ServiceInstance> lookup(String application) {

        if (serviceDiscovery != null) {
            try {
                Collection<ServiceInstance> c = serviceDiscovery.discovery(application);
                if (c != null) {
                    return new ArrayList<ServiceInstance>(c);
                }
            } catch (Exception e) {
                throw new RuntimeException("service [" + application + "] discovery error", e);
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        if (serviceDiscovery != null) {
            serviceDiscovery.destroy();
        }
    }

}
