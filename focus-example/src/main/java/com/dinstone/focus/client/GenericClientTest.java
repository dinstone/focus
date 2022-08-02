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
package com.dinstone.focus.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;

public class GenericClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(GenericClientTest.class);

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions().setEndpoint("focus.example.client").connect("localhost", 3333)
                .setConnectOptions(new ConnectOptions());
        FocusClient client = new FocusClient(option);

        LOG.info("init end");

        try {
            demoService(client);

            orderService(client);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.destroy();
        }

    }

    private static void demoService(FocusClient client) throws Exception {
        GenericService gs = client.genericService("com.dinstone.focus.example.DemoService", "", 30000);
        String r = gs.sync(String.class, "hello", String.class, "dinstone");
        System.out.println("result =  " + r);

        Future<String> future = gs.async(String.class, "hello", String.class, "dinstone");
        System.out.println("result =  " + future.get());
    }

    private static void orderService(FocusClient client) throws Exception {
        GenericService gs = client.genericService("com.dinstone.focus.example.OrderService", "", 30000);
        Map<String, String> p = new HashMap<String, String>();
        p.put("sn", "S001");
        p.put("uid", "U981");
        p.put("poi", "20910910");
        p.put("ct", "2022-06-17");

        Map<String, Object> r = gs.sync(HashMap.class, "findOldOrder", Map.class, p);
        System.out.println("result =  " + r);
    }
}
