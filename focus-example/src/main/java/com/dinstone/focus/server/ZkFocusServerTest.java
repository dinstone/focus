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
package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.focus.clutch.zookeeper.ZookeeperClutchOptions;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZkFocusServerTest {
    private static final Logger LOG = LoggerFactory.getLogger(FocusServerTest.class);

    public static void main(String[] args) {
        // setting registry config
        ZookeeperClutchOptions registryConfig = new ZookeeperClutchOptions().setZookeeperNodes("192.168.1.120:2181");
        ServerOptions setEndpointCode = new ServerOptions("com.rpc.demo.server").listen("-", 3333)
                .setAcceptOptions(new PhotonAcceptOptions());
        FocusServer server = new FocusServer(setEndpointCode);
        server.exporting(DemoService.class, new DemoServiceImpl());
        server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.close();
        LOG.info("server stop");
    }

}
