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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public abstract class AbstractServiceLocater implements ServiceLocater {

    private final AtomicInteger index = new AtomicInteger();

    public AbstractServiceLocater() {
        super();
    }

    @Override
    public ServiceInstance locate(Call call, ServiceInstance selected) {
        try {
            // routing
            List<ServiceInstance> instances = routing(call, selected);
            // balance
            return balance(call, instances);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    public void feedback(ServiceInstance instance, Call call, Reply reply, Throwable error, long delay) {
    }

    @Override
    public void subscribe(String serviceName) {
    }

    @Override
    public void destroy() {
    }

    protected abstract List<ServiceInstance> routing(Call call, ServiceInstance selected) throws Exception;

    protected ServiceInstance balance(Call call, List<ServiceInstance> instances) {
        if (instances.size() == 0) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return instances.get(next % instances.size());
        }
    }

}