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
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.example.ArithService;
import com.dinstone.focus.protobuf.ArithRequest;
import com.dinstone.focus.protobuf.ArithResponse;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
import com.dinstone.focus.transport.photon.PhotonConnectOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class GoServiceClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(GoServiceClientTest.class);

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions("focus.example.client").connect("localhost", 9010)
                .setConnectOptions(new PhotonConnectOptions());
        FocusClient client = new FocusClient(option);

        LOG.info("init end");

        try {
            demoService(client);
            client.close();

            option = new ClientOptions("focus.example.client").connect("localhost", 8010)
                    .setConnectOptions(new PhotonConnectOptions());
            client = new FocusClient(option);
            arithService(client);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }

    }

    private static void arithService(FocusClient client) {
        ArithService as = client.importing(ArithService.class,
                new ImportOptions("ArithService").setSerializerType(ProtobufSerializer.SERIALIZER_TYPE));

        ArithRequest request = ArithRequest.newBuilder().setA(20).setB(34).build();
        ArithResponse response = as.Add(request);

        System.out.println(response.getC());
    }

    @SuppressWarnings("rawtypes")
    private static void demoService(FocusClient client) throws Exception {
        GenericService gs = client.generic("", "TestService");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("A", 20);
        parameter.put("B", 5);

        CompletableFuture<HashMap> future = gs.async("Sub", parameter);
        future.thenAccept(s -> {
            LOG.info("accept result =  " + s);
        });
        LOG.info("future result =  " + future.get());

        parameter.put("A", 20);
        parameter.put("B", 5);
        gs.sync("Div", parameter);
    }

}
