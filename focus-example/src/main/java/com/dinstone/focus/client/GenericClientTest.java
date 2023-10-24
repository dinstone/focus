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
package com.dinstone.focus.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.transport.photon.PhotonConnectOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class GenericClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(GenericClientTest.class);

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions("focus.example.client").connect("localhost", 3333)
                .setConnectOptions(new PhotonConnectOptions());
        // .setCompressorType(SnappyCompressor.COMPRESSOR_TYPE);
        FocusClient client = new FocusClient(option);

        LOG.info("init end");

        try {
            demoService(client);

            orderService(client);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }

    }

    private static void demoService(FocusClient client) throws Exception {
        GenericService gs = client.generic("", "com.dinstone.focus.example.DemoService");
        String parameter = generateString(10241);
        // String r = gs.sync(String.class, "hello", parameter);
        // System.out.println("result = " + r);

        CompletableFuture<String> future = gs.async(String.class, "hello", parameter);
        future.thenAccept(s -> {
            LOG.info("accept result =  " + s);
        });
        LOG.info("future result =  " + future.get());
    }

    @SuppressWarnings({ "rawtypes" })
    private static void orderService(FocusClient client) throws Exception {
        GenericService gs = client.generic("", "OrderService");
        Map<String, String> p = new HashMap<String, String>();
        p.put("sn", "S001");
        p.put("uid", "U981");
        p.put("poi", "20910910");
        p.put("ct", "2022-06-17");

        CompletableFuture<HashMap> cf = gs.async(HashMap.class, "findOldOrder", p);
        System.out.println("aync result =  " + cf.get());

        Map<String, Object> r = gs.sync("findOldOrder", p);
        System.out.println("sync result =  " + r);

    }

    public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }

        return sb.toString();
    }
}
