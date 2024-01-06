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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

import com.dinstone.focus.naming.ServiceInstance;
import com.tencent.polaris.api.pojo.Instance;

public class PolarisServiceInstance implements ServiceInstance {

    private final Instance instance;

    private final InetSocketAddress address;

    public PolarisServiceInstance(Instance instance) {
        this.instance = instance;
        this.address = new InetSocketAddress(instance.getHost(), instance.getPort());
    }

    @Override
    public String getNamespace() {
        return instance.getNamespace();
    }

    @Override
    public String getServiceName() {
        return instance.getService();
    }

    @Override
    public String getInstanceCode() {
        return instance.getId();
    }

    @Override
    public String getProtocolType() {
        return instance.getProtocol();
    }

    @Override
    public String getInstanceHost() {
        return instance.getHost();
    }

    @Override
    public int getInstancePort() {
        return instance.getPort();
    }

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    @Override
    public InetSocketAddress getInstanceAddress() {
        return address;
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PolarisServiceInstance other = (PolarisServiceInstance) obj;
        return Objects.equals(instance.getId(), other.instance.getId());
    }

}
