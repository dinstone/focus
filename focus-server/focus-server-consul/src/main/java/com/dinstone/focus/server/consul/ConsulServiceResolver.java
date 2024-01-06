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
package com.dinstone.focus.server.consul;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

public class ConsulServiceResolver extends DefaultServiceResolver {

    private final ConsulClient client;
    private final ConsulResolverOptions options;

    private final Map<String, ScheduledFuture<?>> serviceMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable task) {
            Thread t = new Thread(task, "consul-service-check");
            t.setDaemon(true);
            return t;
        }
    });

    public ConsulServiceResolver(ConsulResolverOptions options) {
        this.client = new ConsulClient(options.getAgentHost(), options.getAgentPort());
        this.options = options;
    }

    @Override
    public void destroy() {
        serviceMap.values().forEach(f -> f.cancel(true));
        executor.shutdownNow();
    }

    @Override
    public void publish(ServerOptions serverOptions) throws Exception {
        ServiceInstance service = createInstance(serverOptions);

        NewService newService = new NewService();
        newService.setId(service.getInstanceCode());
        newService.setName(service.getServiceName());
        newService.setAddress(service.getInstanceHost());
        newService.setPort(service.getInstancePort());
        newService.setMeta(service.getMetadata());

        NewService.Check check = new NewService.Check();
        check.setTtl(options.getCheckTtl() + "s");
        newService.setCheck(check);

        client.agentServiceRegister(newService);

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    client.agentCheckPass("service:" + service.getInstanceCode());
                } catch (Exception e) {
                    // ignore
                }
            }
        }, 0, options.getInterval(), TimeUnit.SECONDS);
        serviceMap.put(service.getInstanceCode(), future);
    }

}
