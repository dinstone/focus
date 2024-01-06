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
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;

public class NacosServiceResolver extends DefaultServiceResolver {

    private final NamingService namingService;

    public NacosServiceResolver(NacosResolverOptions options) {
        try {
            this.namingService = NamingFactory.createNamingService(options.getAddresses());
        } catch (NacosException e) {
            throw new RuntimeException("create nacos service locater error", e);
        }
    }

    @Override
    public void destroy() {
        try {
            namingService.shutDown();
        } catch (NacosException e) {
            // ignore
        }
    }

    @Override
    public void publish(ServerOptions serverOptions) throws Exception {
        ServiceInstance service = createInstance(serverOptions);
        Instance instance = new Instance();
        instance.setInstanceId(service.getInstanceCode());
        instance.setServiceName(service.getServiceName());
        instance.setIp(service.getInstanceHost());
        instance.setPort(service.getInstancePort());
        instance.setMetadata(service.getMetadata());
        instance.setHealthy(true);

        namingService.registerInstance(service.getServiceName(), instance);
    }

}
