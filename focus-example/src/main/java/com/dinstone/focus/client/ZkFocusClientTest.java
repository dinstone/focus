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
package com.dinstone.focus.client;

import java.io.IOException;

import com.dinstone.clutch.zookeeper.ZookeeperRegistryConfig;
import com.dinstone.focus.example.DemoService;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;

public class ZkFocusClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");
        ConnectOptions connectOptions = new ConnectOptions();

        ClientOptions option = new ClientOptions().setConnectOptions(connectOptions)
                .setRegistryConfig(new ZookeeperRegistryConfig()).setAppCode("com.rpc.demo.client");

        Client client = new Client(option);
        DemoService ds = client.importing(DemoService.class);

        try {
            ds.hello(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOG.info("int end");

        execute(ds, "hot: ");
        execute(ds, "exe: ");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.destroy();
    }

    private static void execute(DemoService ds, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 200000;
        while (c < loopCount) {
            ds.hello("dinstoneo", c);
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

}
