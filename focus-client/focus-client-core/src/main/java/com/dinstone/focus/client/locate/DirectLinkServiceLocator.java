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
package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;

public class DirectLinkServiceLocator extends AbstractServiceLocator {

    private final List<ServiceInstance> instances = new LinkedList<>();

    public DirectLinkServiceLocator(ClientOptions clientOptions) {
        List<InetSocketAddress> connectAddresses = clientOptions.getConnectAddresses();
        if (connectAddresses == null || connectAddresses.isEmpty()) {
            throw new FocusException("connectAddresses is empty, please set connectAddresses");
        }
        boolean enableSsl = clientOptions.getConnectOptions().isEnableSsl();
        for (InetSocketAddress inetSocketAddress : connectAddresses) {
            this.instances.add(new DefaultInstance(inetSocketAddress, enableSsl));
        }
    }

    @Override
    protected List<ServiceInstance> routing(Invocation invocation, List<ServiceInstance> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return instances;
        }

        return instances.stream().filter(i -> !exclusions.contains(i)).collect(Collectors.toList());
    }

}
