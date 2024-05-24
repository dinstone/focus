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
package com.dinstone.focus.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import com.dinstone.focus.FocusOptions;
import com.dinstone.focus.client.proxy.JdkProxyFactory;
import com.dinstone.focus.client.proxy.ProxyFactory;
import com.dinstone.focus.transport.ConnectOptions;
import com.dinstone.focus.utils.NetworkUtil;

/**
 * global level options
 *
 * @author dinstone
 */
public class ClientOptions extends FocusOptions<ClientOptions> {

    public static final int DEFAULT_TIMEOUT_MILLIS = 1000;

    /**
     * 0: not retry
     */
    private static final int DEFAULT_CONNECT_RETRY = 0;

    /**
     * 0: not retry
     */
    private static final int DEFAULT_TIMEOUT_RETRY = 0;

    private ConnectOptions connectOptions = ConnectOptions.DEFAULT_CONNECT_OPTIONS;

    private final List<InetSocketAddress> connectAddresses = new LinkedList<>();

    private ProxyFactory proxyFactory = new JdkProxyFactory();

    private InetSocketAddress consumerAddress;

    private LocatorOptions locatorOptions;

    private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

    private int timeoutRetry = DEFAULT_TIMEOUT_RETRY;

    private int connectRetry = DEFAULT_CONNECT_RETRY;

    public ClientOptions(String application) {
        super(application);
    }

    public ConnectOptions getConnectOptions() {
        return connectOptions;
    }

    public ClientOptions setConnectOptions(ConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
        return this;
    }

    public ClientOptions connect(String addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return this;
        }

        String[] addressArrays = addresses.split(",");
        for (String address : addressArrays) {
            int index = address.lastIndexOf(':');
            if (index > 0 && (index < address.length() - 1)) {
                String host = address.substring(0, index);
                int port = Integer.parseInt(address.substring(index + 1));

                connectAddresses.add(new InetSocketAddress(host, port));
            }
        }

        return this;
    }

    public ClientOptions connect(String host, int port) {
        connectAddresses.add(new InetSocketAddress(host, port));
        return this;
    }

    public List<InetSocketAddress> getConnectAddresses() {
        return connectAddresses;
    }

    public LocatorOptions getLocatorOptions() {
        return locatorOptions;
    }

    public ClientOptions setLocatorOptions(LocatorOptions locatorOptions) {
        this.locatorOptions = locatorOptions;
        return this;
    }

    public InetSocketAddress getConsumerAddress() {
        if (consumerAddress == null) {
            try {
                InetAddress addr = NetworkUtil.getPrivateAddresses().get(0);
                consumerAddress = new InetSocketAddress(addr, 0);
            } catch (Exception e) {
                throw new RuntimeException("can't init consumer address", e);
            }
        }
        return this.consumerAddress;
    }

    public ClientOptions consumerAddress(String host, int port) {
        consumerAddress = new InetSocketAddress(host, port);
        return this;
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public ClientOptions setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
        return this;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public ClientOptions setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public int getTimeoutRetry() {
        return timeoutRetry;
    }

    public ClientOptions setTimeoutRetry(int timeoutRetry) {
        this.timeoutRetry = timeoutRetry;
        return this;
    }

    public int getConnectRetry() {
        return connectRetry;
    }

    public ClientOptions setConnectRetry(int connectRetry) {
        this.connectRetry = connectRetry;
        return this;
    }
}
