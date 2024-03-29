/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.client.consul;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dinstone.focus.client.locate.DiscoveryServiceLocator;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;

public class ConsulServiceLocator extends DiscoveryServiceLocator {

    private final ConsulClient consulClient;
    private final ConsulLocatorOptions locatorOptions;

    public ConsulServiceLocator(ConsulLocatorOptions locatorOptions) {
        this.consulClient = new ConsulClient(locatorOptions.getAgentHost(), locatorOptions.getAgentPort());
        this.locatorOptions = locatorOptions;
    }

    @Override
    protected long freshInterval() {
        return locatorOptions.getInterval();
    }

    @Override
    protected void freshService(ServiceCache serviceCache) {
        final String serviceName = serviceCache.getServiceName();
        HealthServicesRequest hr = HealthServicesRequest.newBuilder().setPassing(true).build();
        List<HealthService> healthServices = consulClient.getHealthServices(serviceName, hr).getValue();

        List<ServiceInstance> instances = new LinkedList<>();
        for (HealthService healthService : healthServices) {
            instances.add(convert(healthService));
        }
        serviceCache.setInstances(instances);
    }

    private static ServiceInstance convert(HealthService healthService) {
        Service service = healthService.getService();

        DefaultInstance instance = new DefaultInstance();
        instance.setInstanceCode(service.getId());
        instance.setServiceName(service.getService());
        instance.setInstanceHost(service.getAddress());
        instance.setInstancePort(service.getPort());
        Map<String, String> meta = service.getMeta();
        if (meta != null && meta.get("enableSsl") != null) {
            instance.setEnableSsl(Boolean.parseBoolean(meta.get("enableSsl")));
        }
        instance.setMetadata(meta);

        return instance;
    }

}
