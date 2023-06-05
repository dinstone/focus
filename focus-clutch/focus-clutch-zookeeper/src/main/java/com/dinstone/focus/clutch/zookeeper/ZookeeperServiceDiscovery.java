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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZookeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);

    private final ConcurrentHashMap<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final ServiceInstanceSerializer serializer = new ServiceInstanceSerializer();

    private volatile ConnectionState connectionState = ConnectionState.LOST;

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
                connectionState = newState;
                if ((newState == ConnectionState.RECONNECTED) || (newState == ConnectionState.CONNECTED)) {
                    try {
                        watch();
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

    protected void watch() throws Exception {
        synchronized (serviceCacheMap) {
            for (ServiceCache serviceCache : serviceCacheMap.values()) {
                serviceCache.addConsumer();
            }
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
        client.close();
    }

    @Override
    public void cancel(ServiceInstance description) {
        ServiceCache serviceCache = serviceCacheMap.get(description.getIdentity());
        if (serviceCache != null && serviceCache.removeConsumer() <= 0) {
            serviceCache.destroy();
            serviceCacheMap.remove(description.getIdentity());
        }
    }

    @Override
    public void listen(ServiceInstance description) throws Exception {
        ServiceCache serviceCache = null;
        synchronized (serviceCacheMap) {
            serviceCache = serviceCacheMap.get(description.getIdentity());
            if (serviceCache == null) {
                serviceCache = new ServiceCache(client, description).build();
                serviceCacheMap.put(description.getIdentity(), serviceCache);
            }
        }
        if (connectionState == ConnectionState.CONNECTED) {
            serviceCache.addConsumer();
        }
    }

    @Override
    public Collection<ServiceInstance> discovery(String name) throws Exception {
        ServiceCache serviceCache = serviceCacheMap.get(name);
        if (serviceCache != null) {
            return serviceCache.getProviders();
        }
        return null;
    }

    private String pathForConsumer(String name, String id) {
        return ZKPaths.makePath(pathForService(name) + "/consumers", id);
    }

    private String pathForProviders(String name) {
        return ZKPaths.makePath(pathForService(name) + "/providers", "");
    }

    private String pathForService(String name) {
        return ZKPaths.makePath(basePath, name);
    }

    private class ServiceCache implements CuratorCacheListener {

        private final ConcurrentHashMap<String, ServiceInstance> providers = new ConcurrentHashMap<String, ServiceInstance>();

        private final ConcurrentHashMap<String, ServiceInstance> consumers = new ConcurrentHashMap<String, ServiceInstance>();

        private ServiceInstance description;

        private CuratorCache pathCache;

        private String providerPath;

        public ServiceCache(CuratorFramework client, ServiceInstance description) {
            providerPath = pathForProviders(description.getIdentity());
            this.description = description;

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

        public int addConsumer() throws Exception {
            synchronized (consumers) {
                if (consumers.containsKey(description.getInstanceCode())) {
                    return consumers.size();
                }

                description.setRegistTime(System.currentTimeMillis());
                byte[] bytes = serializer.serialize(description);
                String path = pathForConsumer(description.getIdentity(), description.getInstanceCode());

                final int MAX_TRIES = 2;
                boolean isDone = false;
                for (int i = 0; !isDone && (i < MAX_TRIES); ++i) {
                    try {
                        client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, bytes);
                        isDone = true;
                    } catch (KeeperException.NodeExistsException e) {
                        // must delete then re-create so that watchers fire
                        client.delete().forPath(path);
                    }
                }

                consumers.put(description.getInstanceCode(), description);

                return consumers.size();
            }
        }

        public int removeConsumer() {
            synchronized (consumers) {
                consumers.remove(description.getInstanceCode());

                return consumers.size();
            }
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
            if (connectionState == ConnectionState.CONNECTED) {
                for (ServiceInstance consumer : consumers.values()) {
                    String path = pathForConsumer(consumer.getIdentity(), consumer.getInstanceCode());
                    try {
                        client.delete().forPath(path);
                    } catch (Exception e) {
                    }
                }
            }

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
