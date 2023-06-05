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
package com.dinstone.focus.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.dinstone.focus.ApplicationOptions;
import com.dinstone.focus.transport.AcceptOptions;
import com.dinstone.focus.utils.NetworkUtil;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

/**
 * global level options
 * 
 * @author dinstone
 *
 */
public class ServerOptions extends ApplicationOptions<ServerOptions> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerOptions.class);

    private AcceptOptions acceptOptions = AcceptOptions.DEFAULT_ACCEPT_OPTIONS;

    private InetSocketAddress serviceAddress;

    public ServerOptions(String identity) {
        super(identity);
    }

    public ServerOptions listen(InetSocketAddress socketAddress) {
        if (socketAddress != null) {
            this.serviceAddress = socketAddress;
        }
        return this;
    }

    public ServerOptions listen(int port) {
        this.serviceAddress = new InetSocketAddress(port);
        return this;
    }

    public ServerOptions listen(String host, int port) {
        try {
            List<InetSocketAddress> resolveAddress = resolveAddress(host, port);
            if (!resolveAddress.isEmpty()) {
                listen(resolveAddress.get(0));
            }
        } catch (SocketException e) {
            throw new RuntimeException("host is invalid", e);
        }
        return this;
    }

    public ServerOptions listen(String address) {
        if (address == null || address.isEmpty()) {
            throw new RuntimeException("address is empty");
        }

        InetSocketAddress socketAddress = parseServiceAddress(address);
        if (socketAddress == null) {
            throw new RuntimeException("address is invalid");
        }

        return listen(socketAddress);
    }

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    private InetSocketAddress parseServiceAddress(String address) {
        try {
            String[] hpParts = address.split(":", 2);
            if (hpParts.length == 2) {
                List<InetSocketAddress> resolveAddress = resolveAddress(hpParts[0], Integer.parseInt(hpParts[1]));
                if (!resolveAddress.isEmpty()) {
                    return resolveAddress.get(0);
                }
            }
        } catch (Exception e) {
            LOG.warn("parse service address error", e);
        }

        return null;
    }

    private List<InetSocketAddress> resolveAddress(String host, int port) throws SocketException {
        List<InetSocketAddress> addresses = new ArrayList<>();
        if (host == null || "-".equals(host)) {
            for (InetAddress inetAddress : NetworkUtil.getPrivateAddresses()) {
                addresses.add(new InetSocketAddress(inetAddress, port));
            }
        } else if ("+".equals(host)) {
            for (InetAddress inetAddress : NetworkUtil.getPublicAddresses()) {
                addresses.add(new InetSocketAddress(inetAddress, port));
            }
        } else if ("*".equals(host)) {
            addresses.add(new InetSocketAddress("0.0.0.0", port));
        } else {
            addresses.add(new InetSocketAddress(host, port));
        }
        return addresses;
    }

    public AcceptOptions getAcceptOptions() {
        return acceptOptions;
    }

    public ServerOptions setAcceptOptions(AcceptOptions acceptOptions) {
        this.acceptOptions = acceptOptions;
        return this;
    }

}
