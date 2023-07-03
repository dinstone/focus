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
package com.dinstone.focus.clutch.zookeeper;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;

import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZookeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);

    private final ConcurrentHashMap<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final ServiceInstanceSerializer serializer = new ServiceInstanceSerializer();

    private final String basePath;

    private final CuratorFramework client;

    private ConnectionStateListener connectionStateListener;

    public ZookeeperServiceDiscovery(ZookeeperClutchOptions discoveryConfig) {
        String zkNodes = discoveryConfig.getZookeeperNodes();
        if (zkNodes == null || zkNodes.length() == 0) {
            throw new IllegalArgumentException("zookeeper.node.list is empty");
        }

        String basePath = discoveryConfig.getConfigPath();
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("basePath is empty");
        }
        this.basePath = basePath;

        // build CuratorFramework Object;
        this.client = CuratorFrameworkFactory.newClient(zkNodes,
                new ExponentialBackoffRetry(discoveryConfig.getBaseSleepTime(), discoveryConfig.getMaxRetries()));

        // add connection state change listener
        this.connectionStateListener = new ConnectionStateListener() {

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if ((newState == ConnectionState.RECONNECTED) || (newState == ConnectionState.CONNECTED)) {
                    try {
                        // watch();
                    } catch (Exception e) {
                        LOG.error("Could not re-register instances after reconnection", e);
                    }
                }
            }
        };
        this.client.getConnectionStateListenable().addListener(connectionStateListener);

        // start CuratorFramework service;
        this.client.start();
    }

    @Override
    public void destroy() {
        synchronized (serviceCacheMap) {
            for (ServiceCache serviceCache : serviceCacheMap.values()) {
                serviceCache.destroy();
            }
            serviceCacheMap.clear();
        }
        client.close();
    }

    @Override
    public void cancel(String serviceName) {
        ServiceCache serviceCache = serviceCacheMap.get(serviceName);
        if (serviceCache != null) {
            serviceCache.destroy();
            serviceCacheMap.remove(serviceName);
        }
    }

    @Override
    public void subscribe(String serviceName) throws Exception {
        ServiceCache serviceCache = null;
        synchronized (serviceCacheMap) {
            serviceCache = serviceCacheMap.get(serviceName);
            if (serviceCache == null) {
                serviceCache = new ServiceCache(client, serviceName).build();
                serviceCacheMap.put(serviceName, serviceCache);
            }
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

    private String pathForProviders(String name) {
        return ZKPaths.makePath(pathForService(name), "");
    }

    private String pathForService(String name) {
        return ZKPaths.makePath(basePath, name);
    }

    private class ServiceCache implements CuratorCacheListener {

        private final ConcurrentHashMap<String, ServiceInstance> providers = new ConcurrentHashMap<String, ServiceInstance>();

        private CuratorCache pathCache;

        private String providerPath;

        public ServiceCache(CuratorFramework client, String serviceName) {
            providerPath = pathForProviders(serviceName);

            pathCache = CuratorCache.build(client, providerPath);
            pathCache.listenable().addListener(this);
        }

        public Collection<ServiceInstance> getProviders() {
            return providers.values();
        }

        public ServiceCache build() throws Exception {
            pathCache.start();
            return this;
        }

        private void addProvider(ChildData childData, boolean onlyIfAbsent) {
            try {
                String instanceId = ZKPaths.getNodeFromPath(childData.getPath());
                ServiceInstance serviceInstance = serializer.deserialize(childData.getData());
                if (onlyIfAbsent) {
                    providers.putIfAbsent(instanceId, serviceInstance);
                } else {
                    providers.put(instanceId, serviceInstance);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        public void destroy() {
            pathCache.listenable().removeListener(this);
            pathCache.close();
        }

        @Override
        public void event(Type type, ChildData oldData, ChildData data) {
            switch (type) {
            case NODE_CREATED: {
                if (providerPath.equals(data.getPath())) {
                    return;
                }
                addProvider(data, false);
                break;
            }

            case NODE_CHANGED: {
                if (providerPath.equals(data.getPath())) {
                    return;
                }
                addProvider(data, false);
                break;
            }

            case NODE_DELETED: {
                providers.remove(ZKPaths.getNodeFromPath(oldData.getPath()));
                break;
            }
            default:
                break;
            }
        }

    }

}
