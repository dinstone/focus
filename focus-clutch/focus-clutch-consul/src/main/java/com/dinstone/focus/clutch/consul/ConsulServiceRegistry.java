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
package com.dinstone.focus.clutch.consul;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

public class ConsulServiceRegistry implements ServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulServiceRegistry.class);

    private ConsulClient client;

    private ConsulClutchOptions config;

    private Map<String, ScheduledFuture<?>> serviceMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable taskt) {
            Thread t = new Thread(taskt, "consul-service-registry");
            t.setDaemon(true);
            return t;
        }
    });

    public ConsulServiceRegistry(ConsulClutchOptions config) {
        this.config = config;
        this.client = new ConsulClient(config.getAgentHost(), config.getAgentPort());
    }

    @Override
    public void register(final ServiceInstance service) throws Exception {
        synchronized (serviceMap) {
            if (!serviceMap.containsKey(service.getInstanceCode())) {
                try {
                    register0(service);
                } catch (Exception e) {
                    retry(service);
                }
            }
        }
    }

    private void retry(final ServiceInstance service) {
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    LOG.info("retry register service {}", service);

                    register0(service);
                } catch (Exception e) {
                    // ignore
                    retry(service);
                }
            }
        }, config.getInterval(), TimeUnit.SECONDS);
    }

    private void register0(final ServiceInstance service) throws Exception {
        NewService newService = new NewService();
        newService.setId(service.getInstanceCode());
        newService.setName(service.getServiceName());
        newService.setAddress(service.getInstanceHost());
        newService.setPort(service.getInstancePort());
        newService.setMeta(service.getAttributes());

        NewService.Check check = new NewService.Check();
        check.setTtl(config.getCheckTtl() + "s");
        newService.setCheck(check);

        client.agentServiceRegister(newService);

        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    client.agentCheckPass("service:" + service.getInstanceCode());
                } catch (Exception e) {
                    // ignore
                }
            }
        }, 0, config.getInterval(), TimeUnit.SECONDS);

        serviceMap.put(service.getInstanceCode(), future);
    }

    @Override
    public void deregister(ServiceInstance service) throws Exception {
        synchronized (serviceMap) {
            ScheduledFuture<?> future = serviceMap.remove(service.getInstanceCode());
            if (future != null) {
                future.cancel(true);
            }
            client.agentServiceDeregister(service.getInstanceCode());
        }
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

}
