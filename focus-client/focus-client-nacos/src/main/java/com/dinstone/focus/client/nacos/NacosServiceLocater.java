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
package com.dinstone.focus.client.nacos;

import java.util.LinkedList;
import java.util.List;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dinstone.focus.client.locate.AbstractServiceLocater;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.protocol.Call;

public class NacosServiceLocater extends AbstractServiceLocater {

    private NamingService namingService;

    public NacosServiceLocater(NacosLocaterOptions locaterOptions) {
        try {
            this.namingService = NamingFactory.createNamingService(locaterOptions.getAddresses());
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
    public void subscribe(String serviceName) {
        try {
            namingService.selectInstances(serviceName, true, true);
        } catch (NacosException e) {
            // ignore
        }
    }

    @Override
    protected List<ServiceInstance> routing(Call call, ServiceInstance selected) throws Exception {
        List<Instance> instances = namingService.selectInstances(call.getProvider(), true);
        List<ServiceInstance> sis = new LinkedList<>();
        for (Instance instance : instances) {
            if (selected != null && selected.getInstanceCode().equals(instance.getInstanceId())) {
                continue;
            }

            DefaultInstance defaultInstance = new DefaultInstance();
            defaultInstance.setServiceName(instance.getServiceName());
            defaultInstance.setInstanceCode(instance.getInstanceId());
            defaultInstance.setInstanceHost(instance.getIp());
            defaultInstance.setInstancePort(instance.getPort());
            defaultInstance.setMetadata(instance.getMetadata());

            sis.add(defaultInstance);
        }
        return sis;
    }

}
