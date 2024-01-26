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
package com.dinstone.focus.server.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;

public class NacosServiceResolver extends DefaultServiceResolver {

    private final NamingService namingService;

    public NacosServiceResolver(NacosResolverOptions options) {
        try {
            this.namingService = NamingFactory.createNamingService(options.getAddresses());
        } catch (NacosException e) {
            throw new RuntimeException("create nacos service locator error", e);
        }
    }

    @Override
    public void destroy() {
        try {
            if (serviceInstance != null) {
                Instance instance = new Instance();
                instance.setInstanceId(serviceInstance.getInstanceCode());
                instance.setServiceName(serviceInstance.getServiceName());
                namingService.deregisterInstance(serviceInstance.getServiceName(), instance);
            }
            namingService.shutDown();
        } catch (NacosException e) {
            // ignore
        }
    }

    @Override
    public void publish(ServiceInstance serviceInstance) throws Exception {
        this.serviceInstance = serviceInstance;

        Instance instance = new Instance();
        instance.setInstanceId(serviceInstance.getInstanceCode());
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setIp(serviceInstance.getInstanceHost());
        instance.setPort(serviceInstance.getInstancePort());
        instance.setMetadata(serviceInstance.getMetadata());
        instance.setHealthy(true);
        instance.getMetadata().put("enableSsl", Boolean.toString(serviceInstance.isEnableSsl()));

        namingService.registerInstance(serviceInstance.getServiceName(), instance);
    }

}
