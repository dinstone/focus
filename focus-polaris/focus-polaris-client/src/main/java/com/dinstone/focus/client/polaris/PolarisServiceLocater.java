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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.protocol.Call;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;

public class PolarisServiceLocater implements ServiceLocater {

	private static final String DEFAULT_NAMESPACE = "default";

	private ConsumerAPI consumerAPI;

	private RouterAPI routerAPI;

	public PolarisServiceLocater(String[] polarisAddress) {
		Configuration config = ConfigAPIFactory.createConfigurationByAddress(polarisAddress);
		consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(config);
		routerAPI = RouterAPIFactory.createRouterAPIByConfig(config);
	}

	@Override
	public InetSocketAddress locate(Call call, InetSocketAddress selected) {
		GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
		request.setNamespace(DEFAULT_NAMESPACE);
		request.setService(call.getProvider());
		request.setTimeoutMs(1000);
		InstancesResponse response = consumerAPI.getHealthyInstances(request);
		ServiceInstances sis = response.getServiceInstances();
		if (sis.getInstances().isEmpty()) {
			return null;
		}

		List<Instance> instances = new ArrayList<>();
		for (Instance instance : response.getInstances()) {
			if (instance.getHost().equalsIgnoreCase(selected.getHostString())
					&& instance.getPort() == selected.getPort()) {
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
		Instance instance = routerAPI.processLoadBalance(lbRequest).getTargetInstance();
		return new InetSocketAddress(instance.getHost(), instance.getPort());
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
	}

	@Override
	public void feedback(Call call, InetSocketAddress selected, boolean ok) {

	}

}
