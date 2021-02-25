/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.clutch.consul.ConsulRegistryConfig;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ConsulFocusServerTest {
    private static final Logger LOG = LoggerFactory.getLogger(FocusServerTest.class);

    public static void main(String[] args) {
        RegistryConfig registryConfig = new ConsulRegistryConfig();
        // setting registry config
        ServerOptions serverOptions = new ServerOptions().setRegistryConfig(registryConfig).listen("-", 3333)
                .setAppCode("com.rpc.demo.server");
        Server server = new Server(serverOptions);
        server.exporting(DemoService.class, new DemoServiceImpl());
        // server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.destroy();
        LOG.info("server stop");
    }

}
