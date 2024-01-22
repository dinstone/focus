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
package com.dinstone.focus.server.polaris;

import java.util.List;
import java.util.Map;

import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.resolver.DefaultServiceResolver;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

public class PolarisServiceResolver extends DefaultServiceResolver {

    private final SDKContext polarisContext;
    private final ProviderAPI providerAPI;

    public PolarisServiceResolver(PolarisResolverOptions locatorOptions) {
        List<String> polarisAddress = locatorOptions.getAddresses();
        if (polarisAddress == null || polarisAddress.isEmpty()) {
            polarisContext = SDKContext.initContext();
            providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(polarisContext);
        } else {
            Configuration config = ConfigAPIFactory.createConfigurationByAddress(polarisAddress);
            polarisContext = SDKContext.initContextByConfig(config);
            providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(polarisContext);
        }
    }

    @Override
    public void publish(ServerOptions serverOptions) {
        ServiceInstance serviceInstance = createInstance(serverOptions);

        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setInstanceId(serviceInstance.getInstanceCode());
        // 设置实例所属服务信息
        request.setService(serviceInstance.getServiceName());
        request.setProtocol(serviceInstance.getProtocolType());
        // 设置实例所属服务的命名空间信息
        request.setNamespace(serviceInstance.getNamespace());
        // 设置实例的 host 信息
        request.setHost(serviceInstance.getInstanceHost());
        // 设置实例的端口信息
        request.setPort(serviceInstance.getInstancePort());

        Map<String, String> metadata = serviceInstance.getMetadata();
        request.setMetadata(metadata);
        // 可选，资源访问Token，即用户/用户组访问凭据，仅当服务端开启客户端鉴权时才需配置
        request.setToken(metadata.get("token"));
        // 设置实例版本
        request.setVersion(metadata.get("version"));
        // 设置实例权重
        String w = metadata.get("weight");
        request.setWeight(parseInt(w, 100));
        // 设置实例的标签
        // 设置实例地理位置 zone 信息
        request.setZone(metadata.get("zone"));
        // 设置实例地理位置 region 信息
        request.setRegion(metadata.get("region"));
        // 设置实例地理位置 campus 信息
        request.setCampus(metadata.get("campus"));

        // 设置心跳健康检查ttl，单位为s，不填默认为5s，TTL的取值范围为 (0s, 60s]
        // 开启了心跳健康检查，客户端必须以TTL间隔上报心跳
        // 健康检查服务器3个TTL未受到心跳则将实例置为不健康
        // request.setTtl(3);

        providerAPI.registerInstance(request);
    }

    private int parseInt(final String w, int d) {
        if (w != null && !w.isEmpty()) {
            try {
                return Integer.parseInt(w);
            } catch (Exception ignored) {
            }
        }
        return d;
    }

    @Override
    public void destroy() {
        providerAPI.close();
        if (polarisContext != null) {
            polarisContext.close();
        }
    }

}
