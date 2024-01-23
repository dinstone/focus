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
package com.dinstone.focus.server.consul;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

public class ConsulServiceResolver extends DefaultServiceResolver {

    private final ConsulClient client;
    private final ConsulResolverOptions options;

    private final Map<String, ScheduledFuture<?>> serviceMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread t = new Thread(task, "consul-service-check");
        t.setDaemon(true);
        return t;
    });

    public ConsulServiceResolver(ConsulResolverOptions options) {
        this.client = new ConsulClient(options.getAgentHost(), options.getAgentPort());
        this.options = options;
    }

    @Override
    public void destroy() {
        serviceMap.values().forEach(f -> f.cancel(true));

        if (serviceInstance != null) {
            client.agentServiceDeregister(serviceInstance.getInstanceCode());
        }

        executor.shutdownNow();
    }

    @Override
    public void publish(ServiceInstance serviceInstance) throws Exception {
        this.serviceInstance = serviceInstance;

        NewService newService = new NewService();
        newService.setId(serviceInstance.getInstanceCode());
        newService.setName(serviceInstance.getServiceName());
        newService.setAddress(serviceInstance.getInstanceHost());
        newService.setPort(serviceInstance.getInstancePort());
        newService.setMeta(serviceInstance.getMetadata());

        NewService.Check check = new NewService.Check();
        check.setTtl(options.getCheckTtl() + "s");
        newService.setCheck(check);

        client.agentServiceRegister(newService);

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                client.agentCheckPass("service:" + serviceInstance.getInstanceCode());
            } catch (Exception e) {
                // ignore
            }
        }, 0, options.getInterval(), TimeUnit.SECONDS);
        serviceMap.put(serviceInstance.getInstanceCode(), future);
    }

}
