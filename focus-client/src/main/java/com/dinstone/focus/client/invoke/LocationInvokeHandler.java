/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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
package com.dinstone.focus.client.invoke;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.clutch.ServiceDescription;
import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.transport.ConnectionManager;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.connection.Connection;

public class LocationInvokeHandler implements InvokeHandler {

    private final AtomicInteger index = new AtomicInteger(0);

    private InvokeHandler invocationHandler;

    private ReferenceBinding referenceBinding;

    private ConnectionManager connectionManager;

    private List<InetSocketAddress> backupServiceAddresses = new ArrayList<>();

    public LocationInvokeHandler(InvokeHandler invocationHandler, ReferenceBinding referenceBinding,
            ConnectionManager connectionManager, List<InetSocketAddress> serviceAddresses) {
        this.invocationHandler = invocationHandler;
        this.referenceBinding = referenceBinding;
        this.connectionManager = connectionManager;

        if (serviceAddresses != null) {
            backupServiceAddresses.addAll(serviceAddresses);
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        Connection connection = loadbalance(call);
        if (connection == null) {
            throw new RuntimeException("can't find a service connection");
        }
        InvokeContext.getContext().put("service.connection", connection);

        return invocationHandler.invoke(call);
    }

    private Connection loadbalance(Call call) throws Exception {
        int count = 0;
        while (count < 2) {
            count++;

            InetSocketAddress serviceAddress = select(call);
            Connection connection = connectionManager.getConnection(serviceAddress);
            if (connection.isBusy()) {
                continue;
            } else {
                call.attach().put("consumer.address", connection.getLocalAddress().toString());
                call.attach().put("provider.address", connection.getRemoteAddress().toString());
                return connection;
            }
        }
        return null;
    }

    public <T> InetSocketAddress select(Call call) {
        String service = call.getService();
        String group = call.getGroup();

        InetSocketAddress serviceAddress = null;
        int next = Math.abs(index.getAndIncrement());
        if (referenceBinding != null) {
            ServiceDescription sd = route(service, group, next);
            if (sd != null) {
                serviceAddress = sd.getServiceAddress();
                call.attach().put("provider.appcode", sd.getApp());
            }
        }

        if (serviceAddress == null && backupServiceAddresses.size() > 0) {
            serviceAddress = backupServiceAddresses.get(next % backupServiceAddresses.size());
        }

        if (serviceAddress == null) {
            throw new RuntimeException("service " + service + "[" + group + "] is not ready");
        }
        return serviceAddress;
    }

    private ServiceDescription route(String service, String group, int index) {
        List<ServiceDescription> serviceDescriptions = referenceBinding.lookup(service);
        if (serviceDescriptions == null || serviceDescriptions.size() == 0) {
            return null;
        }

        List<ServiceDescription> sds = new ArrayList<ServiceDescription>(serviceDescriptions.size());
        for (ServiceDescription serviceDescription : serviceDescriptions) {
            if (group.equals(serviceDescription.getGroup())) {
                sds.add(serviceDescription);
            }
        }

        if (sds.size() == 0) {
            return null;
        } else if (sds.size() == 1) {
            return sds.get(0);
        }
        return sds.get(index % sds.size());
    }

}