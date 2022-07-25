/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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
package com.dinstone.focus.binding;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.clutch.ServiceInstance;
import com.dinstone.clutch.ServiceRegistry;
import com.dinstone.focus.config.MethodInfo;
import com.dinstone.focus.config.ServiceConfig;

public class DefaultImplementBinding implements ImplementBinding {

    protected Map<String, ServiceConfig> serviceConfigMap = new ConcurrentHashMap<>();

    protected InetSocketAddress providerAddress;

    protected ServiceRegistry serviceRegistry;

    public DefaultImplementBinding(ServiceRegistry serviceRegistry, InetSocketAddress providerAddress) {
        this.serviceRegistry = serviceRegistry;
        this.providerAddress = providerAddress;
    }

    @Override
    public <T> void binding(ServiceConfig serviceConfig) {
        String serviceId = serviceConfig.getService() + "-" + serviceConfig.getGroup();
        if (serviceConfigMap.get(serviceId) != null) {
            throw new RuntimeException("multiple object registed with the service interface " + serviceId);
        }
        serviceConfigMap.put(serviceId, serviceConfig);

        if (serviceRegistry != null) {
            publish(serviceConfig);
        }
    }

    protected void publish(ServiceConfig config) {
        String host = providerAddress.getAddress().getHostAddress();
        int port = providerAddress.getPort();
        String group = config.getGroup();

        StringBuilder code = new StringBuilder();
        code.append(config.getEndpoint()).append("@");
        code.append(host).append(":").append(port).append("$");
        code.append((group == null ? "" : group));

        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceCode(code.toString());
        instance.setEndpointCode(config.getEndpoint());
        instance.setHost(host);
        instance.setPort(port);
        instance.setServiceName(config.getService());
        instance.setServiceGroup(group);
        instance.setRtime(System.currentTimeMillis());

        List<String> methodDescList = new ArrayList<>();
        for (MethodInfo method : config.methodInfos()) {
            methodDescList.add(description(method));
        }
        instance.addAttribute("methods", methodDescList);
        instance.addAttribute("timeout", config.getTimeout());

        try {
            serviceRegistry.register(instance);
        } catch (Exception e) {
            throw new RuntimeException("can't publish service", e);
        }
    }

    private String description(MethodInfo mi) {
        StringBuilder desc = new StringBuilder();
        desc.append(getTypeName(mi.getReturnType()) + " ");
        desc.append(getTypeName(mi.getDeclarClass()) + ".");
        desc.append(mi.getMethodName() + "(");
        desc.append(getTypeName(mi.getParamType()));
        desc.append(")");
        return desc.toString();
    }

    private static String getTypeName(Class<?> type) {
        if (type.isArray()) {
            try {
                Class<?> cl = type;
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) {
            }
        }
        return type.getName();
    }

    @Override
    public void destroy() {
        if (serviceRegistry != null) {
            serviceRegistry.destroy();
        }
    }

    @Override
    public ServiceConfig lookup(String service, String group) {
        String serviceId = service + "-" + group;
        return serviceConfigMap.get(serviceId);
    }

}
