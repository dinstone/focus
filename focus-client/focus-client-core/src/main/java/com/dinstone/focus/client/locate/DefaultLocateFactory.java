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

import com.dinstone.focus.client.LoadBalancer;
import com.dinstone.focus.client.LocateFactory;
import com.dinstone.focus.client.ServiceRouter;
import com.dinstone.focus.config.ServiceConfig;

public class DefaultLocateFactory implements LocateFactory {

    public ServiceRouter createSerivceRouter(ServiceConfig serviceConfig) {
        return new GroupServiceRouter(serviceConfig);
    }

    public LoadBalancer createLoadBalancer(ServiceConfig serviceConfig) {
        return new RoundRobinLoadBalancer(serviceConfig);
    }
}
