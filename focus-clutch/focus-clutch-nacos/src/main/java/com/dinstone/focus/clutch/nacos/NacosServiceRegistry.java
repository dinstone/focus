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
package com.dinstone.focus.clutch.nacos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class NacosServiceRegistry implements ServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(NacosServiceRegistry.class);

    private NamingService naming;

    private NacosClutchOptions config;

    private Map<String, Instance> serviceMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable taskt) {
            Thread t = new Thread(taskt, "consul-service-registry");
            t.setDaemon(true);
            return t;
        }
    });

    public NacosServiceRegistry(NacosClutchOptions clutchOptions) {
        this.config = clutchOptions;
        try {
            this.naming = NamingFactory.createNamingService(clutchOptions.getAddresses());
        } catch (NacosException e) {
            throw new RuntimeException("create nacos service registry error", e);
        }
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
        Instance instance = new Instance();
        instance.setInstanceId(service.getInstanceCode());
        instance.setIp(service.getInstanceHost());
        instance.setPort(service.getInstancePort());
        instance.setHealthy(true);

        instance.addMetadata("serviceName", service.getServiceName());
        instance.addMetadata("serviceGroup", service.getServiceGroup());
        instance.addMetadata("endpointCode", service.getEndpointCode());

        naming.registerInstance(service.getServiceName(), instance);

        serviceMap.put(service.getInstanceCode(), instance);
    }

    @Override
    public void deregister(ServiceInstance service) throws Exception {
        synchronized (serviceMap) {
            Instance instance = serviceMap.remove(service.getInstanceCode());
            if (instance != null) {
                naming.deregisterInstance(service.getInstanceCode(), instance);
            }
        }
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

}
