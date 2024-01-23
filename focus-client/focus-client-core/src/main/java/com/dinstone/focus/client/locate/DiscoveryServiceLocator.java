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
package com.dinstone.focus.client.locate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;

public abstract class DiscoveryServiceLocator extends AbstractServiceLocator {

    private static final int DEFAULT_INTERVAL = 3;

    private final Map<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread t = new Thread(task, "Service-Locator-Fresh");
        t.setDaemon(true);
        return t;
    });

    public static class ServiceCache {

        private final String serviceName;

        private ScheduledFuture<?> freshFuture;

        private volatile List<ServiceInstance> instances;

        public ServiceCache(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setFreshFuture(ScheduledFuture<?> freshFuture) {
            this.freshFuture = freshFuture;
        }

        public List<ServiceInstance> getInstances() {
            return instances;
        }

        public void setInstances(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        public void destroy() {
            if (freshFuture != null) {
                freshFuture.cancel(false);
            }
        }

    }

    protected List<ServiceInstance> routing(Invocation invocation, List<ServiceInstance> exclusions) {
        ServiceCache serviceCache = serviceCacheMap.get(invocation.getProvider());
        if (serviceCache == null || serviceCache.getInstances() == null) {
            return null;
        }

        return serviceCache.getInstances().stream().filter(i -> !exclusions.contains(i)).collect(Collectors.toList());
    }

    @Override
    public void subscribe(String serviceName) {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(serviceName);
            if (serviceCache == null) {
                serviceCache = new ServiceCache(serviceName);
                ServiceCache service = serviceCache;
                ScheduledFuture<?> freshFuture = executor.scheduleAtFixedRate(() -> {
                    try {
                        freshService(service);
                    } catch (Exception e) {
                        // ignore
                    }
                }, freshInterval(), freshInterval(), TimeUnit.SECONDS);

                // init
                try {
                    freshService(service);
                } catch (Exception e) {
                    // ignore
                }

                serviceCache.setFreshFuture(freshFuture);
                // cache the service
                serviceCacheMap.put(serviceName, serviceCache);
            }
        }
    }

    protected void freshService(ServiceCache serviceCache) throws Exception {
    }

    protected long freshInterval() {
        return DEFAULT_INTERVAL;
    }

    @Override
    public void destroy() {
        synchronized (serviceCacheMap) {
            for (ServiceCache serviceCache : serviceCacheMap.values()) {
                serviceCache.destroy();
            }
            serviceCacheMap.clear();
            executor.shutdownNow();
        }
    }

}