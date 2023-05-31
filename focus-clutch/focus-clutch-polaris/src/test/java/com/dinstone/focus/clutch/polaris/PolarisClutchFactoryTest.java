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
package com.dinstone.focus.clutch.polaris;

import java.util.Collection;

import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.clutch.ServiceRegistry;

public class PolarisClutchFactoryTest {

    public static void main(String[] args) {

        PolarisClutchOptions clutchOptions = new PolarisClutchOptions();
        clutchOptions.addAddresses("192.168.1.120:8091");

        PolarisClutchFactory factory = new PolarisClutchFactory();
        ServiceRegistry registry = factory.createServiceRegistry(clutchOptions);
        testRigstry(registry);

        ServiceDiscovery discovery = factory.createServiceDiscovery(clutchOptions);
        testDiscovery(discovery);

        registry.destroy();
        discovery.destroy();
    }

    private static void testDiscovery(ServiceDiscovery discovery) {
        try {
            Collection<ServiceInstance> sis = discovery.discovery("provider.user");
            for (ServiceInstance serviceInstance : sis) {
                System.out.println(serviceInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
            discovery.destroy();
        }

        System.out.println("discovery ok");
    }

    private static void testRigstry(ServiceRegistry registry) {
        ServiceInstance si = new ServiceInstance();
        si.setEndpointCode("test.provider");
        si.setServiceName("provider.user");
        si.setServiceGroup("default");
        si.setInstanceHost("192.168.1.20");
        si.setInstancePort(8888);
        try {
            registry.register(si);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("reigster ok");
    }

}
