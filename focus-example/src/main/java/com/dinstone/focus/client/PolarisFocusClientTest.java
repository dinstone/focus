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

import com.dinstone.focus.client.polaris.CircuitBreakInterceptor;
import com.dinstone.focus.client.polaris.PolarisLocaterOptions;
import com.dinstone.focus.example.DemoService;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class PolarisFocusClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");

        final String pa = "119.91.66.223:8091";// "192.168.1.120:8091";
        PolarisLocaterOptions locaterOptions = new PolarisLocaterOptions().addAddresses(pa);
        ClientOptions option = new ClientOptions("com.rpc.demo.client").setLocaterOptions(locaterOptions)
                .addInterceptor(new CircuitBreakInterceptor(pa));

        FocusClient client = new FocusClient(option);
        DemoService ds = client.importing(DemoService.class, "com.rpc.demo.server");

        LOG.info("int end");

        try {
            Thread.sleep(1000);

            ds.hello(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            execute(ds, "hot: ");
            execute(ds, "exe: ");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.close();
    }

    private static void execute(DemoService ds, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 200000;
        while (c < loopCount) {
            ds.hello("dinstoneo");
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

}
