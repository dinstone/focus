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
package com.dinstone.focus.client.polaris;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.dinstone.focus.client.ServiceLocator;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

public class PolarisServiceLocator implements ServiceLocator {

    private static final String DEFAULT_NAMESPACE = "default";

    private final SDKContext polarisContext;

    private final ConsumerAPI consumerAPI;

    private final RouterAPI routerAPI;

    public PolarisServiceLocator(PolarisLocatorOptions locatorOptions) {
        List<String> polarisAddress = locatorOptions.getAddresses();
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
    public ServiceInstance locate(Invocation invocation, List<ServiceInstance> exclusions) {

        return stepLocate(invocation, exclusions);

        // return oneLocate(invocation);
    }

    private @NotNull ServiceInstance stepLocate(Invocation invocation, List<ServiceInstance> exclusions) {
        // discovery
        ServiceInstances instances = discovery(invocation, exclusions);
        // routing
        instances = routing(invocation, instances);
        // load balance
        return loadBalance(invocation, instances);
    }

    private @Nullable PolarisServiceInstance oneLocate(Invocation invocation) {
        GetOneInstanceRequest request = new GetOneInstanceRequest();
        request.setNamespace(DEFAULT_NAMESPACE);
        request.setService(invocation.getProvider());

        ServiceInfo source = new ServiceInfo();
        source.setNamespace(DEFAULT_NAMESPACE);
        source.setService(invocation.getConsumer());
        source.setMetadata(invocation.attributes());
        request.setServiceInfo(source);

        InstancesResponse response = consumerAPI.getOneInstance(request);

        Instance instance = response.getInstance();
        if (instance != null) {
            return new PolarisServiceInstance(instance);
        }
        return null;
    }

    private ServiceInstance loadBalance(Invocation invocation, ServiceInstances dstInstances) {
        ProcessLoadBalanceRequest lbRequest = new ProcessLoadBalanceRequest();
        // 设置需要参与负载均衡的服务实例
        lbRequest.setDstInstances(dstInstances);
        // 设置负载均衡策略, 当前支持的负载均衡策略如下
        // - 权重随机负载均衡: weightedRandom
        // - 权重一致性负载均衡: ringHash
        // lbRequest.setLbPolicy("weightedRandom");

        //
        Instance instance = routerAPI.processLoadBalance(lbRequest).getTargetInstance();
        // return instance
        return new PolarisServiceInstance(instance);
    }

    private ServiceInstances routing(Invocation invocation, ServiceInstances dstInstances) {
        ProcessRoutersRequest routeRequest = new ProcessRoutersRequest();
        // 设置待参与路由的目标实例
        routeRequest.setDstInstances(dstInstances);
        // 被调服务命名空间
        routeRequest.setNamespace(DEFAULT_NAMESPACE);
        // 被调服务名称
        routeRequest.setService(invocation.getProvider());
        // 可选，对应自定义路由规则中请求标签中的方法(Method)
        routeRequest.setMethod(invocation.getEndpoint());

        // 可选，设置主调服务信息，只用于路由规则匹配
        ServiceInfo serviceInfo = new ServiceInfo();
        // 设置主调服务命名空间
        serviceInfo.setNamespace(DEFAULT_NAMESPACE);
        // 设置主调服务名称
        serviceInfo.setService(invocation.getConsumer());
        serviceInfo.setMetadata(invocation.attributes());

        routeRequest.setSourceService(serviceInfo);

        return routerAPI.processRouters(routeRequest).getServiceInstances();
    }

    private ServiceInstances discovery(Invocation invocation, List<ServiceInstance> exclusions) {
        GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
        request.setNamespace(DEFAULT_NAMESPACE);
        request.setService(invocation.getProvider());

        InstancesResponse response = consumerAPI.getHealthyInstances(request);
        ServiceInstances healthInstances = response.getServiceInstances();

        if (!exclusions.isEmpty()) {
            List<Instance> instances = new LinkedList<>();
            for (Instance instance : healthInstances.getInstances()) {
                if (isContains(exclusions, instance)) {
                    continue;
                }
                instances.add(instance);
            }
            if (!instances.isEmpty()) {
                healthInstances = new DefaultServiceInstances(healthInstances.getServiceKey(), instances);
            }
        }

        return healthInstances;
    }

    private static boolean isContains(List<ServiceInstance> exclusions, Instance instance) {
        for (ServiceInstance element : exclusions) {
            if (element.getInstanceCode().equals(instance.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void feedback(ServiceInstance selected, Invocation invocation, Object reply, Throwable error, long delay) {
        ServiceCallResult result = new ServiceCallResult();
        // 设置被调服务所在命名空间
        result.setNamespace(DEFAULT_NAMESPACE);
        // 设置被调服务的服务信息
        result.setService(invocation.getProvider());
        // 设置本次请求调用的方法
        result.setMethod(invocation.getEndpoint());

        result.setCallerService(new ServiceKey(DEFAULT_NAMESPACE, invocation.getConsumer()));

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
        }
        result.setRetCode(code);
        result.setRetStatus(status);

        // 设置本次请求的耗时
        result.setDelay(delay);

        consumerAPI.updateServiceCallResult(result);
    }

}
