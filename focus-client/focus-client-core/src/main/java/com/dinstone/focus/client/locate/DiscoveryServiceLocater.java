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
package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;

public class DiscoveryServiceLocater extends DefaultServiceLocater {

    private ServiceDiscovery serviceDiscovery;

    public DiscoveryServiceLocater(ClutchOptions clutchOptions) {
        super();
        ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
        for (ClutchFactory clutchFactory : serviceLoader) {
            if (clutchFactory.appliable(clutchOptions)) {
                serviceDiscovery = clutchFactory.createServiceDiscovery(clutchOptions);
                break;
            }
        }
        if (serviceDiscovery == null) {
            throw new FocusException("can't find discovery implement for " + clutchOptions);
        }
    }

    @Override
    public void subscribe(String serviceName) {
        try {
            serviceDiscovery.subscribe(serviceName);
        } catch (Exception e) {
            throw new FocusException("subscribe " + serviceName + " error", e);
        }
    }

    @Override
    public void destroy() {
        serviceDiscovery.destroy();
    }

    protected List<InetSocketAddress> routing(Call call, InetSocketAddress selected) {
        List<InetSocketAddress> addresses = new ArrayList<>();

        try {
            Collection<ServiceInstance> serviceInstances = serviceDiscovery.discovery(call.getProvider());
            // routing
            for (ServiceInstance instance : serviceInstances) {
                if (instance.getSocketAddress().equals(selected)) {
                    continue;
                }
                addresses.add(instance.getSocketAddress());
            }
        } catch (Exception e) {
            // igonre
        }

        return addresses;
    }

}