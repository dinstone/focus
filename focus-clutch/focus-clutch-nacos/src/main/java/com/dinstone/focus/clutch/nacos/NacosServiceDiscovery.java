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
package com.dinstone.focus.clutch.nacos;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;

public class NacosServiceDiscovery implements ServiceDiscovery {

    private Map<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable taskt) {
            Thread t = new Thread(taskt, "nacos-service-discovery");
            t.setDaemon(true);
            return t;
        }
    });

    private NacosClutchOptions config;

    private NamingService naming;

    public NacosServiceDiscovery(NacosClutchOptions clutchOptions) {
        this.config = clutchOptions;
        try {
            this.naming = NamingFactory.createNamingService(clutchOptions.getAddresses());
        } catch (NacosException e) {
            throw new RuntimeException("create nacos service discovery error", e);
        }
    }

    @Override
    public void destroy() {
        synchronized (serviceCacheMap) {
            for (ServiceCache serviceCache : serviceCacheMap.values()) {
                serviceCache.destroy();
            }
            serviceCacheMap.clear();
        }

        executorService.shutdownNow();
    }

    @Override
    public void cancel(String serviceName) {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(serviceName);
            if (serviceCache != null && serviceCache.decrement() <= 0) {
                serviceCache.destroy();
                serviceCacheMap.remove(serviceName);
            }
        }
    }

    @Override
    public void subscribe(String serviceName) throws Exception {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(serviceName);
            if (serviceCache == null) {
                serviceCache = new ServiceCache(serviceName, config).build();
                serviceCacheMap.put(serviceName, serviceCache);
            }
            serviceCache.increment();
        }
    }

    @Override
    public Collection<ServiceInstance> discovery(String serviceName) throws Exception {
        ServiceCache serviceCache = serviceCacheMap.get(serviceName);
        if (serviceCache != null) {
            return serviceCache.getProviders();
        }
        return null;
    }

    public class ServiceCache {

        private String serviceName;

        private NacosClutchOptions clutchOptions;

        private ScheduledFuture<?> scheduledFuture;

        private AtomicInteger reference = new AtomicInteger();

        private ConcurrentHashMap<String, ServiceInstance> providers = new ConcurrentHashMap<>();

        public ServiceCache(String serviceName, NacosClutchOptions clutchOptions) {
            this.serviceName = serviceName;
            this.clutchOptions = clutchOptions;
        }

        public ServiceCache build() {
            this.scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    try {
                        freshProvidors();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }, clutchOptions.getInterval(), clutchOptions.getInterval(), TimeUnit.SECONDS);

            try {
                freshProvidors();
            } catch (Exception e) {
                // ignore
            }

            return this;
        }

        protected void freshProvidors() throws Exception {
            List<Instance> instances = naming.selectInstances(serviceName, true, true);
            Map<String, ServiceInstance> newProviders = new HashMap<>();
            for (Instance instance : instances) {
                ServiceInstance description = new ServiceInstance();
                description.setServiceName(instance.getServiceName());
                description.setInstanceCode(instance.getInstanceId());
                description.setInstanceHost(instance.getIp());
                description.setInstancePort(instance.getPort());
                description.setMetadata(instance.getMetadata());

                newProviders.put(description.getInstanceCode(), description);
            }
            providers.clear();
            providers.putAll(newProviders);
        }

        public Collection<ServiceInstance> getProviders() {
            return providers.values();
        }

        public int increment() {
            return reference.incrementAndGet();
        }

        public int decrement() {
            return reference.decrementAndGet();
        }

        public void destroy() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }

    }
}
