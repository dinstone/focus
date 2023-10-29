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

import java.util.Map;

import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

public class PolarisServiceRegistry implements ServiceRegistry {

    private ProviderAPI providerAPI;

    public PolarisServiceRegistry(PolarisClutchOptions clutchOptions) {
        providerAPI = DiscoveryAPIFactory.createProviderAPIByAddress(clutchOptions.getAddresses());
    }

    @Override
    public void register(ServiceInstance service) throws Exception {
        InstanceRegisterRequest request = new InstanceRegisterRequest();
        // 设置实例所属服务信息
        request.setService(service.getServiceName());
        request.setProtocol(service.getServiceType());
        // 设置实例所属服务的命名空间信息
        request.setNamespace(service.getNamespace());
        // 设置实例的 host 信息
        request.setHost(service.getInstanceHost());
        // 设置实例的端口信息
        request.setPort(service.getInstancePort());

        Map<String, String> metadata = service.getMetadata();
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

        InstanceRegisterResponse response = providerAPI.registerInstance(request);
        service.setInstanceCode(response.getInstanceId());
    }

    private int parseInt(final String w, int d) {
        if (w != null && !w.isEmpty()) {
            try {
                return Integer.parseInt(w);
            } catch (Exception e) {
            }
        }
        return d;
    }

    @Override
    public void deregister(ServiceInstance service) throws Exception {
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setInstanceID(service.getInstanceCode());
        providerAPI.deRegister(request);
    }

    @Override
    public void destroy() {
        providerAPI.close();
    }

}
