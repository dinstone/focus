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
package com.dinstone.focus.client.invoke;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.dinstone.clutch.ServiceInstance;
import com.dinstone.focus.binding.ReferenceBinding;
import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.locate.LoadBalancer;
import com.dinstone.focus.client.locate.LocateFactory;
import com.dinstone.focus.client.locate.ServiceRouter;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class LocationInvokeHandler implements InvokeHandler {

    private List<ServiceInstance> backupServiceInstances = new ArrayList<ServiceInstance>();

    private InvokeHandler invocationHandler;

    private ReferenceBinding referenceBinding;

    private ServiceRouter serviceRouter;

    private LoadBalancer loadBalancer;

    public LocationInvokeHandler(ServiceConfig serviceConfig, InvokeHandler invocationHandler,
            ReferenceBinding referenceBinding, ClientOptions clientOptions) {
        this.invocationHandler = invocationHandler;
        this.referenceBinding = referenceBinding;
        // init backup service instances
        if (clientOptions.getServiceAddresses() != null) {
            for (InetSocketAddress socketAddress : clientOptions.getServiceAddresses()) {
                String service = serviceConfig.getService();
                String group = serviceConfig.getGroup();
                String host = socketAddress.getHostString();
                int port = socketAddress.getPort();

                ServiceInstance si = new ServiceInstance();
                si.setServiceName(service);
                si.setServiceGroup(group);
                si.setHost(host);
                si.setPort(port);

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
        LocateFactory locateFactory = clientOptions.getLocateFactory();
        serviceRouter = locateFactory.createSerivceRouter(serviceConfig);
        loadBalancer = locateFactory.createLoadBalancer(serviceConfig);
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        List<ServiceInstance> candidates = collect(call);

        int retry = 2;
        ServiceInstance selected = null;
        for (int i = 0; i < retry; i++) {
            List<ServiceInstance> ris = serviceRouter.route(call, selected, candidates);
            selected = loadBalancer.select(call, selected, ris);
            // check
            if (selected == null) {
                continue;
            }

            call.context().put("service.instance", selected);
            try {
                return invocationHandler.invoke(call);
            } catch (ConnectException e) {
                // ignore
            } catch (Exception e) {
                throw e;
            }
        }

        throw new RuntimeException("can't find a service instance for " + call.getService());
    }

    private List<ServiceInstance> collect(Call call) {
        List<ServiceInstance> instances = referenceBinding.lookup(call.getService());
        if (instances != null) {
            return instances;
        } else {
            return new ArrayList<ServiceInstance>(backupServiceInstances);
        }
    }

}