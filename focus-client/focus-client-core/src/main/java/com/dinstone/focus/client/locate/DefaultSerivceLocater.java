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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.SerivceLocater;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;

public class DefaultSerivceLocater implements SerivceLocater {

    private final AtomicInteger index = new AtomicInteger(0);

    private ServiceConfig serviceConfig;

    public DefaultSerivceLocater(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public ServiceInstance locate(Call call, ServiceInstance selected, List<ServiceInstance> instances) {
        instances = route(selected, instances);

        if (instances == null || instances.size() == 0) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return instances.get(next % instances.size());
        }
    }

    private List<ServiceInstance> route(ServiceInstance selected, List<ServiceInstance> instances) {
        String group = serviceConfig.getGroup();
        List<ServiceInstance> sds = new ArrayList<ServiceInstance>();
        for (ServiceInstance instance : instances) {
            if (instance == selected) {
                continue;
            }
            if (group.equals(instance.getServiceGroup())) {
                sds.add(instance);
            }
        }
        return sds;
    }

}
