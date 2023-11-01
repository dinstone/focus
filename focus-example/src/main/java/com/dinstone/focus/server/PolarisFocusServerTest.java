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

import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.server.polaris.PolarisResolverOptions;
import com.dinstone.focus.server.polaris.RateLimitInterceptor;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class PolarisFocusServerTest {
    private static final Logger LOG = LoggerFactory.getLogger(FocusServerTest.class);

    public static void main(String[] args) {
        // setting registry config
        final String pa = "119.91.66.223:8091";// "192.168.1.120:8091";
        Interceptor rateLimt = new RateLimitInterceptor(pa);
        ServerOptions serverOptions = new ServerOptions("com.rpc.demo.server");
        serverOptions.setResolverOptions(new PolarisResolverOptions().addAddresses(pa));
        serverOptions.listen("-", 3333).setAcceptOptions(new PhotonAcceptOptions());
        serverOptions.addInterceptor(rateLimt);

        FocusServer server = new FocusServer(serverOptions);
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
