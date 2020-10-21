/*
 * Copyright (C) 2018~2020 dinstone<dinstone@163.com>
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

import com.dinstone.focus.example.DemoService;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");
        ClientOptions option = new ClientOptions().connect("localhost", 3333);
        Client client = new Client(option);
        DemoService ds = client.importing(DemoService.class);

        LOG.info("int end");

        int c = 0;
        while (c < 200) {
            System.out.println(ds.hello("dinstoneo"));
            
            c++;
        }
        
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.destroy();
    }

}
