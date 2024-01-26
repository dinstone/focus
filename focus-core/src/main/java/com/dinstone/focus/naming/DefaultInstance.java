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
package com.dinstone.focus.naming;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultInstance implements ServiceInstance, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final Map<String, String> metadata = new LinkedHashMap<>();

    private String namespace = DEFAULT_NAMESPACE;

    private String serviceName;

    private String protocolType;

    private String instanceCode;

    private String instanceHost;

    private int instancePort;

    private boolean enableSsl;

    private volatile InetSocketAddress socketAddress;

    public DefaultInstance() {
        super();
    }

    public DefaultInstance(InetSocketAddress socketAddress, boolean enableSsl) {
        this.enableSsl = enableSsl;
        this.socketAddress = socketAddress;

        this.instanceHost = socketAddress.getHostString();
        this.instancePort = socketAddress.getPort();

        this.instanceCode = this.instanceHost + ":" + this.instancePort;
    }

    @Override
    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    @Override
    public String getInstanceHost() {
        return instanceHost;
    }

    public void setInstanceHost(String instanceHost) {
        this.instanceHost = instanceHost;
    }

    @Override
    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    @Override
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

    @Override
    public InetSocketAddress getInstanceAddress() {
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress(instanceHost, instancePort);
        }
        return socketAddress;
    }

    @Override
    public boolean isEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultInstance other = (DefaultInstance) obj;
        return Objects.equals(instanceCode, other.instanceCode);
    }

    @Override
    public String toString() {
        return "DefaultInstance [namespace=" + namespace + ", serviceName=" + serviceName + ", instanceCode="
                + instanceCode + ", protocolType=" + protocolType + ", instanceHost=" + instanceHost + ", instancePort="
                + instancePort + ", metadata=" + metadata + "]";
    }

}
