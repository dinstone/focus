/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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

import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.registry.ServiceDescription;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class LocationInvokeHandler implements InvokeHandler {

    private final AtomicInteger index = new AtomicInteger(0);

    private InvokeHandler invocationHandler;

    private ReferenceBinding referenceBinding;

    private List<InetSocketAddress> backupServiceAddresses = new ArrayList<>();

    public LocationInvokeHandler(InvokeHandler invocationHandler, ReferenceBinding referenceBinding,
            List<InetSocketAddress> serviceAddresses) {
        this.invocationHandler = invocationHandler;
        this.referenceBinding = referenceBinding;

        if (serviceAddresses != null) {
            backupServiceAddresses.addAll(serviceAddresses);
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        InetSocketAddress serviceAddress = select(call.getService(), call.getGroup());
        InvokeContext.getContext().put("service.address", serviceAddress);
        return invocationHandler.invoke(call);
    }

    public <T> InetSocketAddress select(String serviceName, String group) {
        InetSocketAddress serviceAddress = null;

        int next = Math.abs(index.getAndIncrement());
        if (referenceBinding != null) {
            serviceAddress = locate(serviceName, group, next);
        }

        if (serviceAddress == null && backupServiceAddresses.size() > 0) {
            serviceAddress = backupServiceAddresses.get(next % backupServiceAddresses.size());
        }

        if (serviceAddress == null) {
            throw new RuntimeException("service " + serviceName + "[" + group + "] is not ready");
        }

        return serviceAddress;
    }

    private InetSocketAddress locate(String serviceName, String group, int index) {
        List<ServiceDescription> serviceDescriptions = referenceBinding.lookup(serviceName, group);
        if (serviceDescriptions == null || serviceDescriptions.size() == 0) {
            return null;
        }

        return serviceDescriptions.get(index % serviceDescriptions.size()).getServiceAddress();
    }

}