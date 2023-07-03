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
package com.dinstone.focus.clutch.consul;

import com.dinstone.focus.clutch.ServiceInstance;

public class ConsulServiceRegistryTest {
    public static void main(String[] args) {
        ServiceInstance description = new ServiceInstance();
        String serviceName = "HelloService";
        description.setInstanceCode("service-provider-" + System.currentTimeMillis());
        description.setInstanceHost("localhost");
        description.setInstancePort(80);

        description.setServiceName(serviceName);
        description.setNamespace("default");

        ConsulClutchOptions config = new ConsulClutchOptions();
        ConsulServiceRegistry registry = new ConsulServiceRegistry(config);

        try {
            registry.register(description);

            System.out.println("started");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            registry.destroy();
        }
    }
}
