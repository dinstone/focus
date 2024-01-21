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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;

public abstract class AbstractServiceLocater implements ServiceLocater {

    private static final int DEFAULT_INTERVAL = 3;

    private final AtomicInteger index = new AtomicInteger();

    private final Map<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable task) {
            Thread t = new Thread(task, "Service-Locater-Fresh");
            t.setDaemon(true);
            return t;
        }
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

        public ScheduledFuture<?> getFreshFuture() {
            return freshFuture;
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

    @Override
    public ServiceInstance locate(Invocation invocation, ServiceInstance selected) {
        try {
            // routing
            List<ServiceInstance> instances = routing(invocation, selected);
            // balance
            return balance(invocation, instances);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    protected List<ServiceInstance> routing(Invocation invocation, ServiceInstance selected) throws Exception {
        ServiceCache serviceCache = serviceCacheMap.get(invocation.getProvider());
        if (serviceCache == null) {
            return null;
        }

        List<ServiceInstance> candidates = new LinkedList<>();
        for (ServiceInstance candidate : serviceCache.getInstances()) {
            if (selected != null && selected.equals(candidate)) {
                continue;
            }
            candidates.add(candidate);
        }
        return candidates;
    }

    protected ServiceInstance balance(Invocation invocation, List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return instances.get(next % instances.size());
        }
    }

    @Override
    public void feedback(ServiceInstance instance, Invocation invocation, Object reply, Throwable error, long delay) {
    }

    @Override
    public void subscribe(String serviceName) {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(serviceName);
            if (serviceCache == null) {
                serviceCache = new ServiceCache(serviceName);
                ServiceCache service = serviceCache;
                ScheduledFuture<?> freshFuture = executor.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            freshService(service);
                        } catch (Exception e) {
                            // ignore
                        }
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