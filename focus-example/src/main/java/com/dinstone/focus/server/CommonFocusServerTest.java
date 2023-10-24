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

import com.dinstone.focus.example.ArithService;
import com.dinstone.focus.example.ArithServiceImpl;
import com.dinstone.focus.example.AuthenService;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.focus.example.OrderService;
import com.dinstone.focus.example.OrderServiceImpl;
import com.dinstone.focus.example.UserService;
import com.dinstone.focus.example.UserServiceServerImpl;
import com.dinstone.focus.serialze.json.JacksonSerializer;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
import com.dinstone.focus.serialze.protostuff.ProtostuffSerializer;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class CommonFocusServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(CommonFocusServerTest.class);

    public static void main(String[] args) {

        String appName = "focus.example.server";
        ServerOptions serverOptions = new ServerOptions(appName).listen("localhost", 3333);
        FocusServer server = new FocusServer(serverOptions);

        server.exporting(UserService.class, new UserServiceServerImpl());
        server.exporting(DemoService.class, new DemoServiceImpl());

        // stuff
        server.exporting(OrderService.class, new OrderServiceImpl(null, null),
                new ExportOptions(OrderService.class.getName())
                        .setSerializerType(ProtostuffSerializer.SERIALIZER_TYPE));
        // json
        server.exporting(OrderService.class, new OrderServiceImpl(null, null),
                new ExportOptions("OrderService").setSerializerType(JacksonSerializer.SERIALIZER_TYPE));

        // export alias service
        server.exporting(AuthenService.class, new AuthenService(), "AuthenService");
        server.exporting(ArithService.class, new ArithServiceImpl(),
                new ExportOptions("ArithService").setSerializerType(ProtobufSerializer.SERIALIZER_TYPE));

        try {
            LOG.info("server start");
            server.start();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.close();
        LOG.info("server stop");
    }

}
