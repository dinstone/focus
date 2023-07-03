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
package com.dinstone.focus.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import com.dinstone.focus.FocusOptions;
import com.dinstone.focus.client.locate.DefaultLocaterFactory;
import com.dinstone.focus.transport.ConnectOptions;
import com.dinstone.focus.utils.NetworkUtil;

/**
 * global level options
 * 
 * @author dinstone
 *
 */
public class ClientOptions extends FocusOptions<ClientOptions> {

    private static final int DEFAULT_CONNECT_RETRY = 2;

    private LocaterFactory locaterFactory = new DefaultLocaterFactory();

    private List<InetSocketAddress> connectAddresses = new LinkedList<>();

    private ConnectOptions connectOptions = ConnectOptions.DEFAULT_CONNECT_OPTIONS;

    private int connectRetry = DEFAULT_CONNECT_RETRY;

    private InetSocketAddress consumerAddress;

    public ClientOptions(String identity) {
        super(identity);
    }

    public ConnectOptions getConnectOptions() {
        return connectOptions;
    }

    public ClientOptions setConnectOptions(ConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
        return this;
    }

    public ClientOptions connect(String addresses) {
        if (addresses == null || addresses.length() == 0) {
            return this;
        }

        String[] addressArrays = addresses.split(",");
        for (String address : addressArrays) {
            int pidx = address.lastIndexOf(':');
            if (pidx > 0 && (pidx < address.length() - 1)) {
                String host = address.substring(0, pidx);
                int port = Integer.parseInt(address.substring(pidx + 1));

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

    public LocaterFactory getLocaterFactory() {
        return locaterFactory;
    }

    public ClientOptions setLocaterFactory(LocaterFactory locaterFactory) {
        this.locaterFactory = locaterFactory;
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

    public int getConnectRetry() {
        return connectRetry;
    }

    public ClientOptions setConnectRetry(int connectRetry) {
        this.connectRetry = connectRetry;
        return this;
    }

}
