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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;

public class PolarisServiceLocater implements ServiceLocater {

    private static final String DEFAULT_NAMESPACE = "default";

    private SDKContext polarisContext;

    private ConsumerAPI consumerAPI;

    private RouterAPI routerAPI;

    public PolarisServiceLocater(PolarisLocaterOptions locaterOptions) {
        List<String> polarisAddress = locaterOptions.getAddresses();
        if (polarisAddress == null || polarisAddress.isEmpty()) {
            polarisContext = SDKContext.initContext();
        } else {
            Configuration config = ConfigAPIFactory.createConfigurationByAddress(polarisAddress);
            polarisContext = SDKContext.initContextByConfig(config);
        }

        consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(polarisContext);
        routerAPI = RouterAPIFactory.createRouterAPIByContext(polarisContext);
    }

    @Override
    public ServiceInstance locate(Call call, ServiceInstance selected) {
        GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
        request.setNamespace(DEFAULT_NAMESPACE);
        request.setService(call.getProvider());
        request.setTimeoutMs(1000);
        InstancesResponse response = consumerAPI.getHealthyInstances(request);
        ServiceInstances sis = response.getServiceInstances();
        if (sis.getInstances().isEmpty()) {
            return null;
        }

        List<Instance> instances = new LinkedList<>();
        for (Instance instance : response.getInstances()) {
            if (selected != null && instance.getId().equals(selected.getInstanceCode())) {
                continue;
            }
            instances.add(instance);
        }
        if (instances.isEmpty()) {
            return null;
        }

        ProcessLoadBalanceRequest lbRequest = new ProcessLoadBalanceRequest();
        // 设置需要参与负载均衡的服务实例
        lbRequest.setDstInstances(new DefaultServiceInstances(sis.getServiceKey(), instances));
        // 设置负载均衡策略
        // 当前支持的负载均衡策略如下
        // - 权重随机负载均衡: weightedRandom
        // - 权重一致性负载均衡: ringHash
        lbRequest.setLbPolicy("weightedRandom");

        //
        Instance instance = routerAPI.processLoadBalance(lbRequest).getTargetInstance();
        // return instance
        return new PolarisServiceInstance(instance);
    }

    @Override
    public void subscribe(String serviceName) {
        GetInstancesRequest request = new GetInstancesRequest();
        request.setNamespace(DEFAULT_NAMESPACE);
        request.setService(serviceName);
        request.setTimeoutMs(1000);
        consumerAPI.asyncGetInstances(request);
    }

    @Override
    public void destroy() {
        consumerAPI.destroy();
        if (polarisContext != null) {
            polarisContext.close();
        }
    }

    @Override
    public void feedback(ServiceInstance selected, Call call, Reply reply, Throwable error, long delay) {
        ServiceCallResult result = new ServiceCallResult();
        // 设置被调服务所在命名空间
        result.setNamespace(DEFAULT_NAMESPACE);
        // 设置被调服务的服务信息
        result.setService(call.getProvider());
        // result.setCallerService(new ServiceKey(DEFAULT_NAMESPACE,
        // call.getConsumer()));

        // 设置被调实例
        Instance instance = ((PolarisServiceInstance) selected).getInstance();
        result.setInstance(instance);

        // 设置本次请求的响应码
        int code = 0;
        RetStatus status = RetStatus.RetSuccess;
        if (error != null) {
            code = 1;
            status = RetStatus.RetFail;
            if (error instanceof TimeoutException) {
                code = 2;
                status = RetStatus.RetTimeout;
            }
        } else if (reply.isError()) {
            code = 1;
            status = RetStatus.RetFail;
        }
        result.setRetCode(code);
        result.setRetStatus(status);

        // 设置本次请求的耗时
        result.setDelay(delay);

        // 设置本次请求调用的方法
        result.setMethod(call.getService() + "/" + call.getMethod());

        consumerAPI.updateServiceCallResult(result);
    }

}
