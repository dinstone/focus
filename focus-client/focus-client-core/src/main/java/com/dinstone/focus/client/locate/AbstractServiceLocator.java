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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocator;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;

public abstract class AbstractServiceLocator implements ServiceLocator {

    private final AtomicInteger index = new AtomicInteger();

    @Override
    public ServiceInstance locate(Invocation invocation, List<ServiceInstance> exclusions) {
        // routing
        List<ServiceInstance> instances = routing(invocation, exclusions);
        // balance
        return balance(invocation, instances);
    }

    protected abstract List<ServiceInstance> routing(Invocation invocation, List<ServiceInstance> exclusions);

    protected ServiceInstance balance(Invocation invocation, List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return instances.get(next % instances.size());
        }
    }

    @Override
    public void feedback(ServiceInstance instance, Invocation invocation, Object reply, Throwable error, long delay) {

    }

    @Override
    public void subscribe(String serviceName) {

    }

    @Override
    public void destroy() {

    }
}
