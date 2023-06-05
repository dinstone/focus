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
package com.dinstone.focus.client.invoke;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.LocaterFactory;
import com.dinstone.focus.client.SerivceLocater;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.transport.Connector;

public class RemoteInvokeHandler implements Handler {

    private List<ServiceInstance> backupServiceInstances = new ArrayList<ServiceInstance>();

    private Connector connector;

    private ServiceConfig serviceConfig;

    private ReferenceBinding referenceBinding;

    private SerivceLocater serivceLocater;

    private int connectRetry;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, Connector connector, ReferenceBinding referenceBinding,
            ClientOptions clientOptions) {
        this.serviceConfig = serviceConfig;
        this.connector = connector;
        this.referenceBinding = referenceBinding;

        // init backup service instances
        if (clientOptions.getServiceAddresses() != null) {
            for (InetSocketAddress socketAddress : clientOptions.getServiceAddresses()) {
                String service = serviceConfig.getApplication();
                String group = serviceConfig.getNamespace();
                String host = socketAddress.getHostString();
                int port = socketAddress.getPort();

                ServiceInstance si = new ServiceInstance();
                si.setIdentity(service);
                si.setNamespace(group);
                si.setInstanceHost(host);
                si.setInstancePort(port);

                StringBuilder code = new StringBuilder();
                code.append(service).append("@");
                code.append(host).append(":");
                code.append(port).append("$");
                code.append((group == null ? "" : group));
                si.setInstanceCode(code.toString());

                backupServiceInstances.add(si);
            }
        }

        // init router and load balancer
        LocaterFactory locateFactory = clientOptions.getLocaterFactory();
        serivceLocater = locateFactory.createSerivceLocater(serviceConfig);

        connectRetry = clientOptions.getConnectRetry();
    }

    @Override
    public CompletableFuture<Reply> handle(Call call) throws Exception {
        // find candidate service instance
        List<ServiceInstance> candidates = collect(call);

        ServiceInstance selected = null;
        for (int i = 0; i < connectRetry; i++) {
            selected = serivceLocater.locate(call, selected, candidates);
            // check
            if (selected == null) {
                continue;
            }

            try {
                return remoteInvoke(call, selected);
            } catch (ConnectException e) {
                // ignore and retry
            } catch (Exception e) {
                throw e;
            }
        }

        throw new FocusException("can't find a live service instance for " + call.getService());
    }

    private List<ServiceInstance> collect(Call call) {
        List<ServiceInstance> instances = referenceBinding.lookup(serviceConfig);
        if (instances != null) {
            return instances;
        } else {
            return backupServiceInstances;
        }
    }

    private CompletableFuture<Reply> remoteInvoke(Call call, ServiceInstance instance) throws Exception {
        call.attach().put(Attach.PROVIDER_KEY, instance.getIdentity());
        return connector.send(call, serviceConfig, instance);
    }

}
