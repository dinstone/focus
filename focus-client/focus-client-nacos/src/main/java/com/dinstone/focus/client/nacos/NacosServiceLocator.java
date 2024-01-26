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
package com.dinstone.focus.client.nacos;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dinstone.focus.client.locate.DiscoveryServiceLocator;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;

public class NacosServiceLocator extends DiscoveryServiceLocator {

    private final NamingService namingService;
    private final NacosLocatorOptions locatorOptions;

    public NacosServiceLocator(NacosLocatorOptions locatorOptions) {
        try {
            this.namingService = NamingFactory.createNamingService(locatorOptions.getAddresses());
            this.locatorOptions = locatorOptions;
        } catch (NacosException e) {
            throw new RuntimeException("create nacos service locator error", e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            namingService.shutDown();
        } catch (NacosException e) {
            // ignore
        }
    }

    @Override
    protected void freshService(ServiceCache serviceCache) throws Exception {
        List<ServiceInstance> instances = new LinkedList<>();
        final String serviceName = serviceCache.getServiceName();
        List<Instance> healths = namingService.selectInstances(serviceName, true);
        for (Instance healthy : healths) {
            instances.add(convert(healthy));
        }
        serviceCache.setInstances(instances);
    }

    private static ServiceInstance convert(Instance healthy) {
        DefaultInstance defaultInstance = new DefaultInstance();
        defaultInstance.setServiceName(healthy.getServiceName());
        defaultInstance.setInstanceCode(healthy.getInstanceId());
        defaultInstance.setInstanceHost(healthy.getIp());
        defaultInstance.setInstancePort(healthy.getPort());
        Map<String, String> metadata = healthy.getMetadata();
        if (metadata != null && metadata.get("enableSsl") != null) {
            defaultInstance.setEnableSsl(Boolean.parseBoolean(metadata.get("enableSsl")));
        }
        defaultInstance.setMetadata(metadata);
        return defaultInstance;
    }

    @Override
    protected long freshInterval() {
        return locatorOptions.getInterval();
    }

}
