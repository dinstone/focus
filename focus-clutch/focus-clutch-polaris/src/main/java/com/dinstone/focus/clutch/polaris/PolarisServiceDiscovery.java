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
package com.dinstone.focus.clutch.polaris;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

public class PolarisServiceDiscovery implements ServiceDiscovery {

    private ConsumerAPI consumerAPI;

    public PolarisServiceDiscovery(PolarisClutchOptions clutchOptions) {
        consumerAPI = DiscoveryAPIFactory.createConsumerAPIByAddress(clutchOptions.getAddresses());
    }

    @Override
    public void destroy() {
        consumerAPI.close();
    }

    @Override
    public void cancel(String serviceName) {

    }

    @Override
    public void subscribe(String serviceName) throws Exception {

    }

    @Override
    public Collection<ServiceInstance> discovery(String serviceName) throws Exception {
        if (serviceName == null || serviceName.isEmpty()) {
            return Collections.emptyList();
        }

        GetInstancesRequest request = new GetInstancesRequest();
        // 设置服务命名空间
        request.setNamespace("default");
        // 设置服务名称
        request.setService(serviceName);
        // 设置超时时间
        request.setTimeoutMs(1000);

        // 调用 ConsumerAPI 执行该请求
        Collection<ServiceInstance> silist = new ArrayList<>();

        InstancesResponse response = consumerAPI.getInstances(request);
        for (Instance instance : response.getInstances()) {
            ServiceInstance si = new ServiceInstance();
            si.setNamespace(instance.getNamespace());
            si.setServiceName(instance.getService());
            si.setServiceType(instance.getProtocol());
            si.setInstanceCode(instance.getId());
            si.setInstanceHost(instance.getHost());
            si.setInstancePort(instance.getPort());
            si.setMetadata(instance.getMetadata());
            silist.add(si);
        }

        return silist;
    }

}
