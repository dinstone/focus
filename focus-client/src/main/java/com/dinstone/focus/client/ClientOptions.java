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
package com.dinstone.focus.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.dinstone.focus.client.locate.DefaultLocateFactory;
import com.dinstone.focus.client.locate.LocateFactory;
import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.photon.ConnectOptions;

public class ClientOptions extends EndpointOptions<ClientOptions> {

    private static final String DEFAULT_CODEC_ID = "json/jackson";

    private static final int DEFAULT_POOL_SIZE = 1;

    private List<InetSocketAddress> serviceAddresses = new ArrayList<>();

    private ConnectOptions connectOptions = new ConnectOptions();

    private int connectPoolSize = DEFAULT_POOL_SIZE;

    private String codecId = DEFAULT_CODEC_ID;

    private LocateFactory locateFactory;

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

                serviceAddresses.add(new InetSocketAddress(host, port));
            }
        }

        return this;
    }

    public ClientOptions connect(String host, int port) {
        serviceAddresses.add(new InetSocketAddress(host, port));
        return this;
    }

    public List<InetSocketAddress> getServiceAddresses() {
        return serviceAddresses;
    }

    public int getConnectPoolSize() {
        return connectPoolSize;
    }

    public ClientOptions setConnectPoolSize(int connectPoolSize) {
        this.connectPoolSize = connectPoolSize;
        return this;
    }

    public String getCodecId() {
        return codecId;
    }

    public ClientOptions setCodecId(String codecId) {
        this.codecId = codecId;
        return this;
    }

    public LocateFactory getLocateFactory() {
        if (locateFactory == null) {
            locateFactory = new DefaultLocateFactory();
        }
        return locateFactory;
    }

    public ClientOptions setLocateFactory(LocateFactory locateFactory) {
        this.locateFactory = locateFactory;
        return this;
    }

}
