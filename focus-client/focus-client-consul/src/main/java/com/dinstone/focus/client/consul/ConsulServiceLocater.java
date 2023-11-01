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
package com.dinstone.focus.client.consul;

import java.util.LinkedList;
import java.util.List;

import com.dinstone.focus.client.locate.AbstractServiceLocater;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.protocol.Call;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;

public class ConsulServiceLocater extends AbstractServiceLocater {

    private ConsulClient client;

    public ConsulServiceLocater(ConsulLocaterOptions locaterOptions) {
        this.client = new ConsulClient(locaterOptions.getAgentHost(), locaterOptions.getAgentPort());
    }

    @Override
    public void destroy() {
    }

    @Override
    protected List<ServiceInstance> routing(Call call, ServiceInstance selected) throws Exception {
        HealthServicesRequest hr = HealthServicesRequest.newBuilder().setPassing(true).build();
        List<HealthService> healthServices = client.getHealthServices(call.getProvider(), hr).getValue();

        List<ServiceInstance> sis = new LinkedList<>();
        for (HealthService healthService : healthServices) {
            Service service = healthService.getService();
            if (selected != null && selected.getInstanceCode().equals(service.getId())) {
                continue;
            }

            DefaultInstance instance = new DefaultInstance();
            instance.setInstanceCode(service.getId());
            instance.setServiceName(service.getService());
            instance.setInstanceHost(service.getAddress());
            instance.setInstancePort(service.getPort());
            instance.setMetadata(service.getMeta());
            sis.add(instance);
        }
        return sis;
    }

}
