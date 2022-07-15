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
package com.dinstone.focus.client.locate;

import java.util.ArrayList;
import java.util.List;

import com.dinstone.clutch.ServiceInstance;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;

public class GroupServiceRouter implements ServiceRouter {

    private ServiceConfig serviceConfig;

    public GroupServiceRouter(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public List<ServiceInstance> route(Call call, ServiceInstance selected, List<ServiceInstance> instances) {
        if (instances != null && instances.size() > 0) {
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
        return null;
    }

}
