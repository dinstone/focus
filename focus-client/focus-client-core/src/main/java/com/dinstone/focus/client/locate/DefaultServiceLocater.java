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
package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.protocol.Call;

public abstract class DefaultServiceLocater implements ServiceLocater {

    private final AtomicInteger index = new AtomicInteger();

    public DefaultServiceLocater() {
        super();
    }

    @Override
    public InetSocketAddress locate(Call call, InetSocketAddress selected) {
        try {
            // routing
            List<InetSocketAddress> addresses = routing(call, selected);
            // balance
            return balance(call, addresses);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    public void feedback(Call call, InetSocketAddress selected, boolean ok) {
    }

    @Override
    public void subscribe(String serviceName) {
    }

    @Override
    public void destroy() {
    }

    protected abstract List<InetSocketAddress> routing(Call call, InetSocketAddress selected);

    protected InetSocketAddress balance(Call call, List<InetSocketAddress> addresses) {
        if (addresses.size() == 0) {
            return null;
        } else if (addresses.size() == 1) {
            return addresses.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return addresses.get(next % addresses.size());
        }
    }

}