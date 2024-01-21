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
package com.dinstone.focus.client;

import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;

/**
 * Service locator is used for service discovery, request routing and load balancing.
 */
public interface ServiceLocater {

    public ServiceInstance locate(Invocation invocation, ServiceInstance selected);

    public void feedback(ServiceInstance instance, Invocation invocation, Object reply, Throwable error, long delay);

    public void subscribe(String serviceName);

    public void destroy();

}
