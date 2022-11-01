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
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.compress.snappy.SnappyCompressor;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;

public class GoServiceClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(GoServiceClientTest.class);

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions().setEndpoint("focus.example.client").connect("localhost", 8010)
                .setConnectOptions(new ConnectOptions()).setCompressorId(SnappyCompressor.COMPRESSOR_ID);
        FocusClient client = new FocusClient(option);

        LOG.info("init end");

        try {
            demoService(client);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.destroy();
        }

    }

    private static void demoService(FocusClient client) throws Exception {
        GenericService gs = client.generic("TestService", "", 30000);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("A", 20);
        parameter.put("B", 5);

        CompletableFuture<HashMap> future = gs.async("Add", parameter);
        future.thenAccept(s -> {
            LOG.info("accept result =  " + s);
        });
        LOG.info("future result =  " + future.get());
    }

}
