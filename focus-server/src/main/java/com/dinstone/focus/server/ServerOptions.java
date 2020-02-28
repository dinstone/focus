package com.dinstone.focus.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.utils.NetworkUtil;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.AcceptOptions;

public class ServerOptions extends EndpointOptions {

    private static final Logger LOG = LoggerFactory.getLogger(ServerOptions.class);

    private AcceptOptions acceptOptions = new AcceptOptions();

    private List<Filter> filters = new ArrayList<>();

    private InetSocketAddress serviceAddress;

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

    public List<Filter> getFilters() {
        return filters;
    }

    public ServerOptions addFilters(Filter... filters) {
        addFilters(Arrays.asList(filters));
        return this;
    }

    public ServerOptions addFilters(List<Filter> filters) {
        if (filters != null) {
            for (Filter filter : filters) {
                if (filter != null) {
                    this.filters.add(filter);
                }
            }
        }

        return this;
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

    public void setAcceptOptions(AcceptOptions acceptOptions) {
        this.acceptOptions = acceptOptions;
    }

}
