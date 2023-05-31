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
import java.util.HashMap;
import java.util.Map;

/**
 * service instance info
 *
 * @author dinstone
 *
 * @version 1.0.0
 */
public class ServiceInstance implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private Map<String, String> attributes = new HashMap<>();

    private String serviceName;

    private String serviceGroup;

    private String instanceCode;

    private String instanceHost;

    private int instancePort;

    private volatile InetSocketAddress address;

    public String getEndpointCode() {
        return attributes.get("endpointCode");
    }

    public void setEndpointCode(String endpointCode) {
        attributes.put("endpointCode", endpointCode);
    }

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

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
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
        String rt = attributes.get("registTime");
        if (rt != null) {
            return Long.parseLong(rt);
        }
        return 0;
    }

    public void setRegistTime(long registTime) {
        attributes.put("registTime", Long.toString(registTime));
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    public ServiceInstance addAttribute(String att, Object value) {
        this.attributes.put(att, value.toString());
        return this;
    }

    public ServiceInstance removeAttribute(String att) {
        this.attributes.remove(att);
        return this;
    }

    public InetSocketAddress getServiceAddress() {
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
        return "ServiceInstance [serviceName=" + serviceName + ", serviceGroup=" + serviceGroup + ", instanceCode="
                + instanceCode + ", instanceHost=" + instanceHost + ", instancePort=" + instancePort + ", attributes="
                + attributes + "]";
    }

}
