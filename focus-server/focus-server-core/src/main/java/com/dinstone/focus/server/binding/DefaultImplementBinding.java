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
package com.dinstone.focus.server.binding;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.dinstone.focus.config.ServiceConfig;

public class DefaultImplementBinding implements ImplementBinding {

    protected Map<String, ServiceConfig> serviceConfigMap = new ConcurrentHashMap<>();

    protected InetSocketAddress providerAddress;

    protected ServiceRegistry serviceRegistry;

    public DefaultImplementBinding(ClutchOptions clutchOptions, InetSocketAddress providerAddress) {
        if (clutchOptions != null) {
            ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
            for (ClutchFactory clutchFactory : serviceLoader) {
                if (clutchFactory.appliable(clutchOptions)) {
                    this.serviceRegistry = clutchFactory.createServiceRegistry(clutchOptions);
                    break;
                }
            }
        }
        this.providerAddress = providerAddress;
    }

    @Override
    public void binding(ServiceConfig serviceConfig) {
        String serviceId = serviceConfig.getService();
        if (serviceConfigMap.get(serviceId) != null) {
            throw new RuntimeException("multiple object registed with the service interface " + serviceId);
        }
        serviceConfigMap.put(serviceId, serviceConfig);
    }

    @Override
    public ServiceConfig lookup(String service, String group) {
        if (group == null) {
            group = "";
        }
        String serviceId = service;
        return serviceConfigMap.get(serviceId);
    }

    @Override
    public void destroy() {
        if (serviceRegistry != null) {
            serviceRegistry.destroy();
        }
    }

    @Override
    public void publish(String application, String namespace) {
        if (serviceRegistry == null) {
            return;
        }

        String host = providerAddress.getAddress().getHostAddress();
        int port = providerAddress.getPort();

        StringBuilder code = new StringBuilder();
        code.append(application).append("@");
        code.append(host).append(":").append(port).append("$");
        code.append((namespace == null ? "default" : namespace));

        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceCode(code.toString());
        instance.setInstanceHost(host);
        instance.setInstancePort(port);
        instance.setIdentity(application);
        instance.setNamespace(namespace);
        instance.setRegistTime(System.currentTimeMillis());

        try {
            serviceRegistry.register(instance);
        } catch (Exception e) {
            throw new RuntimeException("can't publish service: " + application, e);
        }
    }

}
