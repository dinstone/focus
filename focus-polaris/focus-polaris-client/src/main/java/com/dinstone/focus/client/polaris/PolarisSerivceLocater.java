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
package com.dinstone.focus.client.polaris;

import java.util.ArrayList;
import java.util.List;

import com.dinstone.focus.client.SerivceLocater;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;

public class PolarisSerivceLocater implements SerivceLocater {

    private RouterAPI router;

    public PolarisSerivceLocater(RouterAPI router) {
        this.router = router;
    }

    @Override
    public ServiceInstance locate(Call call, ServiceInstance selected, List<ServiceInstance> sis) {

        List<Instance> instances = new ArrayList<>();
        for (ServiceInstance instance : sis) {
            if (selected != null && instance.getInstanceCode().equals(selected.getInstanceCode())) {
                continue;
            }

            DefaultInstance defaultInstance = new DefaultInstance();
            defaultInstance.setId(instance.getInstanceCode());
            defaultInstance.setHost(instance.getInstanceHost());
            defaultInstance.setPort(instance.getInstancePort());
            defaultInstance.setHealthy(true);
            defaultInstance.setWeight(100);
            instances.add(defaultInstance);
        }

        if (instances.isEmpty()) {
            throw new FocusException("can't find a live service instance for " + call.getService());
        }

        ProcessLoadBalanceRequest lbRequest = new ProcessLoadBalanceRequest();
        // 设置需要参与负载均衡的服务实例
        ServiceKey serviceKey = new ServiceKey("default", call.getTarget());
        lbRequest.setDstInstances(new DefaultServiceInstances(serviceKey, instances));
        // 设置负载均衡策略
        // 当前支持的负载均衡策略如下
        // - 权重随机负载均衡: weightedRandom
        // - 权重一致性负载均衡: ringHash
        lbRequest.setLbPolicy("weightedRandom");
        ProcessLoadBalanceResponse response = router.processLoadBalance(lbRequest);
        Instance instance = response.getTargetInstance();
        ServiceInstance si = new ServiceInstance();
        si.setInstanceCode(instance.getId());
        si.setInstanceHost(instance.getHost());
        si.setInstancePort(instance.getPort());

        return si;
    }

}
