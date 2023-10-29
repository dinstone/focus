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
package com.dinstone.focus.clutch;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * service instance info: service part and instance part and metadata part.
 *
 * @author dinstone
 *
 * @version 1.0.0
 */
public class ServiceInstance implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_NAMESPACE = "default";

    private Map<String, String> metadata = new LinkedHashMap<>();

    private String namespace = DEFAULT_NAMESPACE;

    private String serviceName;

    private String serviceType;

    private String instanceCode;

    private String instanceHost;

    private int instancePort;

    private volatile InetSocketAddress address;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getInstanceHost() {
        return instanceHost;
    }

    public void setInstanceHost(String instanceHost) {
        this.instanceHost = instanceHost;
    }

    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public long getRegistTime() {
        String rt = metadata.get("registTime");
        if (rt != null) {
            return Long.parseLong(rt);
        }
        return 0;
    }

    public void setRegistTime(long registTime) {
        metadata.put("registTime", Long.toString(registTime));
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    public ServiceInstance addMetadata(String meta, String data) {
        if (meta != null) {
            this.metadata.put(meta, data);
        }
        return this;
    }

    public ServiceInstance removeMetadata(String meta) {
        this.metadata.remove(meta);
        return this;
    }

    public InetSocketAddress getSocketAddress() {
        if (address == null) {
            address = new InetSocketAddress(instanceHost, instancePort);
        }

        return address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instanceCode == null) ? 0 : instanceCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceInstance other = (ServiceInstance) obj;
        if (instanceCode == null) {
            if (other.instanceCode != null)
                return false;
        } else if (!instanceCode.equals(other.instanceCode))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ServiceInstance [identity=" + serviceName + ", namespace=" + namespace + ", instanceCode="
                + instanceCode + ", instanceType=" + serviceType + ", instanceHost=" + instanceHost + ", instancePort="
                + instancePort + ", metadata=" + metadata + "]";
    }

}
