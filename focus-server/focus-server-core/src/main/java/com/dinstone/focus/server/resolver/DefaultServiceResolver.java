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
package com.dinstone.focus.server.resolver;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.server.ServerOptions;

public class DefaultServiceResolver implements ServiceResolver {

    private Map<String, ServiceConfig> serviceConfigMap = new ConcurrentHashMap<>();

    private ServiceRegistry serviceRegistry;

    public DefaultServiceResolver(ClutchOptions clutchOptions) {
        if (clutchOptions != null) {
            ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
            for (ClutchFactory clutchFactory : serviceLoader) {
                if (clutchFactory.appliable(clutchOptions)) {
                    this.serviceRegistry = clutchFactory.createServiceRegistry(clutchOptions);
                    break;
                }
            }
        }
    }

    @Override
    public List<ServiceConfig> getServices() {
        return serviceConfigMap.values().stream().collect(Collectors.toList());
    }

    @Override
    public void registry(ServiceConfig serviceConfig) {
        String serviceName = serviceConfig.getService();
        if (serviceConfigMap.containsKey(serviceName)) {
            throw new RuntimeException("multiple object registed with the service name : " + serviceName);
        }
        serviceConfigMap.putIfAbsent(serviceName, serviceConfig);
    }

    @Override
    public ServiceConfig lookup(String serviceName) {
        return serviceConfigMap.get(serviceName);
    }

    @Override
    public void publish(ServerOptions serverOptions) {
        if (serviceRegistry == null) {
            return;
        }

        String app = serverOptions.getApplication();
        String host = serverOptions.getListenAddress().getHostString();
        int port = serverOptions.getListenAddress().getPort();

        StringBuilder code = new StringBuilder();
        code.append(app).append("@");
        code.append(host).append(":").append(port);

        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceCode(code.toString());
        instance.setInstanceHost(host);
        instance.setInstancePort(port);
        instance.setServiceName(app);
        instance.setRegistTime(System.currentTimeMillis());
        instance.setMetadata(serverOptions.getMetadata());

        try {
            serviceRegistry.register(instance);
        } catch (Exception e) {
            throw new FocusException("can't register application: " + app, e);
        }
    }

    @Override
    public void destroy() {
        if (serviceRegistry != null) {
            serviceRegistry.destroy();
        }
    }

}
